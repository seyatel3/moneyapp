package ru.moneyapp.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.moneyapp.data.repository.SavingsGoalRepository
import ru.moneyapp.data.repository.TransactionRepository
import ru.moneyapp.domain.model.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val savingsGoalRepository: SavingsGoalRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    val goals: Flow<List<SavingsGoalState>> = savingsGoalRepository.getAll().flatMapLatest { goalsList ->
        kotlinx.coroutines.flow.flow {
                val states = goalsList.map { goal ->
                    // Compute current amount from transactions
                    val currentAmount = transactionRepository.getSavingsForCategory(goal.name)
                    val remaining = goal.targetAmountKopecks - currentAmount
                    val progress = if (goal.targetAmountKopecks > 0)
                        currentAmount.toFloat() / goal.targetAmountKopecks.toFloat()
                    else 0f

                    // Compute recommended monthly contribution
                    val today = LocalDate.now()
                    val daysRemaining = ChronoUnit.DAYS.between(today, LocalDate.ofEpochDay(goal.deadline))
                    val monthsRemaining = maxOf(1, (daysRemaining / 30).toInt())
                    val recommendedMonthly = if (monthsRemaining > 0) remaining / monthsRemaining else 0L

                    // Estimated completion
                    val estimatedCompletion = if (recommendedMonthly > 0 && remaining > 0) {
                        val monthsToReach = ((remaining + recommendedMonthly - 1) / recommendedMonthly)
                        today.plusMonths(monthsToReach.toLong()).toEpochDay()
                    } else null

                    val status = when {
                        currentAmount >= goal.targetAmountKopecks -> GoalStatus.COMPLETED
                        daysRemaining < 0 -> GoalStatus.OVERDUE
                        else -> GoalStatus.IN_PROGRESS
                    }

                    SavingsGoalState(
                        goal = goal,
                        currentAmountKopecks = currentAmount,
                        remainingAmountKopecks = remaining.coerceAtLeast(0),
                        progressPercent = progress.coerceIn(0f, 1f),
                        recommendedMonthlyKopecks = recommendedMonthly,
                        estimatedCompletionDay = estimatedCompletion,
                        status = status
                    )
                }
                emit(states)
        }
    }

    fun createGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            savingsGoalRepository.insert(goal)
        }
    }

    fun updateGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            savingsGoalRepository.update(goal)
        }
    }

    fun deleteGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            savingsGoalRepository.delete(goal)
        }
    }

    fun addContribution(goalId: String, amountKopecks: Long, category: String) {
        viewModelScope.launch {
            val transaction = ru.moneyapp.domain.model.Transaction(
                date = LocalDate.now().toEpochDay(),
                type = TransactionType.SAVINGS,
                category = category,
                description = "Взнос в цель",
                amountKopecks = amountKopecks,
                note = "Цель: $goalId"
            )
            transactionRepository.insert(transaction)
        }
    }
}
