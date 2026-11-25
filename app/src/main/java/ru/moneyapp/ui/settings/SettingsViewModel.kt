package ru.moneyapp.ui.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.squareup.moshi.Moshi
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.moneyapp.data.auth.PkceUtil
import ru.moneyapp.data.auth.TokenStorage
import ru.moneyapp.data.backup.BackupManager
import ru.moneyapp.data.excel.ExcelExporter
import ru.moneyapp.data.excel.ExcelImporter
import ru.moneyapp.data.remote.BatchUpdateRequest
import ru.moneyapp.data.remote.CreateSheetProperties
import ru.moneyapp.data.remote.CreateSheetRequest
import ru.moneyapp.data.remote.CreateSpreadsheetProperties
import ru.moneyapp.data.remote.CreateSpreadsheetRequest
import ru.moneyapp.data.remote.OAuth2Service
import ru.moneyapp.data.remote.SheetsService
import ru.moneyapp.data.remote.ValueRange
import ru.moneyapp.data.repository.BudgetPlanRepository
import ru.moneyapp.data.repository.BudgetTemplateRepository
import ru.moneyapp.data.repository.CategoryRepository
import ru.moneyapp.data.repository.SavingsGoalRepository
import ru.moneyapp.data.repository.SettingsRepository
import ru.moneyapp.data.repository.TransactionRepository
import ru.moneyapp.domain.model.BackupData
import ru.moneyapp.domain.model.BudgetTemplate
import ru.moneyapp.domain.model.BudgetType
import ru.moneyapp.domain.model.Transaction
import ru.moneyapp.notification.DailyReminderReceiver
import ru.moneyapp.util.CrashLog
import ru.moneyapp.ui.auth.OAuthWebViewActivity
import ru.moneyapp.ui.l10n.AppLang
import ru.moneyapp.ui.l10n.L10n
import ru.moneyapp.worker.SyncWorker
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val transactionRepository: TransactionRepository,
    private val savingsGoalRepository: SavingsGoalRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetPlanRepository: BudgetPlanRepository,
    private val budgetTemplateRepository: BudgetTemplateRepository,
    private val sheetsService: SheetsService,
    private val oauth2Service: OAuth2Service,
    private val tokenStorage: TokenStorage,
    private val moshi: Moshi,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    sealed interface SettingsEvent {
        data class Success(val message: String) : SettingsEvent
        data class Error(val message: String) : SettingsEvent
    }

    private val _events = MutableSharedFlow<SettingsEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<SettingsEvent> = _events

    private val _isWorking = MutableStateFlow(false)
    val isWorking: StateFlow<Boolean> = _isWorking.asStateFlow()

    // Settings
    val username: Flow<String> = settingsRepository.username
    val currencySymbol: Flow<String> = settingsRepository.currencySymbol
    val googleConnected: Flow<Boolean> = settingsRepository.googleConnected
    val spreadsheetId: Flow<String> = settingsRepository.spreadsheetId
    val spreadsheetLink: Flow<String> = settingsRepository.spreadsheetLink
    val googleClientId: Flow<String> = settingsRepository.googleClientId
    val dailyReminderEnabled: Flow<Boolean> = settingsRepository.dailyReminderEnabled
    val dailyReminderHour: Flow<Int> = settingsRepository.dailyReminderHour
    val dailyReminderMinute: Flow<Int> = settingsRepository.dailyReminderMinute
    val themeMode: Flow<String> = settingsRepository.themeMode
    val language: Flow<String> = settingsRepository.language // v1.9.0: "ru" / "en"

    // v1.6.0: monthly budget
    val monthlyCapKopecks: Flow<Long> = settingsRepository.monthlyCapKopecks
    val capLinkedIncome: Flow<Boolean> = settingsRepository.capLinkedIncome
    val capIncomePct: Flow<Int> = settingsRepository.capIncomePct
    val budgetCopyPrevMonth: Flow<Boolean> = settingsRepository.budgetCopyPrevMonth
    val budgetTemplates: Flow<List<BudgetTemplate>> = budgetTemplateRepository.getAll()

    // Last fatal crash journal (empty = no crash recorded)
    private val _lastCrash = MutableStateFlow(CrashLog.last(appContext) ?: "")
    val lastCrash: StateFlow<String> = _lastCrash.asStateFlow()

    fun clearCrashLog() {
        CrashLog.clear(appContext)
        _lastCrash.value = ""
    }

    fun shareCrashLog() {
        val text = _lastCrash.value
        if (text.isBlank()) return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, L10n.s.crashShareSubject)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        appContext.startActivity(
            Intent.createChooser(intent, L10n.s.crashChooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    // OAuth web view: non-null while it must be launched
    private val _oauthLaunch = MutableStateFlow<Intent?>(null)
    val oauthLaunch: StateFlow<Intent?> = _oauthLaunch

    // Excel import preview (shown in a confirm dialog)
    private val _importPreview = MutableStateFlow<ExcelImporter.ImportPreview?>(null)
    val importPreview: StateFlow<ExcelImporter.ImportPreview?> = _importPreview
    private var pendingImportUri: Uri? = null

    private val backupManager = BackupManager(appContext, moshi)

    /** Flattens an exception chain ("Foo: bar: baz") for user-visible messages. */
    private fun describeError(e: Exception): String {
        val parts = mutableListOf<String>()
        var cur: Throwable? = e
        while (cur != null && parts.size < 3) {
            parts.add(cur.message ?: cur.javaClass.simpleName)
            cur = cur.cause
        }
        return parts.joinToString(": ")
    }

    // ---------- Account ----------

    fun setUsername(name: String) {
        viewModelScope.launch { settingsRepository.setUsername(name) }
    }

    fun setCurrency(symbol: String) {
        viewModelScope.launch { settingsRepository.setCurrencySymbol(symbol) }
    }

    fun setTheme(mode: String) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    /**
     * v1.9.0: in-app language switch. L10n.lang is snapshot state, so the
     * whole UI updates immediately; then the choice is persisted (a persist
     * failure must not leave the UI stuck on the old language).
     */
    fun setLanguage(lang: String) {
        viewModelScope.launch {
            L10n.lang = if (lang == "en") AppLang.EN else AppLang.RU
            settingsRepository.setLanguage(lang)
        }
    }

    // ---------- v1.6.0: monthly budget ----------

    fun setMonthlyCapText(text: String) {
        viewModelScope.launch {
            val kopecks = if (text.isBlank()) 0L
            else ((text.trim().toDoubleOrNull() ?: 0.0) * 100).toLong()
            settingsRepository.setMonthlyCapKopecks(if (kopecks > 0) kopecks else 0L)
        }
    }

    fun setCapLinkedIncome(linked: Boolean) {
        viewModelScope.launch { settingsRepository.setCapLinkedIncome(linked) }
    }

    fun setCapIncomePctText(text: String) {
        viewModelScope.launch {
            val pct = text.trim().toIntOrNull() ?: 0
            settingsRepository.setCapIncomePct(pct.coerceIn(0, 1000))
        }
    }

    fun setBudgetCopyPrevMonth(copy: Boolean) {
        viewModelScope.launch { settingsRepository.setBudgetCopyPrevMonth(copy) }
    }

    /** rublesText: user input in ₽; blank removes the template row. */
    fun setTemplateAmount(category: String, type: BudgetType, rublesText: String) {
        viewModelScope.launch {
            val text = rublesText.trim()
            if (text.isEmpty()) {
                budgetTemplateRepository.delete(category, type.name)
                return@launch
            }
            val kopecks = ((text.toDoubleOrNull() ?: 0.0) * 100).toLong()
            if (kopecks > 0) {
                budgetTemplateRepository.upsert(BudgetTemplate(category, type, kopecks))
            } else {
                budgetTemplateRepository.delete(category, type.name)
            }
        }
    }

    // ---------- Google sync (OAuth 2.0 + PKCE, installed client) ----------

    fun setGoogleClientId(id: String) {
        viewModelScope.launch { settingsRepository.setGoogleClientId(id.trim()) }
    }

    /** v1.8.0: link to the user's own spreadsheet (raw input, trimmed on use). */
    fun setGoogleSpreadsheetLink(link: String) {
        viewModelScope.launch { settingsRepository.setSpreadsheetLink(link) }
    }

    /**
     * Starts the OAuth flow: builds the PKCE challenge and the intent for
     * the WebView activity. If no client id is configured, asks the user
     * to enter it first.
     */
    fun connectGoogle() {
        viewModelScope.launch {
            val clientId = settingsRepository.googleClientId.first().trim()
            if (clientId.isBlank()) {
                emit(SettingsEvent.Error(L10n.s.setErrNoClientId))
                return@launch
            }
            val pkce = PkceUtil.generate()
            tokenStorage.saveCodeVerifier(pkce.verifier)
            _oauthLaunch.value = Intent(appContext, OAuthWebViewActivity::class.java).apply {
                putExtra("client_id", clientId)
                putExtra("redirect_uri", OAuth2Service.REDIRECT_URI)
                putExtra("code_challenge", pkce.challenge)
                putExtra(
                    "scopes",
                    arrayListOf(OAuth2Service.SPREADSHEETS_SCOPE, OAuth2Service.DRIVE_SCOPE)
                )
            }
        }
    }

    fun clearOauthLaunch() {
        _oauthLaunch.value = null
    }

    /** Called by the activity-result launcher when the WebView finishes. */
    fun onOauthResult(result: ActivityResult) {
        viewModelScope.launch {
            when {
                result.resultCode == Activity.RESULT_OK &&
                    result.data?.getStringExtra("auth_code") != null ->
                    completeOauth(result.data!!.getStringExtra("auth_code")!!)

                result.resultCode == Activity.RESULT_OK -> {
                    tokenStorage.clearCodeVerifier()
                    emit(
                        SettingsEvent.Error(
                            result.data?.getStringExtra("error") ?: L10n.s.setErrAuthError
                        )
                    )
                }

                else -> {
                    tokenStorage.clearCodeVerifier()
                    emit(SettingsEvent.Error(L10n.s.setErrAuthCancelled))
                }
            }
        }
    }

    private suspend fun completeOauth(code: String) {
        _isWorking.value = true
        try {
            val clientId = settingsRepository.googleClientId.first().trim()
            val verifier = tokenStorage.getCodeVerifier()
            if (verifier.isNullOrBlank()) {
                throw IllegalStateException(L10n.s.setErrNoPkce)
            }

            // Token exchange WITHOUT client_secret (installed client + PKCE)
            val token = oauth2Service.exchangeCode(
                code = code,
                clientId = clientId,
                redirectUri = OAuth2Service.REDIRECT_URI,
                codeVerifier = verifier
            )
            tokenStorage.saveAccessToken(token.access_token)
            // Google may omit refresh_token on re-consent — keep the old one
            if (!token.refresh_token.isNullOrBlank()) {
                tokenStorage.saveRefreshToken(token.refresh_token)
            }
            tokenStorage.clearCodeVerifier()

            // Spreadsheet to sync with (v1.8.0): the user's own spreadsheet
            // (link from settings), otherwise a new one created by the app.
            var sheetId = settingsRepository.spreadsheetId.first().trim()
            if (sheetId.isBlank()) {
                val link = settingsRepository.spreadsheetLink.first().trim()
                if (link.isNotBlank()) {
                    sheetId = extractSpreadsheetId(link)
                        ?: throw IllegalStateException(L10n.s.setErrBadLink)
                    ensureTransactionsSheet(sheetId)
                } else {
                    val created = sheetsService.createSpreadsheet(
                        CreateSpreadsheetRequest(
                            CreateSpreadsheetProperties("MoneyAPP — Транзакции"),
                            listOf(
                                CreateSheetRequest(CreateSheetProperties(0, SyncWorker.SHEET_TITLE))
                            )
                        )
                    )
                    sheetId = created.spreadsheetId
                    sheetsService.updateValues(
                        sheetId,
                        "'${SyncWorker.SHEET_TITLE}'!A1:J1",
                        spreadsheetHeader()
                    )
                }
                settingsRepository.setSpreadsheetId(sheetId)
            }

            settingsRepository.setGoogleConnected(true)
            SyncWorker.enqueueSync(appContext)
            emit(SettingsEvent.Success(L10n.s.setMsgConnected))
        } catch (e: Exception) {
            Timber.e(e, "OAuth completion failed")
            tokenStorage.clearCodeVerifier()
            emit(SettingsEvent.Error(L10n.s.setMsgConnectError(describeError(e))))
        } finally {
            _isWorking.value = false
        }
    }

    /**
     * v1.8.0: connect to the user's existing spreadsheet. The getSpreadsheet
     * call doubles as an access check (wrong link or someone else's table →
     * friendly error instead of a raw 403); if the "Транзакции" tab is missing
     * it is added (with the header row). Other tabs are left untouched.
     */
    private suspend fun ensureTransactionsSheet(sheetId: String) {
        val spreadsheet = try {
            sheetsService.getSpreadsheet(sheetId)
        } catch (e: Exception) {
            Timber.e(e, "Cannot access spreadsheet $sheetId")
            throw IllegalStateException(L10n.s.setErrNoSheetAccess)
        }
        val hasTab = spreadsheet.sheets
            ?.any { it.properties?.title?.equals(SyncWorker.SHEET_TITLE, ignoreCase = true) == true }
            ?: false
        if (hasTab) return
        sheetsService.batchUpdate(
            sheetId,
            BatchUpdateRequest(
                listOf(
                    mapOf(
                        "addSheet" to mapOf(
                            "properties" to mapOf(
                                "sheetId" to 0,
                                "title" to SyncWorker.SHEET_TITLE
                            )
                        )
                    )
                )
            )
        )
        sheetsService.updateValues(
            sheetId,
            "'${SyncWorker.SHEET_TITLE}'!A1:J1",
            spreadsheetHeader()
        )
    }

    /** v1.8.0: spreadsheet ID from a docs.google.com link, or a raw ID pasted as-is. */
    private fun extractSpreadsheetId(linkOrId: String): String? {
        val match = Regex("docs\\.google\\.com/spreadsheets/d/([A-Za-z0-9\\-_]+)").find(linkOrId)
        if (match != null) return match.groupValues[1]
        if (Regex("^[A-Za-z0-9\\-_]{20,}$").matches(linkOrId)) return linkOrId
        return null
    }

    private fun spreadsheetHeader(): ValueRange = ValueRange(
        range = "A1:J1",
        major = null,
        values = listOf(
            listOf(
                "ID", "Дата", "Тип", "Категория", "Описание",
                "Сумма (₽)", "Нал/Безнал", "Тег", "Примечание", "Обновлено"
            )
        )
    )

    fun syncNow() {
        viewModelScope.launch {
            SyncWorker.enqueueOneTimeSync(appContext)
            emit(SettingsEvent.Success(L10n.s.setMsgSyncStarted))
        }
    }

    fun disconnectGoogle() {
        viewModelScope.launch {
            settingsRepository.setGoogleConnected(false)
            settingsRepository.setSpreadsheetId("")
            tokenStorage.clear()
            WorkManager.getInstance(appContext).cancelUniqueWork(SyncWorker.TAG)
            emit(SettingsEvent.Success(L10n.s.setMsgDisconnected))
        }
    }

    // ---------- Reminders ----------

    fun toggleReminder(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDailyReminderEnabled(enabled)
            if (enabled) {
                val hour = settingsRepository.dailyReminderHour.first()
                val minute = settingsRepository.dailyReminderMinute.first()
                DailyReminderReceiver.scheduleDailyReminder(appContext, hour, minute)
            } else {
                DailyReminderReceiver.cancelDailyReminder(appContext)
            }
        }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsRepository.setDailyReminderTime(hour, minute)
            if (settingsRepository.dailyReminderEnabled.first()) {
                DailyReminderReceiver.scheduleDailyReminder(appContext, hour, minute)
            }
            emit(
                SettingsEvent.Success(
                    L10n.s.setMsgReminderTime("%02d:%02d".format(hour, minute))
                )
            )
        }
    }

    // ---------- Excel (SAF) ----------

    fun exportExcel(uri: Uri) {
        viewModelScope.launch {
            _isWorking.value = true
            try {
                val transactions = transactionRepository.getAll().first()
                val budgets = budgetPlanRepository.getAll().first()
                val goals = savingsGoalRepository.getAllAsync()
                val templates = budgetTemplateRepository.getAllAsync()
                withContext(Dispatchers.IO) {
                    val out = appContext.contentResolver.openOutputStream(uri)
                        ?: throw IllegalStateException(L10n.s.errOpenFile)
                    out.use { stream ->
                        ExcelExporter(appContext).exportToStream(stream, transactions, budgets, goals, templates)
                    }
                }
                emit(
                    SettingsEvent.Success(
                        L10n.s.setMsgExcelExported(transactions.size)
                    )
                )
            } catch (e: Exception) {
                Timber.e(e, "Excel export failed")
                emit(SettingsEvent.Error(L10n.s.setMsgExportError(describeError(e))))
            } finally {
                _isWorking.value = false
            }
        }
    }

    fun importExcelPick(uri: Uri) {
        viewModelScope.launch {
            try {
                val preview = withContext(Dispatchers.IO) {
                    ExcelImporter(appContext).getPreview(uri)
                }
                if (preview.rowCount == 0) {
                    emit(SettingsEvent.Error(L10n.s.setMsgNoTxInFile))
                    return@launch
                }
                pendingImportUri = uri
                _importPreview.value = preview
            } catch (e: Exception) {
                Timber.e(e, "Excel preview failed")
                emit(SettingsEvent.Error(L10n.s.setMsgReadFileError(describeError(e))))
            }
        }
    }

    fun dismissImportPreview() {
        pendingImportUri = null
        _importPreview.value = null
    }

    fun confirmImportExcel() {
        val uri = pendingImportUri ?: return
        viewModelScope.launch {
            _isWorking.value = true
            try {
                val parsed = withContext(Dispatchers.IO) {
                    ExcelImporter(appContext).importTransactions(uri)
                }
                val local = transactionRepository.getAll().first()

                // Dedupe by (date, type, category, description, amount) —
                // both against local data and within the file itself.
                fun key(tx: Transaction): String =
                    "${tx.date}|${tx.type.name}|${tx.category}|${tx.description}|${tx.amountKopecks}"

                val localKeys = local.map(::key).toSet()
                val seen = mutableSetOf<String>()
                val toAdd = parsed.filter { tx ->
                    val k = key(tx)
                    if (k in localKeys || k in seen) false else {
                        seen.add(k)
                        true
                    }
                }
                if (toAdd.isNotEmpty()) {
                    transactionRepository.insertAll(toAdd)
                }
                val skipped = parsed.size - toAdd.size
                emit(
                    SettingsEvent.Success(
                        if (toAdd.isEmpty()) {
                            L10n.s.setMsgImportNothing(skipped)
                        } else {
                            L10n.s.setMsgImportDone(toAdd.size, skipped)
                        }
                    )
                )
            } catch (e: Exception) {
                Timber.e(e, "Excel import failed")
                emit(SettingsEvent.Error(L10n.s.setMsgImportError(describeError(e))))
            } finally {
                _isWorking.value = false
                pendingImportUri = null
                _importPreview.value = null
            }
        }
    }

    // ---------- Backup JSON (SAF) ----------

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            _isWorking.value = true
            try {
                val data = BackupData(
                    exportedAt = System.currentTimeMillis(),
                    username = settingsRepository.username.first(),
                    transactions = transactionRepository.getAll().first(),
                    goals = savingsGoalRepository.getAllAsync(),
                    categories = categoryRepository.getAllAsync(),
                    budgets = budgetPlanRepository.getAll().first(),
                    budgetTemplates = budgetTemplateRepository.getAllAsync()
                )
                withContext(Dispatchers.IO) {
                    backupManager.writeToFile(uri, data)
                }
                emit(
                    SettingsEvent.Success(
                        L10n.s.setMsgBackupCreated(data.transactions.size, data.goals.size)
                    )
                )
            } catch (e: Exception) {
                Timber.e(e, "Backup export failed")
                emit(SettingsEvent.Error(L10n.s.setMsgBackupError(describeError(e))))
            } finally {
                _isWorking.value = false
            }
        }
    }

    fun importBackupPick(uri: Uri) {
        viewModelScope.launch {
            _isWorking.value = true
            try {
                val data = withContext(Dispatchers.IO) {
                    backupManager.readFromFile(uri)
                }
                val result = backupManager.apply(
                    data = data,
                    transactionRepository = transactionRepository,
                    goalRepository = savingsGoalRepository,
                    categoryRepository = categoryRepository,
                    budgetRepository = budgetPlanRepository,
                    templateRepository = budgetTemplateRepository
                )
                emit(
                    SettingsEvent.Success(
                        L10n.s.setMsgRestored(
                            result.transactionsAdded,
                            result.transactionsUpdated,
                            result.goalsAdded,
                            result.categoriesAdded
                        )
                    )
                )
            } catch (e: Exception) {
                Timber.e(e, "Backup import failed")
                emit(SettingsEvent.Error(L10n.s.setMsgRestoreError(describeError(e))))
            } finally {
                _isWorking.value = false
            }
        }
    }

    private suspend fun emit(event: SettingsEvent) {
        _events.emit(event)
    }
}