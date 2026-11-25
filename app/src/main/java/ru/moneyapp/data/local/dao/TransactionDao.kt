package ru.moneyapp.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.moneyapp.data.local.entity.TransactionEntity

@Dao
interface TransactionDao {

    // ---- CRUD ----

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: String): TransactionEntity?

    @Query("SELECT * FROM transactions ORDER BY date DESC, createdAt DESC")
    fun getAll(): Flow<List<TransactionEntity>>

    // ---- Queries for Dashboard / Analytics ----

    @Query("""
        SELECT 
            COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amountKopecks ELSE 0 END), 0) as income,
            COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amountKopecks ELSE 0 END), 0) as expenses,
            COALESCE(SUM(CASE WHEN type = 'SAVINGS' THEN amountKopecks ELSE 0 END), 0) as savings,
            COALESCE(SUM(CASE WHEN type = 'SAVINGS_SPEND' THEN amountKopecks ELSE 0 END), 0) as savingsSpend
        FROM transactions
        WHERE date >= :startDate AND date < :endDate
    """)
    suspend fun getMonthlyTotals(startDate: Long, endDate: Long): MonthlyTotals

    // v1.10.2: cumulative totals of everything before :endDate — the balance
    // carried over from all prior periods.
    @Query("""
        SELECT 
            COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amountKopecks ELSE 0 END), 0) as income,
            COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amountKopecks ELSE 0 END), 0) as expenses,
            COALESCE(SUM(CASE WHEN type = 'SAVINGS' THEN amountKopecks ELSE 0 END), 0) as savings,
            COALESCE(SUM(CASE WHEN type = 'SAVINGS_SPEND' THEN amountKopecks ELSE 0 END), 0) as savingsSpend
        FROM transactions
        WHERE date < :endDate
    """)
    suspend fun getTotalsBefore(endDate: Long): MonthlyTotals

    @Query("""
        SELECT 
            COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amountKopecks ELSE 0 END), 0) as income,
            COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amountKopecks ELSE 0 END), 0) as expenses,
            COALESCE(SUM(CASE WHEN type = 'SAVINGS' THEN amountKopecks ELSE 0 END), 0) as savings,
            COALESCE(SUM(CASE WHEN type = 'SAVINGS_SPEND' THEN amountKopecks ELSE 0 END), 0) as savingsSpend
        FROM transactions
        WHERE strftime('%Y', date * 86400, 'unixepoch') = CAST(:year AS TEXT)
    """)
    suspend fun getYearlyTotals(year: Int): MonthlyTotals

    @Query("""
        SELECT category, SUM(amountKopecks) as total
        FROM transactions
        WHERE type IN ('EXPENSE','SAVINGS_SPEND')
          AND date >= :startDate AND date < :endDate
        GROUP BY category
        ORDER BY total DESC
    """)
    suspend fun getExpensesByCategory(startDate: Long, endDate: Long): List<CategoryAmount>

    // Monthly aggregation for bar chart
    @Query("""
        SELECT 
            strftime('%m', date * 86400, 'unixepoch') as month,
            COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amountKopecks ELSE 0 END), 0) as income,
            COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amountKopecks ELSE 0 END), 0) as expenses,
            COALESCE(SUM(CASE WHEN type = 'SAVINGS' THEN amountKopecks ELSE 0 END), 0) as savings,
            COALESCE(SUM(CASE WHEN type = 'SAVINGS_SPEND' THEN amountKopecks ELSE 0 END), 0) as savingsSpend
        FROM transactions
        WHERE strftime('%Y', date * 86400, 'unixepoch') = CAST(:year AS TEXT)
        GROUP BY month
        ORDER BY month
    """)
    suspend fun getMonthlyBreakdown(year: Int): List<MonthlyBreakdownRow>

    // Cumulative savings by month (v1.11.6: NET of SAVINGS_SPEND — money
    // spent from the piggy bank no longer sits in it)
    @Query("""
        SELECT 
            strftime('%m', date * 86400, 'unixepoch') as month,
            COALESCE(SUM(CASE WHEN type = 'SAVINGS' THEN amountKopecks ELSE 0 END), 0)
                - COALESCE(SUM(CASE WHEN type = 'SAVINGS_SPEND' THEN amountKopecks ELSE 0 END), 0) as savings
        FROM transactions
        WHERE type IN ('SAVINGS','SAVINGS_SPEND')
          AND strftime('%Y', date * 86400, 'unixepoch') = CAST(:year AS TEXT)
        GROUP BY month
        ORDER BY month
    """)
    suspend fun getCumulativeSavingsByMonth(year: Int): List<MonthlySavingsRow>

    // ---- Queries for Budget ----

    @Query("""
        SELECT category, SUM(amountKopecks) as total
        FROM transactions
        WHERE type IN ('EXPENSE','SAVINGS_SPEND')
          AND date >= :startDate AND date < :endDate
        GROUP BY category
    """)
    suspend fun getActualExpensesByCategory(startDate: Long, endDate: Long): List<CategoryAmount>

    @Query("""
        SELECT category, SUM(amountKopecks) as total
        FROM transactions
        WHERE type = 'INCOME'
          AND date >= :startDate AND date < :endDate
        GROUP BY category
    """)
    suspend fun getActualIncomeByCategory(startDate: Long, endDate: Long): List<CategoryAmount>

    // v1.11.5: monthly SAVINGS actuals for budget plans (parity with the
    // mini-app, which sums SAVINGS transactions for the month per category).
    @Query("""
        SELECT category, SUM(amountKopecks) as total
        FROM transactions
        WHERE type = 'SAVINGS'
          AND date >= :startDate AND date < :endDate
        GROUP BY category
    """)
    suspend fun getActualSavingsByCategory(startDate: Long, endDate: Long): List<CategoryAmount>

    // ---- Queries for Goals ----

    @Query("""
        SELECT COALESCE(SUM(amountKopecks), 0) as total
        FROM transactions
        WHERE type = 'SAVINGS'
          AND category = :category
    """)
    suspend fun getSavingsForCategory(category: String): Long

    // ---- Filtered queries ----

    @Query("""
        SELECT * FROM transactions
        WHERE (:type IS NULL OR type = :type)
          AND (:category IS NULL OR category = :category)
          AND (:paymentMethod IS NULL OR paymentMethod = :paymentMethod)
          AND date >= :startDate AND date <= :endDate
          AND (:searchQuery IS NULL OR description LIKE '%' || :searchQuery || '%')
        ORDER BY date DESC, createdAt DESC
    """)
    fun getFiltered(
        type: String?,
        category: String?,
        paymentMethod: String?,
        startDate: Long,
        endDate: Long,
        searchQuery: String?
    ): Flow<List<TransactionEntity>>

    // ---- Sync queries ----

    @Query("SELECT * FROM transactions WHERE syncedAt IS NULL OR syncedAt < :lastSyncTime ORDER BY updatedAt DESC")
    suspend fun getUnsyncedOrStale(lastSyncTime: Long): List<TransactionEntity>

    @Query("UPDATE transactions SET syncedAt = :syncTime, updatedAt = :syncTime WHERE id = :id")
    suspend fun markSynced(id: String, syncTime: Long)

    @Query("UPDATE transactions SET syncedAt = :syncTime, updatedAt = :syncTime, sheetsRowIndex = :rowIndex WHERE id = :id")
    suspend fun markSyncedWithRow(id: String, syncTime: Long, rowIndex: Int)

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getCount(): Int

    // v1.10.0: category rename propagation. Type-scoped so a same-named
    // category of a different type (e.g. INCOME vs EXPENSE «Прочее») is untouched.
    @Query("UPDATE transactions SET category = :newName WHERE category = :oldName AND type = :oldType")
    suspend fun renameCategory(oldName: String, oldType: String, newName: String)

    // ---- v1.11.0: extended analytics (mirror of the mini-app /api/analytics) ----

    @Query("""
        SELECT category, COUNT(*) as cnt, SUM(amountKopecks) as total
        FROM transactions
        WHERE type IN ('EXPENSE','SAVINGS_SPEND') AND date >= :startDate AND date < :endDate
        GROUP BY category
        ORDER BY cnt DESC, total DESC
        LIMIT 6
    """)
    suspend fun getAvgCheckByCategory(startDate: Long, endDate: Long): List<AvgCheckDaoRow>

    @Query("""
        SELECT date, category, description, amountKopecks
        FROM transactions
        WHERE type IN ('EXPENSE','SAVINGS_SPEND') AND date >= :startDate AND date < :endDate
        ORDER BY amountKopecks DESC
        LIMIT 10
    """)
    suspend fun getTopExpenses(startDate: Long, endDate: Long): List<TopExpenseDaoRow>

    @Query("""
        SELECT date, SUM(amountKopecks) as total
        FROM transactions
        WHERE type IN ('EXPENSE','SAVINGS_SPEND') AND date >= :startDate AND date < :endDate
        GROUP BY date
    """)
    suspend fun getDailyExpenses(startDate: Long, endDate: Long): List<DailyExpenseDaoRow>

    @Query("""
        SELECT date,
            COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amountKopecks ELSE 0 END), 0) as income,
            COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amountKopecks ELSE 0 END), 0) as expenses,
            COALESCE(SUM(CASE WHEN type = 'SAVINGS' THEN amountKopecks ELSE 0 END), 0) as savings,
            COALESCE(SUM(CASE WHEN type = 'SAVINGS_SPEND' THEN amountKopecks ELSE 0 END), 0) as savingsSpend
        FROM transactions
        WHERE date >= :startDate AND date < :endDate
        GROUP BY date
    """)
    suspend fun getDailyTotals(startDate: Long, endDate: Long): List<DailyTotalsDaoRow>

    // ---- Delete all ----

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}

// ---- Data classes for DAO queries ----

data class MonthlyTotals(
    val income: Long = 0,
    val expenses: Long = 0,
    val savings: Long = 0,
    val savingsSpend: Long = 0
)

data class CategoryAmount(
    val category: String,
    val total: Long
)

data class MonthlyBreakdownRow(
    val month: String,
    val income: Long,
    val expenses: Long,
    val savings: Long,
    val savingsSpend: Long = 0
)

data class MonthlySavingsRow(
    val month: String,
    val savings: Long
)

// ---- v1.11.0: row classes for the extended analytics queries ----

data class AvgCheckDaoRow(
    val category: String,
    val cnt: Long,
    val total: Long
)

data class TopExpenseDaoRow(
    val date: Long,
    val category: String,
    val description: String,
    val amountKopecks: Long
)

data class DailyExpenseDaoRow(
    val date: Long,
    val total: Long
)

data class DailyTotalsDaoRow(
    val date: Long,
    val income: Long,
    val expenses: Long,
    val savings: Long,
    val savingsSpend: Long = 0
)
