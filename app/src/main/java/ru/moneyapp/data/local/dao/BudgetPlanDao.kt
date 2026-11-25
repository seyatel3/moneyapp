package ru.moneyapp.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.moneyapp.data.local.entity.BudgetPlanEntity

@Dao
interface BudgetPlanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plan: BudgetPlanEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(plans: List<BudgetPlanEntity>)

    @Update
    suspend fun update(plan: BudgetPlanEntity)

    @Delete
    suspend fun delete(plan: BudgetPlanEntity)

    @Query("SELECT * FROM budget_plans WHERE id = :id")
    suspend fun getById(id: Int): BudgetPlanEntity?

    @Query("SELECT * FROM budget_plans WHERE year = :year AND month = :month ORDER BY category")
    fun getByMonth(year: Int, month: Int): Flow<List<BudgetPlanEntity>>

    @Query("SELECT * FROM budget_plans WHERE year = :year AND month = :month ORDER BY category")
    suspend fun getByMonthAsync(year: Int, month: Int): List<BudgetPlanEntity>

    @Query("SELECT * FROM budget_plans ORDER BY year DESC, month DESC")
    fun getAll(): Flow<List<BudgetPlanEntity>>

    @Query("SELECT * FROM budget_plans WHERE year = :year AND month = :month AND category = :category AND type = :type LIMIT 1")
    suspend fun getByMonthAndCategory(year: Int, month: Int, category: String, type: String): BudgetPlanEntity?

    @Query("DELETE FROM budget_plans WHERE year = :year AND month = :month")
    suspend fun deleteByMonth(year: Int, month: Int)

    // v1.10.0: category rename propagation (name column only, type-scoped).
    @Query("UPDATE budget_plans SET category = :newName WHERE category = :oldName AND type = :oldType")
    suspend fun renameCategory(oldName: String, oldType: String, newName: String)

    @Query("DELETE FROM budget_plans")
    suspend fun deleteAll()
}
