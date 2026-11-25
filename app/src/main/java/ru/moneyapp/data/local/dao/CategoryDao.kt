package ru.moneyapp.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.moneyapp.data.local.entity.CategoryEntity

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Update
    suspend fun update(category: CategoryEntity)

    // v1.10.0: composite PK (name, type) — every op is now type-scoped so a
    // second «Прочее» (EXPENSE) can coexist with INCOME «Прочее».
    @Query("SELECT * FROM categories WHERE type = :type AND name = :name")
    suspend fun getByTypeAndName(type: String, name: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE type = :type AND isVisible = 1 ORDER BY sortOrder")
    fun getByType(type: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE type = :type AND isVisible = 1 ORDER BY sortOrder")
    suspend fun getByTypeAsync(type: String): List<CategoryEntity>

    @Query("SELECT * FROM categories ORDER BY type, sortOrder")
    fun getAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY type, sortOrder")
    suspend fun getAllAsync(): List<CategoryEntity>

    @Query("UPDATE categories SET isVisible = :isVisible WHERE type = :type AND name = :name")
    suspend fun setVisibility(type: String, name: String, isVisible: Boolean)

    @Query("UPDATE categories SET sortOrder = :sortOrder WHERE type = :type AND name = :name")
    suspend fun setSortOrder(type: String, name: String, sortOrder: Int)

    @Query("DELETE FROM categories WHERE type = :type AND name = :name")
    suspend fun deleteByTypeAndName(type: String, name: String)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}
