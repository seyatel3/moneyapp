package ru.moneyapp.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * One-shot cross-screen UI events, scoped to the activity.
 *
 * v1.11.8: the dashboard «Добавить» FAB used to navigate to
 * "transactions?add=true". That left a back-stack entry carrying a
 * non-default argument on the stack, and the bottom tab click
 * (`popUpTo(start) { saveState = true }` + `restoreState = true`) then
 * silently re-added that very entry on top of the tapped tab — after the FAB
 * flow the «Дашборд» tab did nothing, so there was no way back to the
 * dashboard. The sheet is now requested here instead of via nav args.
 */
@HiltViewModel
class UiFlowViewModel @Inject constructor() : ViewModel() {

    private val _openAdd = MutableStateFlow(false)

    /** Set by the dashboard FAB, consumed once by TransactionsScreen. */
    val openAdd: StateFlow<Boolean> = _openAdd

    fun requestOpenAdd() {
        _openAdd.value = true
    }

    fun consumeOpenAdd() {
        _openAdd.value = false
    }
}
