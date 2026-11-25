package ru.moneyapp.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.moneyapp.data.local.dao.SavingsGoalDao
import ru.moneyapp.data.local.mappers.SavingsGoalMapper
import ru.moneyapp.domain.model.SavingsGoal
import javax.inject.Inject

class SavingsGoalRepository @Inject constructor(
    private val dao: SavingsGoalDao
) {

    fun getAll(): Flow<List<SavingsGoal>> =
        dao.getAll().map { SavingsGoalMapper.toDomainList(it) }

    suspend fun getAllAsync(): List<SavingsGoal> =
        dao.getAllAsync().map { SavingsGoalMapper.toDomain(it) }

    suspend fun getById(id: String): SavingsGoal? =
        dao.getById(id)?.let { SavingsGoalMapper.toDomain(it) }

    suspend fun insert(goal: SavingsGoal): Long =
        dao.insert(SavingsGoalMapper.toEntity(goal))

    suspend fun update(goal: SavingsGoal) {
        dao.update(SavingsGoalMapper.toEntity(goal))
    }

    suspend fun delete(goal: SavingsGoal) =
        dao.delete(SavingsGoalMapper.toEntity(goal))
}
