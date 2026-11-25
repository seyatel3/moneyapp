package ru.moneyapp.data.service

import kotlinx.coroutines.flow.first
import ru.moneyapp.data.repository.BudgetPlanRepository
import ru.moneyapp.data.repository.BudgetTemplateRepository
import ru.moneyapp.data.repository.SettingsRepository
import ru.moneyapp.data.repository.TransactionRepository
import ru.moneyapp.domain.model.BudgetCapInfo
import ru.moneyapp.domain.model.BudgetCapState
import ru.moneyapp.domain.model.BudgetPlan
import ru.moneyapp.domain.model.BudgetPlanSource
import ru.moneyapp.domain.model.BudgetType
import ru.moneyapp.domain.model.EffectiveBudgetPlan
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

/**
 * v1.6.0: resolves the effective monthly budget.
 *
 * Per-category plan priority:
 *   explicit plan for the month  >  template  >  copy of the previous month  >  nothing
 *
 * Total expense cap:
 *   fixed limit (₽/month) and/or N% of the month's income:
 *     - both set  → min(limit, N% × income)
 *     - only %    → N% × income (income 0 → falls back to the fixed limit)
 *     - only limit → limit
 *     - neither   → cap disabled (null)
 */
@Singleton
class BudgetService @Inject constructor(
    private val budgetPlanRepository: BudgetPlanRepository,
    private val budgetTemplateRepository: BudgetTemplateRepository,
    private val transactionRepository: TransactionRepository,
    private val settingsRepository: SettingsRepository
) {

    private data class Key(val category: String, val type: BudgetType)

    suspend fun effectivePlans(year: Int, month: Int): List<EffectiveBudgetPlan> {
        val explicit = budgetPlanRepository.getByMonthAsync(year, month)
            .filter { it.type != BudgetType.SUMMARY }
        // v1.11.5: SUMMARY templates are excluded too — the summary section is
        // dead (nothing creates SUMMARY plans), parity with the mini-app.
        val templates = budgetTemplateRepository.getAllAsync()
            .filter { it.type != BudgetType.SUMMARY }
        val copyPrev = settingsRepository.budgetCopyPrevMonth.first()
        val prev = YearMonth.of(year, month).minusMonths(1)
        val prevPlans = if (copyPrev) {
            budgetPlanRepository.getByMonthAsync(prev.year, prev.monthValue)
                .filter { it.type != BudgetType.SUMMARY }
        } else {
            emptyList()
        }

        // Lowest priority first — later writes overwrite earlier ones:
        // copy of the previous month < template < explicit plan for the month
        // (v1.11.5: matches the class docstring; the old order let the
        // previous-month copy override a template).
        val result = LinkedHashMap<Key, EffectiveBudgetPlan>()
        for (p in prevPlans) {
            result[Key(p.category, p.type)] = EffectiveBudgetPlan(
                p.copy(month = month, year = year), BudgetPlanSource.PREV_MONTH
            )
        }
        for (t in templates) {
            result[Key(t.category, t.type)] = EffectiveBudgetPlan(
                BudgetPlan(
                    month = month, year = year,
                    category = t.category, type = t.type,
                    plannedAmountKopecks = t.amountKopecks
                ),
                BudgetPlanSource.TEMPLATE
            )
        }
        for (e in explicit) {
            result[Key(e.category, e.type)] = EffectiveBudgetPlan(e, BudgetPlanSource.EXPLICIT)
        }
        return result.values.toList()
    }

    /**
     * Effective total expense cap for the month, or null when disabled.
     * v1.10.0: also resolves for a MedianBased cap (no income this month, so the
     * cap uses the year's median income) — the dashboard then shows the bar.
     */
    suspend fun capInfo(year: Int, month: Int): BudgetCapInfo? = when (val state = capState(year, month)) {
        is BudgetCapState.Ready -> state.info
        is BudgetCapState.MedianBased -> state.info
        else -> null
    }

    /**
     * v1.7.0: full cap state. Ready — the cap is computable; AwaitingIncome —
     * linked to income (pct > 0) but the month has no income yet; Disabled —
     * neither a fixed limit nor a meaningful income link is configured.
     * (pct = 0 no longer produces a zero-cap "Ready" state.)
     */
    suspend fun capState(year: Int, month: Int): BudgetCapState {
        val cap = settingsRepository.monthlyCapKopecks.first()
        val linked = settingsRepository.capLinkedIncome.first()
        val pct = settingsRepository.capIncomePct.first()
        val yearMonth = YearMonth.of(year, month)
        val totals = transactionRepository.getMonthlyTotals(
            yearMonth.atDay(1).toEpochDay(),
            yearMonth.atEndOfMonth().toEpochDay() + 1
        )
        val incomePart = if (linked && pct > 0 && totals.income > 0) totals.income * pct / 100 else null
        // v1.11.6: spending from the piggy bank also eats the monthly cap.
        val spent = totals.expenses + totals.savingsSpend
        val capKopecks = when {
            incomePart != null && cap > 0 -> minOf(cap, incomePart)
            incomePart != null -> incomePart
            cap > 0 -> cap
            else -> 0L
        }
        return when {
            capKopecks > 0 -> BudgetCapState.Ready(
                BudgetCapInfo(
                    capKopecks = capKopecks,
                    spentKopecks = spent,
                    percentage = spent.toDouble() / capKopecks
                )
            )
            linked && pct > 0 -> {
                // v1.10.0: no income this month → fall back to the MEDIAN of the
                // calendar year's monthly incomes instead of waiting indefinitely.
                val median = medianYearlyIncomeKopecks(year)
                val medianCap = if (median > 0L) {
                    val part = median * pct / 100
                    if (cap > 0) minOf(cap, part) else part
                } else 0L
                if (medianCap > 0) {
                    BudgetCapState.MedianBased(pct, spent, medianCap, median)
                } else {
                    BudgetCapState.AwaitingIncome(pct, spent)
                }
            }
            else -> BudgetCapState.Disabled
        }
    }

    /**
     * v1.10.0: median of the calendar year's monthly incomes (kopecks).
     * Only months with income > 0 count; an even count uses the mean of the two
     * middle values. Returns 0 when the year has no income at all.
     */
    private suspend fun medianYearlyIncomeKopecks(year: Int): Long {
        val incomes = transactionRepository.getMonthlyBreakdown(year)
            .map { it.income }
            .filter { it > 0L }
            .sorted()
        if (incomes.isEmpty()) return 0L
        val n = incomes.size
        return if (n % 2 == 1) {
            incomes[n / 2]
        } else {
            (incomes[n / 2 - 1] + incomes[n / 2]) / 2L
        }
    }
}
