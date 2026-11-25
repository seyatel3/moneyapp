package ru.moneyapp.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import ru.moneyapp.data.auth.TokenStorage
import ru.moneyapp.data.remote.SheetsService
import ru.moneyapp.data.remote.ValueRange
import ru.moneyapp.data.repository.SettingsRepository
import ru.moneyapp.data.repository.TransactionRepository
import ru.moneyapp.domain.model.PaymentMethod
import ru.moneyapp.domain.model.Transaction
import ru.moneyapp.domain.model.TransactionType
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * WorkManager job for syncing data with Google Sheets.
 * No GMS dependency — pure Jetpack WorkManager + REST API.
 *
 * Sync strategy (last-write-wins per transaction, keyed by UUID):
 *  1. PULL  — read the sheet, insert remote rows missing locally, and apply
 *             remote rows that are newer than the local copy (by updatedAt).
 *  2. WRITE — rewrite the sheet from the merged local state in one canonical
 *             order (date, createdAt), in chunks. This is a constant number of
 *             API requests regardless of data size and can't produce
 *             duplicate rows.
 *  3. MARK  — record the new sheet row index / syncedAt for local rows that
 *             moved, so future syncs update in place.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted private val workerParams: WorkerParameters,
    private val transactionRepository: TransactionRepository,
    private val sheetsService: SheetsService,
    private val settingsRepository: SettingsRepository,
    private val tokenStorage: TokenStorage
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                Timber.d("SyncWorker started (run #${runAttemptCount})")

                val connected = settingsRepository.googleConnected.first()
                if (!connected) {
                    Timber.d("SyncWorker: Google not connected, skipping")
                    Result.success()
                } else {
                    sync()
                    Result.success()
                }
            } catch (e: Exception) {
                Timber.e(e, "SyncWorker failed")
                if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
            }
        }
    }

    private suspend fun sync() {
        val sheetId = settingsRepository.spreadsheetId.first()
        if (sheetId.isBlank()) {
            Timber.d("SyncWorker: no spreadsheet id, skipping")
            return
        }
        // The interceptor attaches the token if present; without it the API
        // calls would just 401, so skip early to avoid noisy retries.
        if (tokenStorage.getAccessToken().isNullOrBlank()) {
            Timber.d("SyncWorker: no access token, skipping")
            return
        }

        val spreadsheet = sheetsService.getSpreadsheet(sheetId)
        val sheetTitle = spreadsheet.sheets
            ?.firstOrNull { it.properties?.title?.equals(SHEET_TITLE, ignoreCase = true) == true }
            ?.properties?.title
            ?: spreadsheet.sheets
                ?.firstOrNull { it.properties?.title?.contains("транзак", ignoreCase = true) == true }
                ?.properties?.title
            ?: spreadsheet.sheets?.firstOrNull()?.properties?.title
        if (sheetTitle.isNullOrBlank()) {
            Timber.e("SyncWorker: transactions sheet not found in $sheetId")
            return
        }

        val now = System.currentTimeMillis()

        // ---- 1. PULL ----
        val values = sheetsService.getValues(sheetId, "'$sheetTitle'!A2:J").values ?: emptyList()
        val remoteRows = values.withIndex().mapNotNull { (index, row) -> parseRemoteRow(index, row) }
        Timber.d("SyncWorker: pulled ${remoteRows.size} remote rows")

        for (remote in remoteRows) {
            val local = transactionRepository.getById(remote.transaction.id)
            if (local == null) {
                transactionRepository.insert(remote.transaction)
            } else if (remote.transaction.updatedAt > local.updatedAt) {
                transactionRepository.update(
                    local.copy(
                        date = remote.transaction.date,
                        type = remote.transaction.type,
                        category = remote.transaction.category,
                        description = remote.transaction.description,
                        amountKopecks = remote.transaction.amountKopecks,
                        paymentMethod = remote.transaction.paymentMethod,
                        tag = remote.transaction.tag,
                        note = remote.transaction.note,
                        updatedAt = remote.transaction.updatedAt,
                        sheetsRowIndex = remote.sheetRow
                    )
                )
            }
        }

        // ---- 2. WRITE (canonical rewrite from merged local state) ----
        val canonical = transactionRepository.getAll().first()
            .sortedWith(compareBy<Transaction>({ it.date }, { it.createdAt }, { it.id }))

        if (canonical.isNotEmpty()) {
            var offset = 0
            while (offset < canonical.size) {
                val chunk = canonical.subList(offset, minOf(offset + ROWS_PER_WRITE, canonical.size))
                val startRow = offset + 2
                val endRow = offset + chunk.size + 1
                sheetsService.updateValues(
                    sheetId,
                    "'$sheetTitle'!A$startRow:J$endRow",
                    ValueRange("A$startRow:J$endRow", null, chunk.map { toSheetRow(it) })
                )
                offset += chunk.size
            }
        }

        // Drop remote rows for transactions deleted locally.
        val lastRemoteRow = remoteRows.maxOfOrNull { it.sheetRow } ?: 1
        if (lastRemoteRow > canonical.size + 1) {
            sheetsService.clearValues(sheetId, "'$sheetTitle'!A${canonical.size + 2}:J")
        }

        // ---- 3. MARK ----
        for ((index, tx) in canonical.withIndex()) {
            val newRow = index + 2
            if (tx.sheetsRowIndex != newRow) {
                transactionRepository.markSyncedWithRow(tx.id, now, newRow)
            } else if (tx.syncedAt == null) {
                transactionRepository.markSynced(tx.id, now)
            }
        }

        Timber.d("SyncWorker completed: ${canonical.size} transactions")
    }

    private data class RemoteRow(
        val sheetRow: Int,
        val transaction: Transaction
    )

    private fun parseRemoteRow(listIndex: Int, row: List<Any?>): RemoteRow? {
        val id = row.getOrNull(0)?.toString()?.trim().orEmpty()
        if (id.isEmpty()) return null
        val date = parseDate(row.getOrNull(1)?.toString()) ?: return null
        val type = when (row.getOrNull(2)?.toString()?.trim()) {
            "Доход" -> TransactionType.INCOME
            "Расход" -> TransactionType.EXPENSE
            "Отложить" -> TransactionType.SAVINGS
            "Из накоплений" -> TransactionType.SAVINGS_SPEND
            else -> return null
        }
        val category = row.getOrNull(3)?.toString()?.trim().orEmpty()
        if (category.isEmpty()) return null
        // Keep an empty description as-is (round-trip: local "" must not
        // silently turn into the category name on every sync).
        val description = row.getOrNull(4)?.toString()?.trim().orEmpty()
        val amountRubles = numberOrZero(row.getOrNull(5))
        val payment = when (row.getOrNull(6)?.toString()?.trim()) {
            "Нал" -> PaymentMethod.CASH
            else -> PaymentMethod.CARD
        }
        val tag = row.getOrNull(7)?.toString()?.trim()?.ifEmpty { null }
        val note = row.getOrNull(8)?.toString()?.trim()?.ifEmpty { null }
        val updatedAt = numberOrZero(row.getOrNull(9)).toLong()

        return RemoteRow(
            sheetRow = listIndex + 2,
            transaction = Transaction(
                id = id,
                date = date.toEpochDay(),
                type = type,
                category = category,
                description = description,
                amountKopecks = Math.round(amountRubles * 100),
                paymentMethod = payment,
                tag = tag,
                note = note,
                createdAt = updatedAt,
                updatedAt = updatedAt
            )
        )
    }

    private fun toSheetRow(tx: Transaction): List<Any> {
        return listOf(
            tx.id,
            LocalDate.ofEpochDay(tx.date).format(CELL_DATE_FORMAT),
            tx.type.xlsxLabel,
            tx.category,
            tx.description,
            tx.amountKopecks / 100.0,
            tx.paymentMethod.xlsxLabel,
            tx.tag ?: "",
            tx.note ?: "",
            tx.updatedAt
        )
    }

    private fun parseDate(raw: String?): LocalDate? {
        if (raw.isNullOrBlank()) return null
        val text = raw.trim()
        for (pattern in DATE_PATTERNS) {
            try {
                return LocalDate.parse(text, DateTimeFormatter.ofPattern(pattern))
            } catch (e: DateTimeParseException) {
                // try the next format
            }
        }
        return null
    }

    private fun numberOrZero(value: Any?): Double =
        when (value) {
            is Number -> value.toDouble()
            is String -> value.trim().toDoubleOrNull() ?: 0.0
            else -> 0.0
        }

    companion object {
        const val TAG = "sync_worker"
        const val SHEET_TITLE = "Транзакции"
        private const val MAX_ATTEMPTS = 3
        private const val ROWS_PER_WRITE = 4000
        private val DATE_PATTERNS = listOf("dd.MM.yyyy", "yyyy-MM-dd", "dd/MM/yyyy")
        private val CELL_DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy")

        fun enqueueSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncWork = PeriodicWorkRequestBuilder<SyncWorker>(15, java.util.concurrent.TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, java.util.concurrent.TimeUnit.MINUTES)
                .addTag(TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                syncWork
            )
        }

        fun enqueueOneTimeSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncWork = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .addTag(TAG)
                .build()

            WorkManager.getInstance(context).enqueue(syncWork)
        }
    }
}
