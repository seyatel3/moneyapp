package ru.moneyapp.data.local.mappers

import ru.moneyapp.data.local.entity.BudgetPlanEntity
import ru.moneyapp.data.local.entity.CategoryEntity
import ru.moneyapp.data.local.entity.SavingsGoalEntity
import ru.moneyapp.domain.model.*

object BudgetPlanMapper {
    // Tolerant enum parsing: a broken row must not crash the budget screen.
    fun toDomain(entity: BudgetPlanEntity): BudgetPlan = BudgetPlan(
        id = entity.id,
        month = entity.month,
        year = entity.year,
        category = entity.category,
        type = runCatching { BudgetType.valueOf(entity.type) }
            .getOrDefault(BudgetType.EXPENSE),
        plannedAmountKopecks = entity.plannedAmountKopecks
    )

    fun toEntity(domain: BudgetPlan): BudgetPlanEntity = BudgetPlanEntity(
        id = domain.id,
        month = domain.month,
        year = domain.year,
        category = domain.category,
        type = domain.type.name,
        plannedAmountKopecks = domain.plannedAmountKopecks
    )

    fun toDomainList(entities: List<BudgetPlanEntity>): List<BudgetPlan> =
        entities.map(::toDomain)
}

object SavingsGoalMapper {
    fun toDomain(entity: SavingsGoalEntity): SavingsGoal = SavingsGoal(
        id = entity.id,
        name = entity.name,
        description = entity.description,
        targetAmountKopecks = entity.targetAmountKopecks,
        deadline = entity.deadline,
        createdAt = entity.createdAt
    )

    fun toEntity(domain: SavingsGoal): SavingsGoalEntity = SavingsGoalEntity(
        id = domain.id,
        name = domain.name,
        description = domain.description,
        targetAmountKopecks = domain.targetAmountKopecks,
        deadline = domain.deadline,
        createdAt = domain.createdAt
    )

    fun toDomainList(entities: List<SavingsGoalEntity>): List<SavingsGoal> =
        entities.map(::toDomain)
}

object CategoryMapper {
    // Tolerant enum parsing: a broken row must not crash category lists.
    fun toDomain(entity: CategoryEntity): Category = Category(
        name = entity.name,
        type = runCatching { TransactionType.valueOf(entity.type) }
            .getOrDefault(TransactionType.EXPENSE),
        emoji = entity.emoji,
        icon = entity.icon,
        isVisible = entity.isVisible,
        sortOrder = entity.sortOrder
    )

    fun toEntity(domain: Category): CategoryEntity = CategoryEntity(
        name = domain.name,
        type = domain.type.name,
        emoji = domain.emoji,
        icon = domain.icon,
        isVisible = domain.isVisible,
        sortOrder = domain.sortOrder
    )

    fun toDomainList(entities: List<CategoryEntity>): List<Category> =
        entities.map(::toDomain)
}
