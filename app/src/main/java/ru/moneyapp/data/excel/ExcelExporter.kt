package ru.moneyapp.data.excel

import android.content.Context
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import ru.moneyapp.domain.model.BudgetPlan
import ru.moneyapp.domain.model.BudgetType
import ru.moneyapp.domain.model.BudgetTemplate
import ru.moneyapp.domain.model.SavingsGoal
import ru.moneyapp.domain.model.Transaction
import ru.moneyapp.domain.model.TransactionType
import java.io.OutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Exports data to .xlsx format using Apache POI.
 * No GMS dependency — pure Java/Kotlin library.
 *
 * Column widths are fixed: POI's autoSizeColumn() needs AWT FontMetrics,
 * which is unavailable on Android and throws at runtime.
 */
class ExcelExporter(
    private val context: Context
) {

    companion object {
        val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

        // "Транзакции" sheet layout — the importer parses exactly this.
        // Row 0: title, row 1: headers, data starts at row 2.
        // 0=Дата, 1=Тип, 2=Категория, 3=Описание, 4=Сумма (₽),
        // 5=Нал/Безнал, 6=Тег, 7=Бюджет %, 8=Примечание
        val TRANSACTIONS_SHEET_KEYWORDS = listOf("транзак", "transaction")
    }

    fun exportToStream(
        outputStream: OutputStream,
        transactions: List<Transaction>,
        budgetPlans: List<BudgetPlan>,
        goals: List<SavingsGoal>,
        budgetTemplates: List<BudgetTemplate> = emptyList()
    ) {
        val workbook = XSSFWorkbook()
        try {
            createTransactionsSheet(workbook, transactions)
            createDashboardSheet(workbook, transactions)
            createBudgetSheet(workbook, budgetPlans, transactions)
            createGoalsSheet(workbook, goals, transactions)
            if (budgetTemplates.isNotEmpty()) {
                createBudgetTemplateSheet(workbook, budgetTemplates)
            }
            workbook.write(outputStream)
        } finally {
            workbook.close()
        }
    }

    private fun createTransactionsSheet(workbook: Workbook, transactions: List<Transaction>) {
        val sheet = workbook.createSheet("📋 Транзакции")

        // Title
        val titleRow = sheet.createRow(0)
        titleRow.createCell(0).setCellValue("💰 УЧЁТ ДОХОДОВ И РАСХОДОВ")

        // Headers
        val subHeaderRow = sheet.createRow(1)
        listOf(
            "Дата", "Тип", "Категория", "Описание", "Сумма (₽)",
            "Нал/Безнал", "Тег", "Бюджет %", "Примечание"
        ).forEachIndexed { i, header ->
            subHeaderRow.createCell(i).setCellValue(header)
        }

        // Data rows — sorted by date so the sheet reads like a statement
        transactions.sortedWith(compareBy<Transaction>({ it.date }, { it.createdAt }))
            .forEachIndexed { index, tx ->
                val row = sheet.createRow(index + 2)
                row.createCell(0).setCellValue(LocalDate.ofEpochDay(tx.date).format(DATE_FORMAT))
                row.createCell(1).setCellValue(tx.type.xlsxLabel)
                row.createCell(2).setCellValue(tx.category)
                row.createCell(3).setCellValue(tx.description)
                row.createCell(4).setCellValue(tx.amountKopecks / 100.0)
                row.createCell(5).setCellValue(tx.paymentMethod.xlsxLabel)
                row.createCell(6).setCellValue(tx.tag ?: "")
                row.createCell(7).setCellValue(0.0)
                row.createCell(8).setCellValue(tx.note ?: "")
            }

        // Fixed widths (autoSizeColumn crashes on Android — no AWT)
        intArrayOf(12, 10, 18, 30, 14, 12, 12, 10, 24).forEachIndexed { i, w ->
            sheet.setColumnWidth(i, w * 256)
        }
    }

    private fun createDashboardSheet(workbook: Workbook, transactions: List<Transaction>) {
        val sheet = workbook.createSheet("📊 Дашборд")

        val titleRow = sheet.createRow(0)
        titleRow.createCell(1).setCellValue("💰 ЛИЧНЫЙ ФИНАНСОВЫЙ ДАШБОРД")

        // KPI row
        // v1.11.6: «ОТЛОЖЕНО» is the piggy NET of SAVINGS_SPEND (parity with
        // the dashboard KPI); the wallet balance formula is untouched.
        val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amountKopecks }
        val totalExpenses = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountKopecks }
        val totalSavings = transactions.filter { it.type == TransactionType.SAVINGS }.sumOf { it.amountKopecks }
        val totalSavingsSpend = transactions.filter { it.type == TransactionType.SAVINGS_SPEND }.sumOf { it.amountKopecks }
        val balance = totalIncome - totalExpenses - totalSavings
        val savingsRate = if (totalIncome > 0) totalSavings.toDouble() / totalIncome else 0.0

        val kpiHeaderRow = sheet.createRow(4)
        kpiHeaderRow.createCell(1).setCellValue("📈 ДОХОДЫ")
        kpiHeaderRow.createCell(3).setCellValue("📉 РАСХОДЫ")
        kpiHeaderRow.createCell(5).setCellValue("🎯 ОТЛОЖЕНО")
        kpiHeaderRow.createCell(7).setCellValue("💵 ОСТАТОК")
        kpiHeaderRow.createCell(9).setCellValue("💡 НОРМА СБЕРЕЖЕНИЙ")

        val kpiValueRow = sheet.createRow(5)
        kpiValueRow.createCell(1).setCellValue(totalIncome / 100.0)
        kpiValueRow.createCell(3).setCellValue(totalExpenses / 100.0)
        kpiValueRow.createCell(5).setCellValue((totalSavings - totalSavingsSpend) / 100.0)
        kpiValueRow.createCell(7).setCellValue(balance / 100.0)
        kpiValueRow.createCell(9).setCellValue(String.format("%d%%", (savingsRate * 100).toInt()))

        // Monthly summary (left block)
        val summaryHeaderRow = sheet.createRow(7)
        summaryHeaderRow.createCell(1).setCellValue("📅 ПОМЕСЯЧНАЯ СВОДКА")

        val monthHeaderRow = sheet.createRow(8)
        listOf("Месяц", "Доходы", "Расходы", "Отложено", "Остаток").forEachIndexed { i, header ->
            monthHeaderRow.createCell(i + 1).setCellValue(header)
        }

        val monthNames = listOf(
            "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
            "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
        )
        val byMonth = transactions.groupBy {
            val d = LocalDate.ofEpochDay(it.date)
            d.monthValue to d.year
        }
        val months = byMonth.keys.sortedBy { (m, y) -> y * 100 + m }
        months.forEachIndexed { index, (month, year) ->
            val rows = byMonth.getValue(month to year)
            val mIncome = rows.filter { it.type == TransactionType.INCOME }.sumOf { it.amountKopecks }
            val mExpenses = rows.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountKopecks }
            val mSavings = rows.filter { it.type == TransactionType.SAVINGS }.sumOf { it.amountKopecks }
            // v1.11.6: «Отложено» column is the piggy NET of SAVINGS_SPEND;
            // «Остаток» keeps the wallet formula (income − expenses − savings).
            val mSavingsSpend = rows.filter { it.type == TransactionType.SAVINGS_SPEND }.sumOf { it.amountKopecks }
            val row = sheet.createRow(index + 9)
            row.createCell(1).setCellValue("${monthNames[month - 1]} $year")
            row.createCell(2).setCellValue(mIncome / 100.0)
            row.createCell(3).setCellValue(mExpenses / 100.0)
            row.createCell(4).setCellValue((mSavings - mSavingsSpend) / 100.0)
            row.createCell(5).setCellValue((mIncome - mExpenses - mSavings) / 100.0)
        }

        // Expense breakdown (right block)
        val breakdownHeaderRow = sheet.createRow(7)
        breakdownHeaderRow.createCell(7).setCellValue("📊 РАСХОДЫ ПО КАТЕГОРИЯМ")

        val catHeaderRow = sheet.createRow(8)
        listOf("Категория", "Сумма", "% от расходов", "% от дохода").forEachIndexed { i, header ->
            catHeaderRow.createCell(i + 7).setCellValue(header)
        }

        // v1.11.6: SAVINGS_SPEND is an expense in all expense analytics —
        // include it in the breakdown (percent denominator follows the same).
        val expenses = transactions.filter {
            it.type == TransactionType.EXPENSE || it.type == TransactionType.SAVINGS_SPEND
        }
        val byCategory = expenses.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amountKopecks } }
            .map { (category, kopecks) -> category to kopecks }
            .sortedByDescending { it.second }
        val breakdownTotal = byCategory.sumOf { it.second }
        byCategory.forEachIndexed { index, (category, kopecks) ->
            val row = sheet.createRow(index + 9)
            row.createCell(7).setCellValue(category)
            row.createCell(8).setCellValue(kopecks / 100.0)
            row.createCell(9).setCellValue(
                if (breakdownTotal > 0) String.format("%.1f%%", kopecks.toDouble() / breakdownTotal * 100) else "—"
            )
            row.createCell(10).setCellValue(
                if (totalIncome > 0) String.format("%.1f%%", kopecks.toDouble() / totalIncome * 100) else "—"
            )
        }

        // Fixed widths
        intArrayOf(2, 14, 14, 14, 14, 14, 2, 20, 14, 16, 16).forEachIndexed { i, w ->
            sheet.setColumnWidth(i, w * 256)
        }
    }

    private fun createBudgetSheet(
        workbook: Workbook,
        budgetPlans: List<BudgetPlan>,
        transactions: List<Transaction>
    ) {
        val sheet = workbook.createSheet("📝 Бюджет")

        val titleRow = sheet.createRow(0)
        titleRow.createCell(1).setCellValue("📝 ПЛАН БЮДЖЕТА (ВСЕ МЕСЯЦЫ)")

        val headerRow = sheet.createRow(3)
        listOf("Месяц", "Категория", "Тип", "Бюджет (план)", "Факт", "Разница", "% исполнения", "Статус")
            .forEachIndexed { i, header ->
                headerRow.createCell(i + 1).setCellValue(header)
            }

        val monthNames = listOf(
            "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
            "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
        )
        budgetPlans
            .sortedWith(compareBy<BudgetPlan>({ it.year }, { it.month }, { it.category }))
            .forEachIndexed { index, plan ->
                val row = sheet.createRow(index + 4)
                val actual = actualForMonthCategory(
                    transactions, plan.year, plan.month, plan.category, plan.type
                )
                val difference = actual - plan.plannedAmountKopecks
                val percent = if (plan.plannedAmountKopecks > 0)
                    actual.toDouble() / plan.plannedAmountKopecks * 100 else 0.0
                val status = when {
                    plan.plannedAmountKopecks == 0L -> "—"
                    percent >= 100.0 -> "⛔ Перебор"
                    percent >= 90.0 -> "⚠️ Почти"
                    else -> "✅ OK"
                }
                row.createCell(1).setCellValue("${monthNames[plan.month - 1]} ${plan.year}")
                row.createCell(2).setCellValue(plan.category)
                row.createCell(3).setCellValue(plan.type.labelRu)
                row.createCell(4).setCellValue(plan.plannedAmountKopecks / 100.0)
                row.createCell(5).setCellValue(actual / 100.0)
                row.createCell(6).setCellValue(difference / 100.0)
                row.createCell(7).setCellValue(String.format("%.0f%%", percent))
                row.createCell(8).setCellValue(status)
            }

        // Fixed widths
        intArrayOf(2, 14, 20, 12, 14, 14, 12, 14, 12).forEachIndexed { i, w ->
            sheet.setColumnWidth(i, w * 256)
        }
    }

    private fun actualForMonthCategory(
        transactions: List<Transaction>,
        year: Int,
        month: Int,
        category: String,
        planType: BudgetType
    ): Long {
        val start = LocalDate.of(year, month, 1).toEpochDay()
        val end = LocalDate.of(year, month, 1).plusMonths(1).toEpochDay()
        // v1.11.6: actuals follow the plan type; an EXPENSE plan also counts
        // SAVINGS_SPEND (parity with the in-app budget screen).
        return transactions
            .filter { tx ->
                val typeMatch = when (planType) {
                    BudgetType.EXPENSE ->
                        tx.type == TransactionType.EXPENSE || tx.type == TransactionType.SAVINGS_SPEND
                    BudgetType.INCOME -> tx.type == TransactionType.INCOME
                    BudgetType.SAVINGS -> tx.type == TransactionType.SAVINGS
                    // SUMMARY plans are not created (v1.11.5); legacy behaviour.
                    BudgetType.SUMMARY -> tx.type != TransactionType.SAVINGS
                }
                tx.category == category &&
                        tx.date in start until end &&
                        typeMatch
            }
            .sumOf { it.amountKopecks }
    }

    private fun createGoalsSheet(
        workbook: Workbook,
        goals: List<SavingsGoal>,
        transactions: List<Transaction>
    ) {
        val sheet = workbook.createSheet("🎯 Цели")

        val titleRow = sheet.createRow(0)
        titleRow.createCell(1).setCellValue("🎯 ЦЕЛИ НАКОПЛЕНИЙ")

        val headerRow = sheet.createRow(3)
        listOf("Цель", "Описание", "Целевая сумма", "Уже накоплено", "Осталось",
            "% выполнения", "Срок", "Мес. взнос", "Статус")
            .forEachIndexed { i, header ->
                headerRow.createCell(i + 1).setCellValue(header)
            }

        val today = LocalDate.now()
        goals.forEachIndexed { index, goal ->
            val saved = transactions
                .filter { it.category == goal.name }
                .sumOf { it.amountKopecks }
            val remaining = (goal.targetAmountKopecks - saved).coerceAtLeast(0)
            val percent = if (goal.targetAmountKopecks > 0)
                saved.toDouble() / goal.targetAmountKopecks * 100 else 0.0
            val deadlineDate = LocalDate.ofEpochDay(goal.deadline)
            val daysLeft = (deadlineDate.toEpochDay() - today.toEpochDay()).coerceAtLeast(0)
            val monthsLeft = (daysLeft / 30).coerceAtLeast(1)
            val monthly = remaining / monthsLeft

            val status = when {
                saved >= goal.targetAmountKopecks -> "✅ Выполнено"
                daysLeft == 0L -> "⛔ Просрочено"
                else -> "🔄 В процессе"
            }

            val row = sheet.createRow(index + 4)
            row.createCell(1).setCellValue(goal.name)
            row.createCell(2).setCellValue(goal.description)
            row.createCell(3).setCellValue(goal.targetAmountKopecks / 100.0)
            row.createCell(4).setCellValue(saved / 100.0)
            row.createCell(5).setCellValue(remaining / 100.0)
            row.createCell(6).setCellValue(String.format("%.0f%%", percent))
            row.createCell(7).setCellValue(deadlineDate.format(DATE_FORMAT))
            row.createCell(8).setCellValue(monthly / 100.0)
            row.createCell(9).setCellValue(status)
        }

        // Fixed widths
        intArrayOf(2, 20, 24, 14, 14, 12, 12, 12, 12, 14).forEachIndexed { i, w ->
            sheet.setColumnWidth(i, w * 256)
        }
    }

    private fun createBudgetTemplateSheet(
        workbook: Workbook,
        budgetTemplates: List<BudgetTemplate>
    ) {
        val sheet = workbook.createSheet("📐 Шаблон бюджета")

        val titleRow = sheet.createRow(0)
        titleRow.createCell(1).setCellValue("📐 ШАБЛОН БЮДЖЕТА НА МЕСЯЦ")

        val headerRow = sheet.createRow(2)
        listOf("Категория", "Тип", "Сумма (₽)").forEachIndexed { i, header ->
            headerRow.createCell(i + 1).setCellValue(header)
        }

        budgetTemplates
            .sortedWith(compareBy<BudgetTemplate>({ it.type }, { it.category }))
            .forEachIndexed { index, template ->
                val row = sheet.createRow(index + 3)
                row.createCell(1).setCellValue(template.category)
                row.createCell(2).setCellValue(template.type.labelRu)
                row.createCell(3).setCellValue(template.amountKopecks / 100.0)
            }

        // Fixed widths
        intArrayOf(2, 24, 14, 14).forEachIndexed { i, w ->
            sheet.setColumnWidth(i, w * 256)
        }
    }
}