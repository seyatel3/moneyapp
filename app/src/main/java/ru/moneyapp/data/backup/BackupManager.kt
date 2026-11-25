package ru.moneyapp.data.backup

import android.content.Context
import android.net.Uri
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.first
import ru.moneyapp.data.repository.BudgetPlanRepository
import ru.moneyapp.data.repository.BudgetTemplateRepository
import ru.moneyapp.data.repository.CategoryRepository
import ru.moneyapp.data.repository.SavingsGoalRepository
import ru.moneyapp.data.repository.TransactionRepository
import ru.moneyapp.domain.model.BackupData
import ru.moneyapp.domain.model.BudgetPlan
import ru.moneyapp.domain.model.Category
import ru.moneyapp.domain.model.SavingsGoal
import ru.moneyapp.domain.model.Transaction
import timber.log.Timber

/**
 * JSON backup / restore.
 *
 * Format: one UTF-8 JSON document with all app data, serialized by Moshi
 * (KotlinJsonAdapterFactory, reflection — the domain models are kept in
 * proguard-rules.pro).
 *
 * Restore is an upsert (merge, never a wipe):
 *  - transactions: by id, last-write-wins by updatedAt
 *  - goals:        by id
 *  - categories:   by (type, name) (insert missing only, local visibility kept)
 *  - budgets:      by natural key (year, month, category, type)
 */
class BackupManager(
    private val context: Context,
    private val moshi: Moshi
) {

    data class ApplyResult(
        val transactionsAdded: Int,
        val transactionsUpdated: Int,
        val goalsAdded: Int,
        val goalsUpdated: Int,
        val categoriesAdded: Int,
        val budgetsUpserted: Int
    )

    private val adapter = moshi.adapter(BackupData::class.java)

    fun exportJson(data: BackupData): String = adapter.toJson(data)

    fun parseJson(json: String): BackupData =
        adapter.fromJson(json)
            ?: throw IllegalArgumentException("Пустая или некорректная резервная копия")

    fun writeToFile(uri: Uri, data: BackupData) {
        val out = context.contentResolver.openOutputStream(uri)
            ?: throw IllegalStateException("Не удалось открыть файл для записи")
        out.use { stream ->
            stream.write(exportJson(data).toByteArray(Charsets.UTF_8))
        }
    }

    fun readFromFile(uri: Uri): BackupData {
        val json = context.contentResolver.openInputStream(uri)
            ?.bufferedReader(Charsets.UTF_8)
            ?.use { it.readText() }
            ?: throw IllegalStateException("Не удалось открыть файл")
        return parseJson(json)
    }

    suspend fun apply(
        data: BackupData,
        transactionRepository: TransactionRepository,
        goalRepository: SavingsGoalRepository,
        categoryRepository: CategoryRepository,
        budgetRepository: BudgetPlanRepository,
        templateRepository: BudgetTemplateRepository? = null
    ): ApplyResult {
        // Categories: insert missing by name, keep local state for known ones
        var categoriesAdded = 0
        for (category in data.categories) {
            // v1.10.0: composite key — a same-named category of another type
            // (e.g. EXPENSE «Прочее») is a distinct row, so match on (type, name).
            if (categoryRepository.getByTypeAndName(category.type, category.name) == null) {
                categoryRepository.insert(category)
                categoriesAdded++
            }
        }

        // Transactions: by id, newer updatedAt wins
        val local = transactionRepository.getAll().first()
        val localById = local.associateBy { it.id }
        var txAdded = 0
        var txUpdated = 0
        for (tx in data.transactions) {
            val existing = localById[tx.id]
            if (existing == null) {
                transactionRepository.insert(tx)
                txAdded++
            } else if (tx.updatedAt > existing.updatedAt) {
                transactionRepository.update(
                    tx.copy(
                        sheetsRowIndex = existing.sheetsRowIndex,
                        syncedAt = existing.syncedAt
                    )
                )
                txUpdated++
            }
        }

        // Goals: by id (goal has no updatedAt — backup version wins)
        val localGoalIds = goalRepository.getAllAsync().map { it.id }.toSet()
        var goalsAdded = 0
        var goalsUpdated = 0
        for (goal in data.goals) {
            if (goal.id in localGoalIds) {
                goalRepository.update(goal)
                goalsUpdated++
            } else {
                goalRepository.insert(goal)
                goalsAdded++
            }
        }

        // Budgets: by natural key (year, month, category, type)
        var budgetsUpserted = 0
        for (plan in data.budgets) {
            val existing = budgetRepository.getByMonthAndCategory(
                plan.year, plan.month, plan.category, plan.type.name
            )
            if (existing == null) {
                budgetRepository.insert(plan)
            } else if (existing.plannedAmountKopecks != plan.plannedAmountKopecks) {
                budgetRepository.update(existing.copy(plannedAmountKopecks = plan.plannedAmountKopecks))
            }
            budgetsUpserted++
        }

        // Budget templates: upsert by (category, type)
        var templatesUpserted = 0
        if (templateRepository != null) {
            for (template in data.budgetTemplates) {
                templateRepository.upsert(template)
                templatesUpserted++
            }
        }

        Timber.d(
            "Backup applied: tx +$txAdded/=$txUpdated, goals +$goalsAdded/=$goalsUpdated, " +
                    "categories +$categoriesAdded, budgets $budgetsUpserted, templates $templatesUpserted"
        )
        return ApplyResult(
            transactionsAdded = txAdded,
            transactionsUpdated = txUpdated,
            goalsAdded = goalsAdded,
            goalsUpdated = goalsUpdated,
            categoriesAdded = categoriesAdded,
            budgetsUpserted = budgetsUpserted
        )
    }
}