package ru.moneyapp.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.moneyapp.data.local.dao.BudgetPlanDao
import ru.moneyapp.data.local.dao.BudgetTemplateDao
import ru.moneyapp.data.local.dao.CategoryDao
import ru.moneyapp.data.local.dao.TransactionDao
import ru.moneyapp.data.local.mappers.CategoryMapper
import ru.moneyapp.domain.model.Category
import ru.moneyapp.domain.model.TransactionType
import javax.inject.Inject

class CategoryRepository @Inject constructor(
    private val dao: CategoryDao,
    // v1.10.0: injected so edit() can propagate a rename to the tables that
    // reference categories by (name, type).
    private val transactionDao: TransactionDao,
    private val budgetPlanDao: BudgetPlanDao,
    private val budgetTemplateDao: BudgetTemplateDao
) {

    fun getAll(): Flow<List<Category>> =
        dao.getAll().map { CategoryMapper.toDomainList(it) }

    suspend fun getAllAsync(): List<Category> =
        dao.getAllAsync().map { CategoryMapper.toDomain(it) }

    fun getByType(type: TransactionType): Flow<List<Category>> =
        dao.getByType(type.name).map { CategoryMapper.toDomainList(it) }

    suspend fun getByTypeAsync(type: TransactionType): List<Category> =
        dao.getByTypeAsync(type.name).map { CategoryMapper.toDomain(it) }

    // v1.10.0: composite lookup (name alone is no longer unique).
    suspend fun getByTypeAndName(type: TransactionType, name: String): Category? =
        dao.getByTypeAndName(type.name, name)?.let { CategoryMapper.toDomain(it) }

    suspend fun insert(category: Category): Long =
        dao.insert(CategoryMapper.toEntity(category))

    suspend fun update(category: Category) {
        dao.update(CategoryMapper.toEntity(category))
    }

    suspend fun setVisibility(type: TransactionType, name: String, isVisible: Boolean) =
        dao.setVisibility(type.name, name, isVisible)

    suspend fun setSortOrder(type: TransactionType, name: String, sortOrder: Int) =
        dao.setSortOrder(type.name, name, sortOrder)

    suspend fun delete(type: TransactionType, name: String) =
        dao.deleteByTypeAndName(type.name, name)

    /**
     * v1.10.0: edit a category (rename and/or change type + icon/visibility).
     *
     * The rename propagates to every table that stores a category name:
     *   transactions.category, budget_plans.category, budget_templates.category —
     * scoped by the OLD type so a same-named category of another type is untouched.
     *
     * A type change keeps the transactions' own type (financial semantics); only
     * the stored name follows the category. When the (name, type) identity
     * changes we delete + re-insert the row, since @Update keys on the PK.
     */
    suspend fun edit(old: Category, new: Category) {
        val nameChanged = old.name != new.name
        val typeChanged = old.type != new.type
        if (nameChanged || typeChanged) {
            val oldTypeStr = old.type.name
            if (nameChanged) {
                transactionDao.renameCategory(old.name, oldTypeStr, new.name)
                budgetPlanDao.renameCategory(old.name, oldTypeStr, new.name)
                budgetTemplateDao.renameCategory(old.name, oldTypeStr, new.name)
            }
            dao.deleteByTypeAndName(old.name, old.type.name)
            dao.insert(CategoryMapper.toEntity(new))
        } else {
            // Same identity — only icon / visibility / sortOrder changed.
            dao.update(CategoryMapper.toEntity(new))
        }
    }

    suspend fun seedDefaults() {
        dao.insertAll(ru.moneyapp.domain.model.DefaultCategories.getAll().map { CategoryMapper.toEntity(it) })
    }
}
