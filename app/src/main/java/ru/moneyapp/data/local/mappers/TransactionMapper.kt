package ru.moneyapp.data.local.mappers

import ru.moneyapp.data.local.entity.TransactionEntity
import ru.moneyapp.domain.model.PaymentMethod
import ru.moneyapp.domain.model.Transaction
import ru.moneyapp.domain.model.TransactionType

object TransactionMapper {

    // Tolerant enum parsing: a broken row (bad type/payment string, e.g. from an
    // external import) must not crash the list screen — fall back to defaults.
    fun toDomain(entity: TransactionEntity): Transaction = Transaction(
        id = entity.id,
        date = entity.date,
        type = runCatching { TransactionType.valueOf(entity.type) }
            .getOrDefault(TransactionType.EXPENSE),
        category = entity.category,
        description = entity.description,
        amountKopecks = entity.amountKopecks,
        paymentMethod = runCatching { PaymentMethod.valueOf(entity.paymentMethod) }
            .getOrDefault(PaymentMethod.CARD),
        tag = entity.tag,
        note = entity.note,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
        syncedAt = entity.syncedAt,
        sheetsRowIndex = entity.sheetsRowIndex
    )

    fun toEntity(domain: Transaction): TransactionEntity = TransactionEntity(
        id = domain.id,
        date = domain.date,
        type = domain.type.name,
        category = domain.category,
        description = domain.description,
        amountKopecks = domain.amountKopecks,
        paymentMethod = domain.paymentMethod.name,
        tag = domain.tag,
        note = domain.note,
        createdAt = domain.createdAt,
        updatedAt = domain.updatedAt,
        syncedAt = domain.syncedAt,
        sheetsRowIndex = domain.sheetsRowIndex
    )

    fun toDomainList(entities: List<TransactionEntity>): List<Transaction> =
        entities.map(::toDomain)

    fun toEntityList(domains: List<Transaction>): List<TransactionEntity> =
        domains.map(::toEntity)
}
