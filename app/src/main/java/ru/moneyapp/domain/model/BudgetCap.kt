package ru.moneyapp.domain.model

/**
 * Total monthly expense cap resolution result.
 * Cap sources (Settings → «Бюджет на месяц»):
 *  - fixed limit in ₽/month, and/or
 *  - link to income: N% of the month's income (capped by the fixed limit).
 */
enum class BudgetCapStatus { OK, CLOSE, OVER }

data class BudgetCapInfo(
    val capKopecks: Long,
    val spentKopecks: Long,
    val percentage: Double // spent / cap, 0.0 when cap is 0
) {
    val capRubles: Double get() = capKopecks / 100.0
    val spentRubles: Double get() = spentKopecks / 100.0
    val remainingKopecks: Long get() = capKopecks - spentKopecks

    // OK < 90%, CLOSE 90–110%, OVER > 110%
    val status: BudgetCapStatus
        get() = when {
            capKopecks <= 0 -> BudgetCapStatus.OK
            spentKopecks > capKopecks * 1.1 -> BudgetCapStatus.OVER
            spentKopecks >= capKopecks * 0.9 -> BudgetCapStatus.CLOSE
            else -> BudgetCapStatus.OK
        }
}

/**
 * v1.7.0: resolved state of the monthly cap for the budget screen.
 * Distinguishes "nothing is configured in settings" (Disabled) from
 * "the cap is linked to income, but the month has no income yet"
 * (AwaitingIncome) — the latter must NOT show the "бюджет не настроен"
 * empty state, the user DID configure the budget.
 */
sealed class BudgetCapState {
    data object Disabled : BudgetCapState()

    data class Ready(val info: BudgetCapInfo) : BudgetCapState()

    data class AwaitingIncome(val pct: Int, val spentKopecks: Long) : BudgetCapState()

    /**
     * v1.10.0: the cap is linked to income (pct > 0) but the selected month has
     * no income yet. Instead of waiting indefinitely, the cap is computed from
     * the MEDIAN of the calendar year's monthly incomes (only months with income
     * count). The dashboard/budget show the real cap so the bar is meaningful.
     */
    data class MedianBased(
        val pct: Int,
        val spentKopecks: Long,
        val capKopecks: Long,
        val medianIncomeKopecks: Long
    ) : BudgetCapState() {
        val info: BudgetCapInfo
            get() = BudgetCapInfo(
                capKopecks = capKopecks,
                spentKopecks = spentKopecks,
                percentage = if (capKopecks > 0) spentKopecks.toDouble() / capKopecks else 0.0
            )
    }
}
