package ru.moneyapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey val id: String,        // UUID
    val name: String,
    val description: String,
    val targetAmountKopecks: Long,
    val deadline: Long,                // epoch day
    val createdAt: Long
)
