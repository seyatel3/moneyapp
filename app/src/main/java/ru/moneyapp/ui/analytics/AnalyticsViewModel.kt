package ru.moneyapp.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.moneyapp.data.local.dao.AvgCheckDaoRow
import ru.moneyapp.data.repository.CategoryRepository
import ru.moneyapp.data.repository.SettingsRepository
import ru.moneyapp.data.repository.TransactionRepository
import ru.moneyapp.data.service.BudgetService
import ru.moneyapp.domain.model.AvgCheckRow
import ru.moneyapp.domain.model.BigExpense
import ru.moneyapp.domain.model.BalanceCurvePoint
import ru.moneyapp.domain.model.BudgetCapState
import ru.moneyapp.domain.model.CalendarDay
import ru.moneyapp.domain.model.CalendarHeatmap
import ru.moneyapp.domain.model.CategoryExpenseBreakdown
import ru.moneyapp.domain.model.CushionInfo
import ru.moneyapp.domain.model.DefaultCategories
import ru.moneyapp.domain.model.ExtendedAnalytics
import ru.moneyapp.domain.model.PaceInfo
import ru.moneyapp.domain.model.TransactionType
import ru.moneyapp.domain.model.TrendCategory
import ru.moneyapp.domain.model.WeekdaySpend
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlin.math.round
import kotlin.math.roundToLong

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository,
    private val budgetService: BudgetService
) : ViewModel() {

    private val currentYear = LocalDate.now().year

    // Monthly comparison data
    val monthlyComparison: Flow<List<MonthlyComparisonData>> = flow {
            val breakdown = transactionRepository.getMonthlyBreakdown(currentYear)
            val months = (1..12).map { m ->
                val row = breakdown.find { it.month == String.format("%02d", m) }
                MonthlyComparisonData(
                    month = m,
                    year = currentYear,
                    incomeKopecks = row?.income ?: 0L,
                    expensesKopecks = row?.expenses ?: 0L,
                    savingsKopecks = row?.savings ?: 0L,
                    savingsSpendKopecks = row?.savingsSpend ?: 0L
                )
            }
            emit(months)
    }

    // Top 5 expense categories for the year
    val topExpenseCategories: Flow<List<CategoryExpenseBreakdown>> = flow {
            val start = LocalDate.of(currentYear, 1, 1).toEpochDay()
            val end = LocalDate.of(currentYear, 12, 31).toEpochDay() + 1

            val categories = transactionRepository.getExpensesByCategory(start, end)
            val top5 = categories.take(5)

            val breakdown = top5.map { catAmount ->
                // v1.9.0: the DB is the source of truth for icons.
                // v1.10.0: expense breakdown → look up under EXPENSE.
                val category = categoryRepository.getByTypeAndName(TransactionType.EXPENSE, catAmount.category)
                    ?: DefaultCategories.findByTypeAndName(TransactionType.EXPENSE, catAmount.category)
                CategoryExpenseBreakdown(
                    category = catAmount.category,
                    categoryEmoji = category?.emoji ?: "📦",
                    iconKey = category?.icon,
                    amountKopecks = catAmount.total,
                    percentageOfExpenses = 0.0,
                    percentageOfIncome = 0.0
                )
            }
            emit(breakdown)
    }

    // Savings rate trend (12 months)
    val savingsRateTrend: Flow<List<SavingsRateTrend>> = flow {
            val breakdown = transactionRepository.getMonthlyBreakdown(currentYear)
            val trend = (1..12).map { m ->
                val row = breakdown.find { it.month == String.format("%02d", m) }
                val rate = if (row?.income ?: 0L > 0)
                    (row?.savings ?: 0L).toDouble() / (row?.income ?: 1L)
                else 0.0
                SavingsRateTrend(month = m, savingsRate = rate)
            }
            emit(trend)
    }

    // ========================================================================
    // v1.11.0: extended analytics — 10 new cards, mirrored 1:1 in the
    // Telegram mini-app /api/analytics. Every formula below matches app.py.
    // All amounts in kopecks, dates as epoch days.
    // ========================================================================

    val extendedAnalytics: Flow<ExtendedAnalytics> = flow {
        val today = LocalDate.now()
        val cy = today.year
        val cm = today.monthValue
        val t0 = today.toEpochDay()
        val curYm = YearMonth.of(cy, cm)
        val mStart = curYm.atDay(1).toEpochDay()
        val mEnd = curYm.atEndOfMonth().toEpochDay() + 1
        val dim = curYm.lengthOfMonth()
        val prevYm = curYm.minusMonths(1)
        val prevStart = prevYm.atDay(1).toEpochDay()
        val prevEnd = prevYm.atEndOfMonth().toEpochDay() + 1
        val y0 = LocalDate.of(cy, 1, 1).toEpochDay()
        val y1 = LocalDate.of(cy + 1, 1, 1).toEpochDay()

        // First day of the month n-1 months before the current one.
        fun monthStartAgo(n: Int): Long =
            curYm.minusMonths((n - 1).toLong()).atDay(1).toEpochDay()

        // v1.10.0: the DB is the source of truth for icons (EXPENSE scope).
        suspend fun iconKey(name: String): String? =
            categoryRepository.getByTypeAndName(TransactionType.EXPENSE, name)?.icon
                ?: DefaultCategories.findByTypeAndName(TransactionType.EXPENSE, name)?.icon

        // 1) Average check per category, top-6 by count.
        //    v1.11.2: two windows — current month and the last 12 months
        //    (same window as the calendar heatmap).
        suspend fun toAvgCheck(rows: List<AvgCheckDaoRow>): List<AvgCheckRow> = rows.map { r ->
            AvgCheckRow(
                category = r.category,
                count = r.cnt.toInt(),
                avgKopecks = (r.total.toDouble() / r.cnt).roundToLong(),
                iconKey = iconKey(r.category)
            )
        }
        val avgCheck = toAvgCheck(transactionRepository.getAvgCheckByCategory(mStart, mEnd))
        val avgCheckYear = toAvgCheck(
            transactionRepository.getAvgCheckByCategory(monthStartAgo(12), mEnd)
        )

        // 2) Top-5 of the month with the trend vs the previous month.
        val top5 = transactionRepository.getExpensesByCategory(mStart, mEnd).take(5)
        val prevByCat = transactionRepository.getExpensesByCategory(prevStart, prevEnd)
            .associate { it.category to it.total }
        val top5Trend = top5.map { r ->
            val prev = prevByCat[r.category] ?: 0L
            TrendCategory(
                category = r.category,
                amountKopecks = r.total,
                prevKopecks = prev,
                deltaKopecks = r.total - prev,
                deltaPct = if (prev > 0L)
                    round((r.total - prev).toDouble() * 100.0 / prev * 10) / 10.0
                else null
            )
        }

        // 3) Financial cushion: runway = (balance + piggy) / avg monthly expense.
        // v1.11.6: the piggy is the NET of SAVINGS_SPEND; the wallet balance
        // is untouched (spending from savings does not come from the wallet).
        val bAll = transactionRepository.getTotalsBefore(t0 + 1)
        val balanceAll = bAll.income - bAll.expenses - bAll.savings
        val piggy = bAll.savings - bAll.savingsSpend
        val base = balanceAll + piggy
        // v1.11.6: a month is "active" when there were expenses OR spending
        // from savings; the average sums both (parity with the mini-app).
        val activeMonths = transactionRepository.getMonthlyBreakdown(cy)
            .filter { it.expenses > 0L || it.savingsSpend > 0L }
        val avgExp = if (activeMonths.isNotEmpty())
            activeMonths.sumOf { it.expenses + it.savingsSpend }.toDouble() / activeMonths.size
        else 0.0
        val cushion = CushionInfo(
            balanceKopecks = balanceAll,
            piggyKopecks = piggy,
            baseKopecks = base,
            avgMonthlyExpenseKopecks = avgExp.roundToLong(),
            runwayMonths = if (avgExp > 0.0) round(base / avgExp * 10) / 10.0 else null
        )

        // 4) Budget pace: day of month vs % of the cap already spent.
        val fixedCap = settingsRepository.monthlyCapKopecks.first()
        val linked = settingsRepository.capLinkedIncome.first()
        // Full precision for the status diff (parity with app.py), rounded to 4
        // decimals for the payload/displayed value (parity with the mini-app
        // JSON, which serialises round(expected, 4)).
        val expectedFull = today.dayOfMonth.toDouble() / dim
        val expectedPct = Math.round(expectedFull * 10000.0) / 10000.0
        val pace = if (fixedCap <= 0L && !linked) {
            PaceInfo("Disabled", 0.0, null, 0L, null, null)
        } else {
            val state = budgetService.capState(cy, cm)
            val spent = when (state) {
                is BudgetCapState.Ready -> state.info.spentKopecks
                is BudgetCapState.MedianBased -> state.spentKopecks
                is BudgetCapState.AwaitingIncome -> state.spentKopecks
                BudgetCapState.Disabled -> 0L
            }
            val cap = when (state) {
                is BudgetCapState.Ready -> state.info.capKopecks
                is BudgetCapState.MedianBased -> state.capKopecks
                else -> null
            }
            val spentPct = cap?.takeIf { it > 0L }?.let { spent.toDouble() / it }
            val status = spentPct?.let { sp ->
                val diff = sp - expectedFull
                if (diff > 0.05) "BEHIND" else if (diff < -0.05) "AHEAD" else "ON"
            }
            PaceInfo(
                state = when (state) {
                    BudgetCapState.Disabled -> "Disabled"
                    is BudgetCapState.Ready -> "Ready"
                    is BudgetCapState.MedianBased -> "MedianBased"
                    is BudgetCapState.AwaitingIncome -> "AwaitingIncome"
                },
                expectedPct = expectedPct,
                spentPct = spentPct,
                spentKopecks = spent,
                capKopecks = cap,
                status = status
            )
        }

        // 5) Big expenses: top-10 of the last 3 months.
        val bigExpenses = transactionRepository.getTopExpenses(monthStartAgo(3), t0 + 1)
            .map { r -> BigExpense(r.date, r.category, r.description, r.amountKopecks) }

        // v1.11.2: removed — auto-detected subscriptions and year-end balance forecast
        // (user request: unclear / not needed)

        // 8) Balance curve: daily balance of the current month (carry-over + net).
        val carry = transactionRepository.getTotalsBefore(mStart)
        var acc = carry.income - carry.expenses - carry.savings
        val dailyTotals = transactionRepository.getDailyTotals(mStart, t0 + 1)
            .associateBy { it.date }
        val balanceCurve = mutableListOf<BalanceCurvePoint>()
        for (dd in 1..dim) {
            val d0 = mStart + (dd - 1)
            if (d0 > t0) break
            val dt = dailyTotals[d0]
            acc += (dt?.income ?: 0L) - (dt?.expenses ?: 0L) - (dt?.savings ?: 0L)
            balanceCurve.add(BalanceCurvePoint(dd, acc))
        }

        // 9) Expenses by weekday, current year (1 = Monday .. 7 = Sunday).
        val wd = LongArray(7)
        for (r in transactionRepository.getDailyExpenses(y0, y1)) {
            wd[LocalDate.ofEpochDay(r.date).dayOfWeek.value - 1] += r.total
        }
        val weekday = (0 until 7).map { i -> WeekdaySpend(i + 1, wd[i]) }

        // 10) Calendar heatmap: daily expenses, last 12 months.
        val calRows = transactionRepository.getDailyExpenses(monthStartAgo(12), mStart + dim)
        val calendar = CalendarHeatmap(
            days = calRows.map { CalendarDay(it.date, it.total) },
            maxKopecks = calRows.maxOfOrNull { it.total } ?: 0L
        )

        emit(
            ExtendedAnalytics(
                avgCheck = avgCheck,
                avgCheckYear = avgCheckYear,
                top5Trend = top5Trend,
                cushion = cushion,
                pace = pace,
                bigExpenses = bigExpenses,
                balanceCurve = balanceCurve,
                weekday = weekday,
                calendar = calendar
            )
        )
    }
}
