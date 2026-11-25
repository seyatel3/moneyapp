package ru.moneyapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ru.moneyapp.data.local.dao.BudgetPlanDao
import ru.moneyapp.data.local.dao.BudgetTemplateDao
import ru.moneyapp.data.local.dao.CategoryDao
import ru.moneyapp.data.local.dao.SavingsGoalDao
import ru.moneyapp.data.local.dao.TransactionDao
import ru.moneyapp.data.local.entity.BudgetPlanEntity
import ru.moneyapp.data.local.entity.BudgetTemplateEntity
import ru.moneyapp.data.local.entity.CategoryEntity
import ru.moneyapp.data.local.entity.SavingsGoalEntity
import ru.moneyapp.data.local.entity.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        BudgetPlanEntity::class,
        SavingsGoalEntity::class,
        CategoryEntity::class,
        BudgetTemplateEntity::class
    ],
    version = 4,
    exportSchema = true
)
// @TypeConverters(TypeConverters::class) — временно отключено: KSP (2.0.21-1.0.25) падает на этой аннотации
// с ошибкой MissingType и ломает всю сборку; конвертеры сейчас не используются (поля сущностей — Long/Int/String/Boolean)
abstract class MoneyAppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun budgetPlanDao(): BudgetPlanDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetTemplateDao(): BudgetTemplateDao

    companion object {
        const val DATABASE_NAME = "moneyapp_database"

        // v1.6.0: monthly budget templates
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `budget_templates` (" +
                        "`category` TEXT NOT NULL, " +
                        "`type` TEXT NOT NULL, " +
                        "`amountKopecks` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`category`, `type`))"
                )
            }
        }

        // v1.9.0: explicit icon key for categories (NULL = automatic by name)
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `categories` ADD COLUMN `icon` TEXT")
            }
        }

        // v1.10.0: composite PK (name, type) on categories. The old name-only
        // key silently dropped the EXPENSE «Прочее» seed row (INSERT IGNORE hit
        // the INCOME «Прочее» primary key), which left imported transactions
        // orphaned. The table is rebuilt and the defaults are re-seeded on top
        // (INSERT OR IGNORE: existing rows — including user deletions of other
        // defaults — are untouched).
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE `categories_new` (" +
                        "`name` TEXT NOT NULL, " +
                        "`type` TEXT NOT NULL, " +
                        "`emoji` TEXT NOT NULL, " +
                        "`icon` TEXT, " +
                        "`isVisible` INTEGER NOT NULL, " +
                        "`sortOrder` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`name`, `type`))"
                )
                db.execSQL(
                    "INSERT OR IGNORE INTO `categories_new` " +
                        "(name, type, emoji, icon, isVisible, sortOrder) " +
                        "SELECT name, type, emoji, icon, isVisible, sortOrder FROM `categories`"
                )
                db.execSQL("DROP TABLE `categories`")
                db.execSQL("ALTER TABLE `categories_new` RENAME TO `categories`")
                ru.moneyapp.domain.model.DefaultCategories.getAll().forEach { c ->
                    db.execSQL(
                        "INSERT OR IGNORE INTO `categories` " +
                            "(name, type, emoji, icon, isVisible, sortOrder) VALUES (?, ?, ?, ?, ?, ?)",
                        arrayOf<Any?>(c.name, c.type.name, c.emoji, c.icon, c.isVisible, c.sortOrder.toLong())
                    )
                }
            }
        }
    }
}
