package ru.moneyapp.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.moneyapp.data.local.entity.SavingsGoalEntity

@Dao
interface SavingsGoalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: SavingsGoalEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(goals: List<SavingsGoalEntity>)

    @Update
    suspend fun update(goal: SavingsGoalEntity)

    @Delete
    suspend fun delete(goal: SavingsGoalEntity)

    @Query("SELECT * FROM savings_goals WHERE id = :id")
    suspend fun getById(id: String): SavingsGoalEntity?

    @Query("SELECT * FROM savings_goals ORDER BY deadline ASC")
    fun getAll(): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals ORDER BY deadline ASC")
    suspend fun getAllAsync(): List<SavingsGoalEntity>

    @Query("DELETE FROM savings_goals")
    suspend fun deleteAll()
}
