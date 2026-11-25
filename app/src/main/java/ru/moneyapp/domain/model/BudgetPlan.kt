package ru.moneyapp.domain.model

data class BudgetPlan(
    val id: Int = 0,
    val month: Int,            // 1-12
    val year: Int,
    val category: String,
    val type: BudgetType,
    val plannedAmountKopecks: Long
) {
    val plannedAmountRubles: Double get() = plannedAmountKopecks / 100.0
}

enum class BudgetType(val labelRu: String) {
    INCOME("Доход"),
    EXPENSE("Расход"),
    SAVINGS("Отложить"),
    SUMMARY("Итог")
}

/**
 * v1.11.5: where a resolved effective plan came from. The Budget tab uses it
 * for dialog hints and to decide whether "delete" makes sense (only an
 * explicit monthly plan can be deleted; template / previous-month rows are
 * managed in Settings). Mirrors the mini-app's plan `source` field.
 */
enum class BudgetPlanSource { EXPLICIT, TEMPLATE, PREV_MONTH }

/** A plan resolved for a month, with its origin (v1.11.5). */
data class EffectiveBudgetPlan(
    val plan: BudgetPlan,
    val source: BudgetPlanSource
)
