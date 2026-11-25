package ru.moneyapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["date"], name = "idx_transaction_date"),
        Index(value = ["type"], name = "idx_transaction_type"),
        Index(value = ["category"], name = "idx_transaction_category"),
        Index(value = ["syncedAt"], name = "idx_transaction_synced")
    ]
)
data class TransactionEntity(
    @PrimaryKey val id: String,        // UUID as String
    val date: Long,                    // LocalDate as epoch day
    val type: String,                  // INCOME / EXPENSE / SAVINGS
    val category: String,
    val description: String,
    val amountKopecks: Long,           // always positive, in kopecks
    val paymentMethod: String,         // CASH / CARD
    val tag: String?,
    val note: String?,
    val createdAt: Long,               // epoch millis
    val updatedAt: Long,
    val syncedAt: Long?,
    val sheetsRowIndex: Int?
)
