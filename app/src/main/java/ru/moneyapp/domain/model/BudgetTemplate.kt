package ru.moneyapp.domain.model

/**
 * Monthly budget template: a fixed planned amount for a category, applied to
 * every month that has no explicit plan (and no copy of the previous month).
 */
data class BudgetTemplate(
    val category: String,
    val type: BudgetType,
    val amountKopecks: Long
) {
    val amountRubles: Double get() = amountKopecks / 100.0
}
