package ru.moneyapp.domain.model

import java.util.UUID

data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val date: Long,           // LocalDate as epoch day
    val type: TransactionType = TransactionType.EXPENSE,
    val category: String,
    val description: String,
    val amountKopecks: Long,  // always positive, in kopecks
    val paymentMethod: PaymentMethod = PaymentMethod.CARD,
    val tag: String? = null,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncedAt: Long? = null,
    val sheetsRowIndex: Int? = null
) {
    val amountRubles: Double get() = amountKopecks / 100.0
}

enum class TransactionType(val labelRu: String, val xlsxLabel: String) {
    INCOME("Доход", "Доход"),
    EXPENSE("Расход", "Расход"),
    SAVINGS("Отложить", "Отложить"),
    // v1.11.6: spending from the piggy bank. Does NOT touch the wallet balance
    // (the money already left it at the SAVINGS deposit); it reduces the
    // piggy and is counted as an expense in all expense analytics.
    SAVINGS_SPEND("Из накоплений", "Из накоплений")
}

enum class PaymentMethod(val labelRu: String, val xlsxLabel: String) {
    CASH("Наличные", "Нал"),
    CARD("Безнал", "Безнал")
}
