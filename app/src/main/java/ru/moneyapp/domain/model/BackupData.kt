package ru.moneyapp.domain.model

import com.squareup.moshi.JsonClass

/**
 * Full-app JSON backup document (Moshi, KotlinJsonAdapterFactory).
 * Kept in domain.model so the R8 rule for the domain package covers it.
 */
@JsonClass(generateAdapter = false)
data class BackupData(
    val app: String = "moneyapp",
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val username: String = "",
    val transactions: List<Transaction> = emptyList(),
    val goals: List<SavingsGoal> = emptyList(),
    val categories: List<Category> = emptyList(),
    val budgets: List<BudgetPlan> = emptyList(),
    val budgetTemplates: List<BudgetTemplate> = emptyList()
)