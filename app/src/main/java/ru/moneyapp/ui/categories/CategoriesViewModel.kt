package ru.moneyapp.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.moneyapp.data.repository.CategoryRepository
import ru.moneyapp.domain.model.Category
import ru.moneyapp.domain.model.TransactionType
import javax.inject.Inject

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val repository: CategoryRepository
) : ViewModel() {

    val categories: Flow<List<Category>> = repository.getAll()

    // v1.10.0: a fast drag can fire several reorder passes; serialize them so
    // two concurrent moveInType calls can't interleave and scramble sortOrder.
    private val reorderMutex = Mutex()

    /** v1.9.0: iconKey = the user's picker choice (null = automatic by name). */
    fun add(name: String, iconKey: String?, type: TransactionType) {
        viewModelScope.launch {
            val maxOrder = repository.getAllAsync()
                .filter { it.type == type }
                .maxOfOrNull { it.sortOrder } ?: 0
            repository.insert(
                Category(
                    name = name,
                    type = type,
                    emoji = "📦",
                    icon = iconKey,
                    isVisible = true,
                    sortOrder = maxOrder + 1
                )
            )
        }
    }

    // v1.10.0: every op is type-scoped (composite PK name+type).
    fun toggleVisible(type: TransactionType, name: String, isVisible: Boolean) {
        viewModelScope.launch { repository.setVisibility(type, name, isVisible) }
    }

    fun delete(type: TransactionType, name: String) {
        viewModelScope.launch { repository.delete(type, name) }
    }

    /** v1.10.0: rename / retype a category; propagates to linked rows. */
    fun edit(old: Category, new: Category) {
        viewModelScope.launch { repository.edit(old, new) }
    }

    fun moveInType(type: TransactionType, from: Int, to: Int) {
        viewModelScope.launch {
            reorderMutex.withLock {
                val section = repository.getAllAsync()
                    .filter { it.type == type }
                    .sortedBy { it.sortOrder }
                if (from !in section.indices || to !in section.indices) return@withLock
                val reordered = section.toMutableList().apply { add(to, removeAt(from)) }
                reordered.forEachIndexed { index, category ->
                    repository.setSortOrder(type, category.name, index + 1)
                }
            }
        }
    }
}
