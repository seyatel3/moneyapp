package ru.moneyapp.data.local.entity

import androidx.room.Entity

/**
 * v1.10.0: composite primary key (name, type).
 *
 * The old name-only key made a second «Прочее» (EXPENSE) impossible to store
 * while INCOME «Прочее» existed: the seed INSERT IGNORE silently dropped it,
 * and every name-keyed op (toggle/reorder/delete) could hit both types at once.
 */
@Entity(tableName = "categories", primaryKeys = ["name", "type"])
data class CategoryEntity(
    val name: String,
    val type: String,      // INCOME / EXPENSE / SAVINGS
    val emoji: String,
    val icon: String? = null,   // v1.9.0: icon key; null = automatic by name
    val isVisible: Boolean = true,
    val sortOrder: Int = 0
)
