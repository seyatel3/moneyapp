package ru.moneyapp.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.moneyapp.data.repository.CategoryRepository
import ru.moneyapp.data.repository.TransactionRepository
import ru.moneyapp.data.service.BudgetService
import ru.moneyapp.domain.model.*
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * v1.10.0: the dashboard can show either the selected MONTH or the whole YEAR.
 * MONTH is the default and behaves exactly as before; YEAR aggregates the
 * calendar year (KPIs + category breakdown from the year range; the monthly cap
 * card is hidden because it is a month-scoped concept).
 */
enum class PeriodMode { MONTH, YEAR }

/** [start, end) in epoch days for the selected period. */
private data class PeriodRange(val start: Long, val end: Long)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetService: BudgetService
) : ViewModel() {

    private val _selectedYear = MutableStateFlow(LocalDate.now().year)
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    fun setSelectedYear(year: Int) {
        if (_selectedYear.value != year) {
            _selectedYear.value = year
        }
    }

    fun getCurrentYear(): Int = _selectedYear.value

    // The dashboard is month-driven by default: the month is the primary
    // selection, the year the secondary context (kept in the same ViewModel).
    private val _selectedMonth = MutableStateFlow(LocalDate.now().monthValue)
    val selectedMonth: StateFlow<Int> = _selectedMonth.asStateFlow()

    fun setSelectedMonth(month: Int) {
        if (_selectedMonth.value != month) {
            _selectedMonth.value = month
        }
    }

    /** Steps the selection by a number of months, rolling the year over. */
    fun changeMonth(delta: Int) {
        val stepped = YearMonth.of(_selectedYear.value, _selectedMonth.value).plusMonths(delta.toLong())
        _selectedYear.value = stepped.year
        _selectedMonth.value = stepped.monthValue
    }

    // v1.10.0: month / year period mode.
    private val _periodMode = MutableStateFlow(PeriodMode.MONTH)
    val periodMode: StateFlow<PeriodMode> = _periodMode.asStateFlow()

    fun setPeriodMode(mode: PeriodMode) {
        if (_periodMode.value != mode) {
            _periodMode.value = mode
        }
    }

    /**
     * v1.10.0: step the selection according to the period mode — the month
     * (rolling the year) in MONTH mode, the whole year in YEAR mode.
     */
    fun changePeriod(delta: Int) {
        when (_periodMode.value) {
            PeriodMode.MONTH -> changeMonth(delta)
            PeriodMode.YEAR -> _selectedYear.value += delta
        }
    }

    // v1.10.0: the [start, end) epoch-day range of the selected period.
    private val periodRange: Flow<PeriodRange> =
        combine(_periodMode, _selectedYear, _selectedMonth) { mode, year, month ->
            when (mode) {
                PeriodMode.MONTH -> {
                    val ym = YearMonth.of(year, month)
                    PeriodRange(ym.atDay(1).toEpochDay(), ym.atEndOfMonth().toEpochDay() + 1)
                }
                PeriodMode.YEAR ->
                    PeriodRange(
                        LocalDate.of(year, 1, 1).toEpochDay(),
                        LocalDate.of(year + 1, 1, 1).toEpochDay()
                    )
            }
        }

    // Monthly breakdown (per month of the selected year); feeds the bar chart.
    val monthlyBreakdown: Flow<List<MonthlyChartData>> = _selectedYear.flatMapLatest { year ->
        kotlinx.coroutines.flow.flow {
            val rows = transactionRepository.getMonthlyBreakdown(year)
            val months = (1..12).map { m ->
                val row = rows.find { it.month == String.format("%02d", m) }
                MonthlyChartData(
                    month = m,
                    incomeKopecks = row?.income ?: 0L,
                    expensesKopecks = row?.expenses ?: 0L,
                    savingsKopecks = row?.savings ?: 0L
                )
            }
            emit(months)
        }
    }

    // v1.10.0: KPIs for the selected period (month OR year). A single
    // getMonthlyTotals over the period range covers both (a full-year range
    // equals getYearlyTotals), so the month-mode values are unchanged.
    // v1.10.2: the hero balance now CARRIES OVER all transactions before the
    // period (net income-expenses-savings of everything earlier), so a month
    // shows the real running balance, not just the month's net flow. The
    // income/expense/savings KPIs themselves stay period-scoped.
    val kpis: Flow<DashboardKPIs> =
        periodRange.flatMapLatest { range ->
            kotlinx.coroutines.flow.flow {
                val totals = transactionRepository.getMonthlyTotals(range.start, range.end)
                val before = transactionRepository.getTotalsBefore(range.start)
                // v1.11.6: SAVINGS_SPEND never touches the wallet (the money
                // already left it at the SAVINGS deposit), but the «Отложено»
                // KPI shows the piggy NET of spending from it.
                val carriedKopecks = before.income - before.expenses - before.savings
                val periodNetKopecks = totals.income - totals.expenses - totals.savings
                emit(
                    DashboardKPIs(
                        totalIncomeKopecks = totals.income,
                        totalExpensesKopecks = totals.expenses,
                        totalSavingsKopecks = totals.savings - totals.savingsSpend,
                        balanceKopecks = periodNetKopecks + carriedKopecks,
                        savingsRate = if (totals.income > 0) totals.savings.toDouble() / totals.income else 0.0
                    )
                )
            }
        }

    // v1.10.0: category breakdown for the selected period (month OR year).
    val categoryBreakdown: Flow<List<CategoryExpenseBreakdown>> =
        periodRange.flatMapLatest { range ->
            kotlinx.coroutines.flow.flow {
                val categories = transactionRepository.getExpensesByCategory(range.start, range.end)
                val totalExpenses = categories.sumOf { it.total }

                val breakdown = categories.map { catAmount ->
                    // v1.9.0: the DB is the source of truth for icons
                    // (custom categories are not in DefaultCategories).
                    // v1.10.0: expense breakdown → look up under EXPENSE.
                    val category = categoryRepository.getByTypeAndName(TransactionType.EXPENSE, catAmount.category)
                        ?: DefaultCategories.findByTypeAndName(TransactionType.EXPENSE, catAmount.category)
                    CategoryExpenseBreakdown(
                        category = catAmount.category,
                        categoryEmoji = category?.emoji ?: "📦",
                        iconKey = category?.icon,
                        amountKopecks = catAmount.total,
                        percentageOfExpenses = if (totalExpenses > 0) catAmount.total.toDouble() / totalExpenses else 0.0,
                        percentageOfIncome = 0.0
                    )
                }
                emit(breakdown)
            }
        }

    // v1.10.0: total cap for the selected period. Month-scoped, so it is only
    // shown in MONTH mode (null in YEAR mode hides the cap card).
    val capInfo: Flow<BudgetCapInfo?> =
        combine(_periodMode, _selectedYear, _selectedMonth) { mode, year, month ->
            Triple(mode, year, month)
        }.flatMapLatest { (mode, year, month) ->
            kotlinx.coroutines.flow.flow {
                emit(if (mode == PeriodMode.YEAR) null else budgetService.capInfo(year, month))
            }
        }

    // Cumulative savings for line chart (always the whole selected year).
    val cumulativeSavings: Flow<List<CumulativeSavingsPoint>> = _selectedYear.flatMapLatest { year ->
        kotlinx.coroutines.flow.flow {
            val rows = transactionRepository.getCumulativeSavingsByMonth(year)
            var cumulative = 0L
            val points = mutableListOf<CumulativeSavingsPoint>()
            for (m in 1..12) {
                val row = rows.find { it.month == String.format("%02d", m) }
                cumulative += (row?.savings ?: 0L)
                points.add(CumulativeSavingsPoint(month = m, cumulativeKopecks = cumulative))
            }
            emit(points)
        }
    }

    fun refresh() {
        val current = _selectedYear.value
        _selectedYear.value = current - 1
        _selectedYear.value = current
    }
}

data class MonthlyChartData(
    val month: Int,
    val incomeKopecks: Long,
    val expensesKopecks: Long,
    val savingsKopecks: Long
)
