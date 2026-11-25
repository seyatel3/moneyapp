package ru.moneyapp.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.moneyapp.data.local.dao.BudgetTemplateDao
import ru.moneyapp.data.local.mappers.BudgetTemplateMapper
import ru.moneyapp.domain.model.BudgetTemplate
import javax.inject.Inject

class BudgetTemplateRepository @Inject constructor(
    private val dao: BudgetTemplateDao
) {

    fun getAll(): Flow<List<BudgetTemplate>> =
        dao.getAll().map { BudgetTemplateMapper.toDomainList(it) }

    suspend fun getAllAsync(): List<BudgetTemplate> =
        dao.getAllAsync().map { BudgetTemplateMapper.toDomain(it) }

    suspend fun upsert(template: BudgetTemplate) {
        dao.upsert(BudgetTemplateMapper.toEntity(template))
    }

    suspend fun upsertAll(templates: List<BudgetTemplate>) {
        dao.upsertAll(BudgetTemplateMapper.toEntityList(templates))
    }

    suspend fun delete(category: String, type: String) {
        dao.delete(category, type)
    }

    suspend fun deleteAll() {
        dao.deleteAll()
    }
}
