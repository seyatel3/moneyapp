package ru.moneyapp.data.excel

import android.content.Context
import android.net.Uri
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import ru.moneyapp.domain.model.PaymentMethod
import ru.moneyapp.domain.model.Transaction
import ru.moneyapp.domain.model.TransactionType
import timber.log.Timber
import java.io.InputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Imports transactions from an .xlsx file using Apache POI.
 * No GMS dependency — pure Java/Kotlin library.
 *
 * Parses the "Транзакции" sheet layout written by ExcelExporter
 * (row 0 title, row 1 headers, data from row 2), but is tolerant:
 * dates are read as strings (dd.MM.yyyy / ISO) or POI date cells,
 * amounts as numbers or numeric strings, blank cells are skipped,
 * and a broken row is skipped without failing the whole import.
 */
class ExcelImporter(
    private val context: Context
) {

    data class ImportPreview(
        val rowCount: Int,
        val dateRange: String,
        val incomeCount: Int,
        val expenseCount: Int,
        val savingsCount: Int,
        val savingsSpendCount: Int,
        val totalAmount: Double
    )

    companion object {
        private val DATE_PATTERNS = listOf(
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd")
        )
    }

    /**
     * Parse Excel file and return import preview.
     */
    fun getPreview(uri: Uri): ImportPreview {
        val parsed = parseAll(uri)

        var incomeCount = 0
        var expenseCount = 0
        var savingsCount = 0
        var savingsSpendCount = 0
        var totalAmount = 0.0
        var firstDate: LocalDate? = null
        var lastDate: LocalDate? = null

        for (tx in parsed) {
            when (tx.type) {
                TransactionType.INCOME -> incomeCount++
                TransactionType.EXPENSE -> expenseCount++
                TransactionType.SAVINGS -> savingsCount++
                TransactionType.SAVINGS_SPEND -> savingsSpendCount++
            }
            totalAmount += tx.amountRubles
            val d = LocalDate.ofEpochDay(tx.date)
            firstDate = firstDate?.let { if (d.isBefore(it)) d else it } ?: d
            lastDate = lastDate?.let { if (d.isAfter(it)) d else it } ?: d
        }

        val dateRange = if (firstDate != null && lastDate != null) {
            "${firstDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))} — " +
                    "${lastDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}"
        } else {
            "—"
        }

        return ImportPreview(
            rowCount = parsed.size,
            dateRange = dateRange,
            incomeCount = incomeCount,
            expenseCount = expenseCount,
            savingsCount = savingsCount,
            savingsSpendCount = savingsSpendCount,
            totalAmount = totalAmount
        )
    }

    /**
     * Parse all valid transaction rows from the file.
     * Rows that cannot be parsed are skipped (and logged), never fatal.
     */
    fun importTransactions(uri: Uri): List<Transaction> = parseAll(uri)

    private fun parseAll(uri: Uri): List<Transaction> {
        val transactions = mutableListOf<Transaction>()
        var inputStream: InputStream? = null
        var workbook: Workbook? = null
        try {
            inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Не удалось открыть файл")
            workbook = XSSFWorkbook(inputStream)

            val sheet = findTransactionsSheet(workbook)
                ?: throw IllegalStateException("Лист «Транзакции» не найден")

            // Data starts at row 2 (row 0 = title, row 1 = headers).
            for (rowIndex in 2 until sheet.lastRowNum + 1) {
                try {
                    parseRow(sheet.getRow(rowIndex))?.let { transactions.add(it) }
                } catch (e: Exception) {
                    Timber.w("ExcelImporter: skipped row %d: %s", rowIndex, e.message)
                }
            }
        } finally {
            inputStream?.close()
            workbook?.close()
        }
        return transactions
    }

    private fun parseRow(row: Row?): Transaction? {
        if (row == null) return null

        val date = readDate(row.getCell(0)) ?: return null
        val typeStr = readString(row.getCell(1))?.trim()
        val type = when (typeStr) {
            "Доход" -> TransactionType.INCOME
            "Расход" -> TransactionType.EXPENSE
            "Отложить" -> TransactionType.SAVINGS
            "Из накоплений" -> TransactionType.SAVINGS_SPEND
            else -> return null
        }
        val category = readString(row.getCell(2))?.trim().orEmpty()
        if (category.isEmpty()) return null
        val description = readString(row.getCell(3))?.trim().orEmpty().ifEmpty { category }
        val amountRubles = readNumber(row.getCell(4)) ?: return null
        val paymentMethod = when (readString(row.getCell(5))?.trim()?.lowercase()) {
            "нал", "наличные" -> PaymentMethod.CASH
            else -> PaymentMethod.CARD
        }
        val tag = readString(row.getCell(6))?.trim()?.takeIf { it.isNotEmpty() }
        val note = readString(row.getCell(8))?.trim()?.takeIf { it.isNotEmpty() }

        if (amountRubles <= 0.0) return null

        val now = System.currentTimeMillis()
        return Transaction(
            date = date.toEpochDay(),
            type = type,
            category = category,
            description = description,
            amountKopecks = Math.round(amountRubles * 100.0).toLong(),
            paymentMethod = paymentMethod,
            tag = tag,
            note = note,
            createdAt = now,
            updatedAt = now
        )
    }

    private fun findTransactionsSheet(workbook: Workbook): Sheet? {
        for (i in 0 until workbook.numberOfSheets) {
            val sheet = workbook.getSheetAt(i)
            val name = sheet.sheetName.lowercase()
            if (ExcelExporter.TRANSACTIONS_SHEET_KEYWORDS.any { name.contains(it) }) {
                return sheet
            }
        }
        // Fallback: first sheet
        return if (workbook.numberOfSheets > 0) workbook.getSheetAt(0) else null
    }

    /** Cell as trimmed string; null for blank/missing. Safe for all cell types. */
    private fun readString(cell: Cell?): String? {
        if (cell == null) return null
        return try {
            when (cell.cellType) {
                CellType.STRING -> cell.stringCellValue.trim().ifEmpty { null }
                CellType.NUMERIC -> {
                    // Render numbers without scientific notation and trailing .0
                    val d = cell.numericCellValue
                    if (d == d.toLong().toDouble()) d.toLong().toString() else d.toString()
                }
                CellType.BOOLEAN -> cell.booleanCellValue.toString()
                CellType.FORMULA -> try { cell.stringCellValue } catch (e: Exception) { null }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Cell as Double; null for blank/missing/non-numeric. */
    private fun readNumber(cell: Cell?): Double? {
        if (cell == null) return null
        return try {
            when (cell.cellType) {
                CellType.NUMERIC -> cell.numericCellValue
                CellType.STRING -> cell.stringCellValue.trim().toDoubleOrNull()
                CellType.FORMULA -> try { cell.numericCellValue } catch (e: Exception) { null }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Cell as LocalDate: POI date cell, or a string in one of the known
     * formats (the exporter writes dd.MM.yyyy as a plain string).
     */
    private fun readDate(cell: Cell?): LocalDate? {
        if (cell == null) return null
        return try {
            when (cell.cellType) {
                CellType.NUMERIC -> {
                    if (DateUtil.isCellDateFormatted(cell)) {
                        // POI's dateCellValue is a java.util.Date; convert explicitly
                        cell.dateCellValue.toInstant()
                            .atZone(java.time.ZoneOffset.UTC)
                            .toLocalDate()
                    } else {
                        null
                    }
                }
                CellType.STRING -> {
                    val text = cell.stringCellValue.trim()
                    text.takeIf { it.isNotEmpty() }?.let { parseDateText(it) }
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseDateText(text: String): LocalDate? {
        for (pattern in DATE_PATTERNS) {
            try {
                return LocalDate.parse(text, pattern)
            } catch (e: DateTimeParseException) {
                // try the next format
            }
        }
        return null
    }
}