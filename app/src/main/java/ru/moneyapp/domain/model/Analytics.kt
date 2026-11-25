package ru.moneyapp.domain.model

/** Monthly summary for dashboard/analytics charts */
data class MonthlySummary(
    val month: Int,      // 1-12
    val year: Int,
    val incomeKopecks: Long,
    val expensesKopecks: Long,
    val savingsKopecks: Long,
    val balanceKopecks: Long
) {
    val incomeRubles: Double get() = incomeKopecks / 100.0
    val expensesRubles: Double get() = expensesKopecks / 100.0
    val savingsRubles: Double get() = savingsKopecks / 100.0
    val balanceRubles: Double get() = balanceKopecks / 100.0
    val savingsRate: Double
        get() = if (incomeKopecks > 0) savingsKopecks.toDouble() / incomeKopecks else 0.0
}

/** Expense breakdown by category for a given month */
data class CategoryExpenseBreakdown(
    val category: String,
    val categoryEmoji: String,
    // v1.9.0: user-chosen icon key (null = automatic by category name)
    val iconKey: String? = null,
    val amountKopecks: Long,
    val percentageOfExpenses: Double,  // 0..1
    val percentageOfIncome: Double     // 0..1
) {
    val amountRubles: Double get() = amountKopecks / 100.0
}

/** Cumulative savings point for line chart */
data class CumulativeSavingsPoint(
    val month: Int,   // 1-12
    val cumulativeKopecks: Long
) {
    val cumulativeRubles: Double get() = cumulativeKopecks / 100.0
}

/** Dashboard KPIs */
data class DashboardKPIs(
    val totalIncomeKopecks: Long,
    val totalExpensesKopecks: Long,
    val totalSavingsKopecks: Long,
    val balanceKopecks: Long,
    val savingsRate: Double
) {
    val totalIncomeRubles: Double get() = totalIncomeKopecks / 100.0
    val totalExpensesRubles: Double get() = totalExpensesKopecks / 100.0
    val totalSavingsRubles: Double get() = totalSavingsKopecks / 100.0
    val balanceRubles: Double get() = balanceKopecks / 100.0
}

/** Budget status for a category */
data class BudgetCategoryStatus(
    val category: String,
    val categoryEmoji: String,
    val iconKey: String? = null,   // v1.9.0
    val type: BudgetType,
    val plannedKopecks: Long,
    val actualKopecks: Long,
    val differenceKopecks: Long,   // positive = under budget, negative = over
    val percentage: Double,        // actual / planned
    val status: BudgetStatus,
    val source: String = "explicit" // v1.11.5: BudgetPlanSource.name.lowercase()
) {
    val plannedRubles: Double get() = plannedKopecks / 100.0
    val actualRubles: Double get() = actualKopecks / 100.0
    val differenceRubles: Double get() = differenceKopecks / 100.0
}

enum class BudgetStatus(val labelRu: String, val emoji: String) {
    OK("OK", "✅"),
    CLOSE("Почти", "⚠️"),
    OVER("Перебор", "❌"),
    UNDER("Недобор", "⚠️")
}

/** Forecast for when a goal will be reached */
data class GoalForecast(
    val goalId: String,
    val goalName: String,
    val estimatedCompletionDay: Long?,  // epoch day or null if unreachable
    val monthlyContributionKopecks: Long,
    val monthsRemaining: Int
)

// ============================================================================
// v1.11.0: extended analytics (10 new cards, mirrored 1:1 in the Telegram
// mini-app /api/analytics). Formulas below match app.py exactly.
// ============================================================================

/** 1) Average check per expense category for the current month */
data class AvgCheckRow(
    val category: String,
    val count: Int,
    val avgKopecks: Long,
    // v1.11.0: user-chosen icon key (null = automatic by category name)
    val iconKey: String? = null
)

/** 2) Month top-5 expense category with the trend vs the previous month */
data class TrendCategory(
    val category: String,
    val amountKopecks: Long,
    val prevKopecks: Long,
    val deltaKopecks: Long,
    val deltaPct: Double?  // null = no previous-month history
) {
    val deltaPercent: Double get() = deltaPct ?: 0.0
}

/** 3) Financial cushion: runway = (balance + piggy) / avg monthly expense */
data class CushionInfo(
    val balanceKopecks: Long,
    val piggyKopecks: Long,
    val baseKopecks: Long,
    val avgMonthlyExpenseKopecks: Long,
    val runwayMonths: Double?  // null = not enough data
) {
    val balanceRubles: Double get() = balanceKopecks / 100.0
    val piggyRubles: Double get() = piggyKopecks / 100.0
    val baseRubles: Double get() = baseKopecks / 100.0
    val avgMonthlyExpenseRubles: Double get() = avgMonthlyExpenseKopecks / 100.0
}

/** 4) Budget pace: day of month vs % of the cap already spent */
data class PaceInfo(
    val state: String,        // Disabled | Ready | MedianBased | AwaitingIncome
    val expectedPct: Double,  // 0..1 fraction of the cap expected by this day
    val spentPct: Double?,    // 0..1 fraction of the cap already spent (null = no cap)
    val spentKopecks: Long,
    val capKopecks: Long?,
    val status: String?       // AHEAD | ON | BEHIND | null
) {
    val spentRubles: Double get() = spentKopecks / 100.0
    val capRubles: Double? get() = capKopecks?.let { it / 100.0 }
}

/** 5) Single big expense (top-10 of the last 3 months) */
data class BigExpense(
    val date: Long,
    val category: String,
    val description: String,
    val amountKopecks: Long
) {
    val amountRubles: Double get() = amountKopecks / 100.0
}

/** v1.11.2: year-end balance forecast removed (user request). */

/** 8) Daily balance point of the current month (carry-over + net) */
data class BalanceCurvePoint(
    val day: Int,
    val balanceKopecks: Long
) {
    val balanceRubles: Double get() = balanceKopecks / 100.0
}

/** 9) Expenses aggregated by weekday (1 = Monday .. 7 = Sunday) */
data class WeekdaySpend(
    val weekday: Int,
    val amountKopecks: Long
) {
    val amountRubles: Double get() = amountKopecks / 100.0
}

/** 10) Calendar heatmap: daily expenses of the last 12 months */
data class CalendarHeatmap(
    val days: List<CalendarDay>,
    val maxKopecks: Long
) {
    val maxRubles: Double get() = maxKopecks / 100.0
}

data class CalendarDay(
    val date: Long,
    val amountKopecks: Long
) {
    val amountRubles: Double get() = amountKopecks / 100.0
}

/** v1.11.0: the extended analytics results in one bundle (single DB pass).
 *  v1.11.2: + avgCheckYear (last 12 months); subscriptions and year-end forecast removed. */
data class ExtendedAnalytics(
    val avgCheck: List<AvgCheckRow>,
    val avgCheckYear: List<AvgCheckRow>,
    val top5Trend: List<TrendCategory>,
    val cushion: CushionInfo,
    val pace: PaceInfo,
    val bigExpenses: List<BigExpense>,
    val balanceCurve: List<BalanceCurvePoint>,
    val weekday: List<WeekdaySpend>,
    val calendar: CalendarHeatmap
)
