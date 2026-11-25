package ru.moneyapp.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.moneyapp.data.local.dao.BudgetPlanDao
import ru.moneyapp.data.local.mappers.BudgetPlanMapper
import ru.moneyapp.domain.model.BudgetPlan
import javax.inject.Inject

class BudgetPlanRepository @Inject constructor(
    private val dao: BudgetPlanDao
) {

    fun getByMonth(year: Int, month: Int): Flow<List<BudgetPlan>> =
        dao.getByMonth(year, month).map { BudgetPlanMapper.toDomainList(it) }

    suspend fun getByMonthAsync(year: Int, month: Int): List<BudgetPlan> =
        dao.getByMonthAsync(year, month).map { BudgetPlanMapper.toDomain(it) }

    fun getAll(): Flow<List<BudgetPlan>> =
        dao.getAll().map { BudgetPlanMapper.toDomainList(it) }

    suspend fun getById(id: Int): BudgetPlan? =
        dao.getById(id)?.let { BudgetPlanMapper.toDomain(it) }

    suspend fun getByMonthAndCategory(year: Int, month: Int, category: String, type: String): BudgetPlan? =
        dao.getByMonthAndCategory(year, month, category, type)?.let { BudgetPlanMapper.toDomain(it) }

    suspend fun insert(plan: BudgetPlan): Long =
        dao.insert(BudgetPlanMapper.toEntity(plan))

    suspend fun update(plan: BudgetPlan) {
        dao.update(BudgetPlanMapper.toEntity(plan))
    }

    suspend fun delete(plan: BudgetPlan) =
        dao.delete(BudgetPlanMapper.toEntity(plan))

    suspend fun deleteByMonth(year: Int, month: Int) =
        dao.deleteByMonth(year, month)

    suspend fun insertAll(plans: List<BudgetPlan>) {
        dao.insertAll(plans.map { BudgetPlanMapper.toEntity(it) })
    }
}
