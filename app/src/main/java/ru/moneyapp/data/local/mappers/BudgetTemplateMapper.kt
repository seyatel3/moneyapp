package ru.moneyapp.data.local.mappers

import ru.moneyapp.data.local.entity.BudgetTemplateEntity
import ru.moneyapp.domain.model.BudgetTemplate
import ru.moneyapp.domain.model.BudgetType

object BudgetTemplateMapper {

    // Tolerant enum parsing: a broken row must not crash the settings screen.
    fun toDomain(entity: BudgetTemplateEntity): BudgetTemplate = BudgetTemplate(
        category = entity.category,
        type = runCatching { BudgetType.valueOf(entity.type) }
            .getOrDefault(BudgetType.EXPENSE),
        amountKopecks = entity.amountKopecks
    )

    fun toEntity(domain: BudgetTemplate): BudgetTemplateEntity = BudgetTemplateEntity(
        category = domain.category,
        type = domain.type.name,
        amountKopecks = domain.amountKopecks
    )

    fun toDomainList(entities: List<BudgetTemplateEntity>): List<BudgetTemplate> =
        entities.map(::toDomain)

    fun toEntityList(templates: List<BudgetTemplate>): List<BudgetTemplateEntity> =
        templates.map(::toEntity)
}
