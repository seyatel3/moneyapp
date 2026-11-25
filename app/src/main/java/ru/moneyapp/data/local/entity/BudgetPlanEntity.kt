package ru.moneyapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "budget_plans",
    indices = [
        Index(value = ["year", "month"], name = "idx_budget_year_month")
    ],
)
data class BudgetPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val month: Int,
    val year: Int,
    val category: String,
    val type: String,       // INCOME / EXPENSE / SAVINGS / SUMMARY
    val plannedAmountKopecks: Long
)
