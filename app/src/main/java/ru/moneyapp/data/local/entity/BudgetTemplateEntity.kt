package ru.moneyapp.data.local.entity

import androidx.room.Entity

/**
 * v1.6.0: monthly budget templates (category × type → planned amount).
 * Composite primary key: one template row per (category, type) pair.
 */
@Entity(
    tableName = "budget_templates",
    primaryKeys = ["category", "type"]
)
data class BudgetTemplateEntity(
    val category: String,
    val type: String,        // INCOME / EXPENSE / SAVINGS
    val amountKopecks: Long
)
