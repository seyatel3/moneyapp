package ru.moneyapp.domain.model

import java.util.UUID

data class SavingsGoal(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val targetAmountKopecks: Long,
    val deadline: Long,        // epoch day
    val createdAt: Long = System.currentTimeMillis()
) {
    val targetAmountRubles: Double get() = targetAmountKopecks / 100.0
}

/**
 * Computed goal state — not stored, derived from transactions + goal.
 */
data class SavingsGoalState(
    val goal: SavingsGoal,
    val currentAmountKopecks: Long,
    val remainingAmountKopecks: Long,
    val progressPercent: Float,
    val recommendedMonthlyKopecks: Long,
    val estimatedCompletionDay: Long?,
    val status: GoalStatus
) {
    val currentAmountRubles: Double get() = currentAmountKopecks / 100.0
    val remainingAmountRubles: Double get() = remainingAmountKopecks / 100.0
    val recommendedMonthlyRubles: Double get() = recommendedMonthlyKopecks / 100.0
}

enum class GoalStatus(val labelRu: String, val emoji: String) {
    IN_PROGRESS("В процессе", "🔄"),
    COMPLETED("Выполнено", "✅"),
    OVERDUE("Просрочено", "⏰")
}
