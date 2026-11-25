package ru.moneyapp.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.moneyapp.data.local.dao.AvgCheckDaoRow
import ru.moneyapp.data.local.dao.CategoryAmount
import ru.moneyapp.data.local.dao.DailyExpenseDaoRow
import ru.moneyapp.data.local.dao.DailyTotalsDaoRow
import ru.moneyapp.data.local.dao.MonthlyBreakdownRow
import ru.moneyapp.data.local.dao.MonthlySavingsRow
import ru.moneyapp.data.local.dao.MonthlyTotals
import ru.moneyapp.data.local.dao.TopExpenseDaoRow
import ru.moneyapp.data.local.dao.TransactionDao
import ru.moneyapp.data.local.mappers.TransactionMapper
import ru.moneyapp.domain.model.Transaction
import javax.inject.Inject

class TransactionRepository @Inject constructor(
    private val dao: TransactionDao
) {

    fun getAll(): Flow<List<Transaction>> =
        dao.getAll().map { TransactionMapper.toDomainList(it) }

    suspend fun getById(id: String): Transaction? =
        dao.getById(id)?.let { TransactionMapper.toDomain(it) }

    suspend fun insert(transaction: Transaction): Long =
        dao.insert(TransactionMapper.toEntity(transaction))

    suspend fun update(transaction: Transaction) {
        val entity = TransactionMapper.toEntity(transaction.copy(
            updatedAt = System.currentTimeMillis()
        ))
        dao.update(entity)
    }

    suspend fun delete(transaction: Transaction) =
        dao.delete(TransactionMapper.toEntity(transaction))

    suspend fun getMonthlyTotals(startDate: Long, endDate: Long): MonthlyTotals =
        dao.getMonthlyTotals(startDate, endDate)

    // v1.10.2: cumulative totals of all transactions before :endDate.
    suspend fun getTotalsBefore(endDate: Long): MonthlyTotals =
        dao.getTotalsBefore(endDate)

    suspend fun getYearlyTotals(year: Int): MonthlyTotals =
        dao.getYearlyTotals(year)

    suspend fun getExpensesByCategory(startDate: Long, endDate: Long): List<CategoryAmount> =
        dao.getExpensesByCategory(startDate, endDate)

    suspend fun getMonthlyBreakdown(year: Int): List<MonthlyBreakdownRow> =
        dao.getMonthlyBreakdown(year)

    suspend fun getCumulativeSavingsByMonth(year: Int): List<MonthlySavingsRow> =
        dao.getCumulativeSavingsByMonth(year)

    suspend fun getActualExpensesByCategory(startDate: Long, endDate: Long): List<CategoryAmount> =
        dao.getActualExpensesByCategory(startDate, endDate)

    suspend fun getActualIncomeByCategory(startDate: Long, endDate: Long): List<CategoryAmount> =
        dao.getActualIncomeByCategory(startDate, endDate)

    // v1.11.5: monthly SAVINGS actuals (budget parity with the mini-app)
    suspend fun getActualSavingsByCategory(startDate: Long, endDate: Long): List<CategoryAmount> =
        dao.getActualSavingsByCategory(startDate, endDate)

    suspend fun getSavingsForCategory(category: String): Long =
        dao.getSavingsForCategory(category)

    fun getFiltered(
        type: String?,
        category: String?,
        paymentMethod: String?,
        startDate: Long,
        endDate: Long,
        searchQuery: String?
    ): Flow<List<Transaction>> =
        dao.getFiltered(type, category, paymentMethod, startDate, endDate, searchQuery)
            .map { TransactionMapper.toDomainList(it) }

    suspend fun getUnsyncedOrStale(lastSyncTime: Long): List<Transaction> =
        dao.getUnsyncedOrStale(lastSyncTime).map { TransactionMapper.toDomain(it) }

    suspend fun markSynced(id: String, syncTime: Long) =
        dao.markSynced(id, syncTime)

    suspend fun markSyncedWithRow(id: String, syncTime: Long, rowIndex: Int) =
        dao.markSyncedWithRow(id, syncTime, rowIndex)

    suspend fun getCount(): Int = dao.getCount()

    suspend fun deleteAll() = dao.deleteAll()

    suspend fun insertAll(transactions: List<Transaction>) {
        dao.insertAll(transactions.map { TransactionMapper.toEntity(it) })
    }

    // ---- v1.11.0: extended analytics (mirror of the mini-app) ----

    suspend fun getAvgCheckByCategory(startDate: Long, endDate: Long): List<AvgCheckDaoRow> =
        dao.getAvgCheckByCategory(startDate, endDate)

    suspend fun getTopExpenses(startDate: Long, endDate: Long): List<TopExpenseDaoRow> =
        dao.getTopExpenses(startDate, endDate)

    suspend fun getDailyExpenses(startDate: Long, endDate: Long): List<DailyExpenseDaoRow> =
        dao.getDailyExpenses(startDate, endDate)

    suspend fun getDailyTotals(startDate: Long, endDate: Long): List<DailyTotalsDaoRow> =
        dao.getDailyTotals(startDate, endDate)
}
