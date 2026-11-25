package ru.moneyapp.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.moneyapp.data.repository.BudgetPlanRepository
import ru.moneyapp.data.repository.BudgetTemplateRepository
import ru.moneyapp.data.repository.CategoryRepository
import ru.moneyapp.data.repository.SettingsRepository
import ru.moneyapp.data.repository.TransactionRepository
import ru.moneyapp.data.service.BudgetService
import ru.moneyapp.domain.model.*
import ru.moneyapp.ui.l10n.L10n
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetPlanRepository: BudgetPlanRepository,
    private val budgetTemplateRepository: BudgetTemplateRepository,
    private val settingsRepository: SettingsRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetService: BudgetService
) : ViewModel() {

    // v1.11.5: one-shot UI events (snackbar messages from the plan dialog)
    sealed class BudgetEvent {
        data class Success(val message: String) : BudgetEvent()
        data class Error(val message: String) : BudgetEvent()
    }
    private val _events = MutableSharedFlow<BudgetEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<BudgetEvent> = _events
    private suspend fun emitEvent(event: BudgetEvent) { _events.emit(event) }

    // v1.11.5: categories for the add-plan dialog
    val categories: Flow<List<Category>> = categoryRepository.getAll()

    private val _selectedMonth = MutableStateFlow(YearMonth.now().monthValue)
    private val _selectedYear = MutableStateFlow(YearMonth.now().year)

    val selectedMonth: StateFlow<Int> = _selectedMonth.asStateFlow()
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    fun setSelectedMonth(month: Int) { _selectedMonth.value = month }
    fun setSelectedYear(year: Int) { _selectedYear.value = year }
    fun previousMonth() {
        val ym = YearMonth.of(_selectedYear.value, _selectedMonth.value).minusMonths(1)
        _selectedMonth.value = ym.monthValue
        _selectedYear.value = ym.year
    }
    fun nextMonth() {
        val ym = YearMonth.of(_selectedYear.value, _selectedMonth.value).plusMonths(1)
        _selectedMonth.value = ym.monthValue
        _selectedYear.value = ym.year
    }

    // Budget status for the current month. Combined with the settings and
    // template flows so the tab reacts live to settings changes (e.g.
    // "copy previous month") without switching months.
    val budgetStatus: Flow<List<BudgetCategoryStatus>> = _selectedMonth.flatMapLatest { month ->
        _selectedYear.flatMapLatest { year ->
            // v1.11.5: budgetPlanRepository.getAll() MUST be in the combine —
            // without it, saving/deleting an explicit plan does not re-emit
            // the flow, so the cards keep the stale source (the plan dialog
            // for a prev row stayed "prev" after materializing an explicit
            // plan, and a deleted plan kept showing "Удалить").
            kotlinx.coroutines.flow.combine(
                transactionRepository.getAll(),
                budgetPlanRepository.getAll(),
                budgetTemplateRepository.getAll(),
                settingsRepository.budgetCopyPrevMonth
            ) { _, _, _, _ -> Unit }
                .map { withContext(Dispatchers.IO) { buildStatuses(year, month) } }
        }
    }

    // v1.6.0: total monthly expense cap. v1.7.0: full state so the screen can
    // tell "nothing configured" (Disabled) apart from "linked to income, but
    // no income recorded for the month yet" (AwaitingIncome). Reactive on both
    // transactions and the cap settings.
    val capState: Flow<BudgetCapState> = _selectedMonth.flatMapLatest { month ->
        _selectedYear.flatMapLatest { year ->
            kotlinx.coroutines.flow.combine(
                transactionRepository.getAll(),
                settingsRepository.monthlyCapKopecks,
                settingsRepository.capLinkedIncome,
                settingsRepository.capIncomePct
            ) { _, _, _, _ -> Unit }
                .map { withContext(Dispatchers.IO) { budgetService.capState(year, month) } }
        }
    }

    private suspend fun buildStatuses(year: Int, month: Int): List<BudgetCategoryStatus> {
        val yearMonth = YearMonth.of(year, month)
        val start = yearMonth.atDay(1).toEpochDay()
        val end = yearMonth.atEndOfMonth().toEpochDay() + 1

        // v1.6.0: effective plans = templates + (optional) copy of
        // the previous month, overridden by the explicit plan.
        val plans = budgetService.effectivePlans(year, month)
        val actualExpenses = transactionRepository.getActualExpensesByCategory(start, end)
        val actualIncome = transactionRepository.getActualIncomeByCategory(start, end)
        // v1.11.5: SAVINGS plans track the month's SAVINGS transactions
        // (parity with the mini-app). The old code used EXPENSE actuals,
        // which never match savings categories → always 0.
        val actualSavings = transactionRepository.getActualSavingsByCategory(start, end)

        // v1.11.5: same display order as the mini-app's /api/budget —
        // sorted by (type, category), so cards inside a group follow the
        // alphabetical category order in both mirrors.
        return plans
            .sortedWith(compareBy({ it.plan.type.name }, { it.plan.category }))
            .map { ep ->
                val plan = ep.plan
                val actual = when (plan.type) {
                    BudgetType.EXPENSE -> actualExpenses.find { it.category == plan.category }?.total ?: 0L
                    BudgetType.INCOME -> actualIncome.find { it.category == plan.category }?.total ?: 0L
                    BudgetType.SAVINGS -> actualSavings.find { it.category == plan.category }?.total ?: 0L
                    BudgetType.SUMMARY -> 0L
                }

                val difference = plan.plannedAmountKopecks - actual
                val percentage = if (plan.plannedAmountKopecks > 0) actual.toDouble() / plan.plannedAmountKopecks else 0.0

                val status = when {
                    plan.type == BudgetType.SUMMARY -> BudgetStatus.OK
                    percentage > 1.1 -> BudgetStatus.OVER
                    percentage > 0.9 -> BudgetStatus.CLOSE
                    percentage < 0.5 && plan.type == BudgetType.INCOME -> BudgetStatus.UNDER
                    else -> BudgetStatus.OK
                }

                // v1.9.0: the DB is the source of truth for icons
                // (custom categories are not in DefaultCategories).
                // v1.10.0: look up under the plan's own type (SUMMARY has none).
                val category = if (plan.type == BudgetType.SUMMARY) {
                    null
                } else {
                    val txType = TransactionType.valueOf(plan.type.name)
                    categoryRepository.getByTypeAndName(txType, plan.category)
                        ?: ru.moneyapp.domain.model.DefaultCategories.findByTypeAndName(txType, plan.category)
                }

                BudgetCategoryStatus(
                    category = plan.category,
                    categoryEmoji = category?.emoji ?: "📦",
                    iconKey = category?.icon,
                    type = plan.type,
                    plannedKopecks = plan.plannedAmountKopecks,
                    actualKopecks = actual,
                    differenceKopecks = difference,
                    percentage = percentage,
                    status = status,
                    // mini-app wire values: "explicit" | "template" | "prev"
                    source = when (ep.source) {
                        BudgetPlanSource.EXPLICIT -> "explicit"
                        BudgetPlanSource.TEMPLATE -> "template"
                        BudgetPlanSource.PREV_MONTH -> "prev"
                    }
                )
            }
    }

    // v1.11.5: upsert the explicit plan for the selected month (wired to the
    // Budget tab dialog; for template / previous-month rows this creates a
    // monthly override without touching the template).
    fun updateBudgetPlan(category: String, type: BudgetType, newAmountKopecks: Long) {
        viewModelScope.launch {
            val existing = budgetPlanRepository.getByMonthAndCategory(
                _selectedYear.value, _selectedMonth.value, category, type.name
            )
            if (existing != null) {
                budgetPlanRepository.update(existing.copy(plannedAmountKopecks = newAmountKopecks))
            } else {
                budgetPlanRepository.insert(
                    BudgetPlan(
                        month = _selectedMonth.value,
                        year = _selectedYear.value,
                        category = category,
                        type = type,
                        plannedAmountKopecks = newAmountKopecks
                    )
                )
            }
            emitEvent(BudgetEvent.Success(L10n.s.budPlanSaved))
        }
    }

    // v1.11.5: delete the explicit plan for the selected month. Only makes
    // sense for EXPLICIT rows (template / previous-month rows have no row to
    // delete — the dialog hides the button for them).
    fun deleteBudgetPlan(status: BudgetCategoryStatus) {
        viewModelScope.launch {
            val existing = budgetPlanRepository.getByMonthAndCategory(
                _selectedYear.value, _selectedMonth.value,
                status.category, status.type.name
            )
            if (existing != null) {
                budgetPlanRepository.delete(existing)
            }
            emitEvent(BudgetEvent.Success(L10n.s.budPlanDeleted))
        }
    }
}
