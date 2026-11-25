package ru.moneyapp.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.moneyapp.data.repository.CategoryRepository
import ru.moneyapp.data.repository.TransactionRepository
import ru.moneyapp.domain.model.*
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _filterState = MutableStateFlow(FilterState())
    val filterState = _filterState.asStateFlow()

    val transactions: Flow<List<Transaction>> = filterState.flatMapLatest { filter ->
        transactionRepository.getFiltered(
            type = filter.type?.name,
            category = filter.category,
            paymentMethod = filter.paymentMethod?.name,
            startDate = filter.startDate,
            endDate = filter.endDate,
            searchQuery = filter.searchQuery
        )
    }

    val incomeCategories: Flow<List<Category>> = categoryRepository.getByType(TransactionType.INCOME)
    val expenseCategories: Flow<List<Category>> = categoryRepository.getByType(TransactionType.EXPENSE)
    val savingsCategories: Flow<List<Category>> = categoryRepository.getByType(TransactionType.SAVINGS)

    fun setFilterType(type: TransactionType?) {
        _filterState.value = _filterState.value.copy(type = type)
    }

    fun setFilterCategory(category: String?) {
        _filterState.value = _filterState.value.copy(category = category)
    }

    fun setFilterPaymentMethod(method: PaymentMethod?) {
        _filterState.value = _filterState.value.copy(paymentMethod = method)
    }

    fun setSearchQuery(query: String) {
        _filterState.value = _filterState.value.copy(searchQuery = query.ifBlank { null })
    }

    /**
     * v1.10.0: #11 — filter by date range (inclusive on both ends).
     * An inverted range is swapped so "from" never lands after "to".
     * 0 / Long.MAX_VALUE = unbounded (the FilterState defaults).
     */
    fun setFilterDateRange(start: Long, end: Long) {
        val (s, e) = if (start <= end) start to end else end to start
        _filterState.value = _filterState.value.copy(startDate = s, endDate = e)
    }

    /** v1.10.0: #11 — remove the date filter (both bounds unbounded). */
    fun clearFilterDates() {
        _filterState.value = _filterState.value.copy(startDate = 0L, endDate = Long.MAX_VALUE)
    }

    fun clearFilters() {
        _filterState.value = FilterState()
    }

    fun addTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.insert(transaction)
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.update(transaction)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.delete(transaction)
        }
    }

    /**
     * v1.7.0: inline category creation from the add/edit transaction sheet.
     * Duplicates within the same type are ignored (case-insensitive); the new
     * category is appended at the end of its type section.
     */
    /** v1.9.0: iconKey = the user's picker choice (null = automatic by name). */
    fun addCategory(name: String, type: TransactionType, iconKey: String? = null) {
        viewModelScope.launch {
            val trimmed = name.trim()
            if (trimmed.isEmpty()) return@launch
            val existing = categoryRepository.getByTypeAsync(type)
            if (existing.any { it.name.equals(trimmed, ignoreCase = true) }) return@launch
            val maxOrder = existing.maxOfOrNull { it.sortOrder } ?: 0
            categoryRepository.insert(
                Category(
                    name = trimmed,
                    type = type,
                    emoji = "📦",
                    icon = iconKey,
                    isVisible = true,
                    sortOrder = maxOrder + 1
                )
            )
        }
    }

    fun getTransactionById(id: String): Transaction? {
        var result: Transaction? = null
        viewModelScope.launch {
            result = transactionRepository.getById(id)
        }
        return result
    }
}

data class FilterState(
    val type: TransactionType? = null,
    val category: String? = null,
    val paymentMethod: PaymentMethod? = null,
    val startDate: Long = 0,
    // No "capped at today" bound: transactions can carry a future date
    // (the user picks it in the add sheet) and must still be listed.
    val endDate: Long = Long.MAX_VALUE,
    val searchQuery: String? = null
)