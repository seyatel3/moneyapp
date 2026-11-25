package ru.moneyapp.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.moneyapp.data.local.entity.BudgetTemplateEntity

@Dao
interface BudgetTemplateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(template: BudgetTemplateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(templates: List<BudgetTemplateEntity>)

    @Query("DELETE FROM budget_templates WHERE category = :category AND type = :type")
    suspend fun delete(category: String, type: String)

    // v1.10.0: category rename propagation (name column only, type-scoped).
    @Query("UPDATE budget_templates SET category = :newName WHERE category = :oldName AND type = :oldType")
    suspend fun renameCategory(oldName: String, oldType: String, newName: String)

    @Query("DELETE FROM budget_templates")
    suspend fun deleteAll()

    @Query("SELECT * FROM budget_templates ORDER BY type, category")
    fun getAll(): Flow<List<BudgetTemplateEntity>>

    @Query("SELECT * FROM budget_templates ORDER BY type, category")
    suspend fun getAllAsync(): List<BudgetTemplateEntity>
}
