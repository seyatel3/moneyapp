package ru.moneyapp.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ru.moneyapp.data.local.MoneyAppDatabase
import ru.moneyapp.data.local.dao.BudgetPlanDao
import ru.moneyapp.data.local.dao.BudgetTemplateDao
import ru.moneyapp.data.local.dao.CategoryDao
import ru.moneyapp.data.local.dao.SavingsGoalDao
import ru.moneyapp.data.local.dao.TransactionDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MoneyAppDatabase {
        return Room.databaseBuilder(
            context,
            MoneyAppDatabase::class.java,
            MoneyAppDatabase.DATABASE_NAME
        )
            .addMigrations(
                MoneyAppDatabase.MIGRATION_1_2,
                MoneyAppDatabase.MIGRATION_2_3,
                MoneyAppDatabase.MIGRATION_3_4
            )
            .build()
    }

    @Provides
    fun provideTransactionDao(database: MoneyAppDatabase): TransactionDao =
        database.transactionDao()

    @Provides
    fun provideBudgetPlanDao(database: MoneyAppDatabase): BudgetPlanDao =
        database.budgetPlanDao()

    @Provides
    fun provideSavingsGoalDao(database: MoneyAppDatabase): SavingsGoalDao =
        database.savingsGoalDao()

    @Provides
    fun provideCategoryDao(database: MoneyAppDatabase): CategoryDao =
        database.categoryDao()

    @Provides
    fun provideBudgetTemplateDao(database: MoneyAppDatabase): BudgetTemplateDao =
        database.budgetTemplateDao()
}
