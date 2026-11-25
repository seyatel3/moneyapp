package ru.moneyapp.ui.transactions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Money
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateTo
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.moneyapp.domain.model.*
import ru.moneyapp.ui.UiFlowViewModel
import ru.moneyapp.ui.theme.MoneyTypography
import ru.moneyapp.ui.theme.categoryIcon
import ru.moneyapp.ui.theme.categoryTint
import ru.moneyapp.ui.theme.expenseColor
import ru.moneyapp.ui.theme.expenseTextColor
import ru.moneyapp.ui.theme.incomeColor
import ru.moneyapp.ui.theme.incomeTextColor
import ru.moneyapp.ui.theme.savingsColor
import ru.moneyapp.ui.theme.savingsTextColor
import ru.moneyapp.ui.categories.CategoryIconPicker
import ru.moneyapp.ui.l10n.L10n
import ru.moneyapp.ui.l10n.label
import ru.moneyapp.ui.theme.typeIcon
import java.text.DecimalFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    onNavigateToSettings: () -> Unit,
    editTransactionId: String? = null,
    uiFlowViewModel: UiFlowViewModel,
    viewModel: TransactionsViewModel = hiltViewModel()
) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle(emptyList())
    val filterState by viewModel.filterState.collectAsStateWithLifecycle(FilterState())
    val incomeCats by viewModel.incomeCategories.collectAsStateWithLifecycle(emptyList())
    val expenseCats by viewModel.expenseCategories.collectAsStateWithLifecycle(emptyList())
    val savingsCats by viewModel.savingsCategories.collectAsStateWithLifecycle(emptyList())

    // The "Новая транзакция" form lives in a persistent scaffold bottom sheet.
    // M3 1.3.1 ModalBottomSheet caps the visible sheet at 50% of the window
    // (content taller than that gets its bottom cut off by the screen edge),
    // so the form was tiny with a long scroll. BottomSheetScaffold instead
    // expands the sheet from the top of the screen down to the top of the nav
    // bar — nearly the full screen. The imePadding on the root Box shrinks the
    // whole area when the keyboard opens, so the sheet bottom edge moves above
    // the keyboard and the field block re-fits.
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    // skipHiddenState=false: the sheet starts Hidden and the FAB expands it
    val sheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.Hidden,
        skipHiddenState = false
    )
    // v1.11.7: a swipe on the sheet body must NOT move the sheet — only the
    // drag handle on top may. The sheet's own nested-scroll connection would
    // otherwise collapse it when the scrollable field block is flung past its
    // end: this connection eats the vertical overscroll (and the fling left by
    // it) before it reaches that connection. Direct drags that start on the
    // body are consumed by the pointerInput on the sheetContent Box below.
    val sheetBodyOverscroll = remember(sheetState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
                if (
                    source == NestedScrollSource.UserInput &&
                    available.y < 0 &&
                    sheetState.currentValue != SheetValue.Expanded
                ) {
                    Offset(0f, available.y)
                } else {
                    Offset.Zero
                }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset =
                if (source == NestedScrollSource.UserInput) {
                    Offset(0f, available.y)
                } else {
                    Offset.Zero
                }

            override suspend fun onPreFling(available: Velocity): Velocity =
                if (
                    available.y < 0 &&
                    sheetState.currentValue != SheetValue.Expanded
                ) {
                    available
                } else {
                    Velocity.Zero
                }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity =
                if (available.y != 0f) {
                    available
                } else {
                    Velocity.Zero
                }
        }
    }
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)
    var sheetOpenCount by remember { mutableIntStateOf(0) }
    var showFilterDialog by remember { mutableStateOf(false) }
    // v1.10.0: #11 — date range filter dialog (От/До)
    var showDateFilterDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf<Transaction?>(null) }
    // Non-null when the sheet is open for editing an existing transaction
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }

    // The dashboard "Добавить" FAB requests the form via a one-shot
    // activity-scoped event (v1.11.8, see UiFlowViewModel). The flag is
    // consumed so a revisited entry never re-opens the sheet on its own.
    LaunchedEffect(Unit) {
        if (uiFlowViewModel.openAdd.value) {
            uiFlowViewModel.consumeOpenAdd()
            sheetOpenCount++
            // The scaffold is not laid out yet during the first composition;
            // expanding before the first frame leaves the sheet hidden.
            scope.launch {
                delay(300)
                sheetState.expand()
            }
        }
    }

    // M3 1.3.1: animateTo(Hidden) does not complete while the IME is shown
    // (the sheet stays open even though the transaction is saved). Dismiss the
    // keyboard first, let the layout settle, then hide the sheet.
    fun hideSheetAfterKeyboard() {
        focusManager.clearFocus()
        scope.launch {
            delay(300)
            sheetState.hide()
        }
    }

    val allCategories = remember(incomeCats, expenseCats, savingsCats) {
        incomeCats + expenseCats + savingsCats
    }

    // Group transactions by day, newest day first.
    val groupedTransactions: List<Map.Entry<LocalDate, List<Transaction>>> =
        transactions
            .groupBy {
                runCatching { LocalDate.ofEpochDay(it.date) }
                    // NOTE: NOT LocalDate.EPOCH — that field is Java 9+ and does not
                    // exist on Android's java.time (Java 8 set) for older devices
                    // (e.g. Android 12/API 31), causing NoSuchFieldError. ofEpochDay(0)
                    // is the identical value (1970-01-01) and is Java 8 / API 26+ safe.
                    .getOrDefault(LocalDate.ofEpochDay(0))
            }
            .entries
            .sortedByDescending { it.key }

    val formatter = remember { DecimalFormat("#,###") }
    val sumFormatter = remember { DecimalFormat("#,###") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetPeekHeight = 56.dp,
            sheetContent = {
                // v1.11.7: consumes drags that start on the sheet body so the
                // sheet only follows a drag on the drag handle above. Child
                // scrollables keep priority over this detector, so the field
                // block still scrolls normally.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                change.consume()
                            }
                        }
                        .nestedScroll(sheetBodyOverscroll)
                ) {
                    AddTransactionSheetContent(
                        incomeCategories = incomeCats,
                        expenseCategories = expenseCats,
                        savingsCategories = savingsCats,
                        initialType = filterState.type ?: TransactionType.EXPENSE,
                        openCount = sheetOpenCount,
                        editing = editingTransaction,
                        onDismiss = {
                            editingTransaction = null
                            hideSheetAfterKeyboard()
                        },
                        onAdd = { transaction ->
                            if (editingTransaction != null) {
                                viewModel.updateTransaction(transaction)
                            } else {
                                viewModel.addTransaction(transaction)
                            }
                            editingTransaction = null
                            hideSheetAfterKeyboard()
                        },
                        onAddCategory = { type, name, iconKey ->
                            viewModel.addCategory(name, type, iconKey)
                        }
                    )
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // v1.10.0: #12 — compact search button; expands into the
                    // field on tap. The query survives collapse, so the filter
                    // stays active while the field is hidden.
                    item {
                        SearchExpandableField(
                            query = searchQuery,
                            onQueryChange = {
                                searchQuery = it
                                viewModel.setSearchQuery(it)
                            },
                            onClearAndCollapse = {
                                searchQuery = ""
                                viewModel.setSearchQuery("")
                            }
                        )
                    }

                    // Filter chips (horizontally scrollable: on narrow screens the 4th chip
                    // used to get compressed and wrap its label one letter per line)
                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(end = 16.dp)
                        ) {
                            FilterChip(
                                selected = filterState.type == null,
                                onClick = { viewModel.setFilterType(null) },
                                label = { Text(L10n.s.txFilterAll) }
                            )
                            FilterChip(
                                selected = filterState.type == TransactionType.INCOME,
                                onClick = { viewModel.setFilterType(TransactionType.INCOME) },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = typeIcon(TransactionType.INCOME),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(TransactionType.INCOME.label())
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = incomeColor().copy(alpha = 0.2f)
                                )
                            )
                            FilterChip(
                                selected = filterState.type == TransactionType.EXPENSE,
                                onClick = { viewModel.setFilterType(TransactionType.EXPENSE) },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = typeIcon(TransactionType.EXPENSE),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(TransactionType.EXPENSE.label())
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = expenseColor().copy(alpha = 0.2f)
                                )
                            )
                            FilterChip(
                                selected = filterState.type == TransactionType.SAVINGS,
                                onClick = { viewModel.setFilterType(TransactionType.SAVINGS) },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = typeIcon(TransactionType.SAVINGS),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(TransactionType.SAVINGS.label())
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = savingsColor().copy(alpha = 0.2f)
                                )
                            )
                            // v1.11.6: spending from the piggy bank
                            FilterChip(
                                selected = filterState.type == TransactionType.SAVINGS_SPEND,
                                onClick = { viewModel.setFilterType(TransactionType.SAVINGS_SPEND) },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = typeIcon(TransactionType.SAVINGS_SPEND),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(TransactionType.SAVINGS_SPEND.label())
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = savingsColor().copy(alpha = 0.2f)
                                )
                            )
                            // v1.10.0: #11 — date range filter (От/До)
                            FilterChip(
                                selected = filterState.startDate != 0L || filterState.endDate != Long.MAX_VALUE,
                                onClick = { showDateFilterDialog = true },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.CalendarMonth,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(L10n.s.txDateFilter)
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }

                    if (transactions.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(
                                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Search,
                                            contentDescription = null,
                                            modifier = Modifier.padding(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(L10n.s.txEmptyTitle, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = L10n.s.txEmptyHint,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        // Grouped transactions: day header + swipeable rows
                        groupedTransactions.forEach { (day, dayTransactions) ->
                            item(key = "header-${day}") {
                                DayHeader(
                                    day = day,
                                    transactions = dayTransactions,
                                    formatter = sumFormatter
                                )
                            }

                            items(dayTransactions, key = { "tx-${it.id}" }) { transaction ->
                                SwipeToDeleteRow(
                                    onClick = {
                                        editingTransaction = transaction
                                        sheetOpenCount++
                                        scope.launch { sheetState.expand() }
                                    },
                                    onLongClick = { showDeleteConfirm = transaction },
                                    onDelete = { showDeleteConfirm = transaction }
                                ) {
                                    TransactionItem(
                                        transaction = transaction,
                                        formatter = formatter,
                                        // v1.9.0: pass the category's chosen icon key
                                        iconKey = allCategories.find { it.name == transaction.category }?.icon
                                    )
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }

                FloatingActionButton(
                    onClick = {
                        sheetOpenCount++
                        scope.launch { sheetState.expand() }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = L10n.s.add)
                }
            }
        }
    }

    // Delete confirmation
    showDeleteConfirm?.let { transaction ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text(L10n.s.txDeleteTitle) },
            text = { Text(transaction.description) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransaction(transaction)
                        showDeleteConfirm = null
                    }
                ) {
                    Text(L10n.s.delete, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text(L10n.s.cancel)
                }
            }
        )
    }

    // v1.10.0: #11 — date range filter (От/До)
    if (showDateFilterDialog) {
        DateRangeFilterDialog(
            initialStart = filterState.startDate,
            initialEnd = filterState.endDate,
            onDismiss = { showDateFilterDialog = false },
            onApply = { start, end -> viewModel.setFilterDateRange(start, end) },
            onClear = { viewModel.clearFilterDates() }
        )
    }
}

// v1.10.0: #12 — the search is a compact button that expands into a full
// OutlinedTextField on tap (with auto-focus). "X" clears the query and
// collapses; collapsing keeps the query so the filter stays applied.
@Composable
private fun SearchExpandableField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearAndCollapse: () -> Unit
) {
    val s = L10n.s
    var expanded by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(expanded) {
        if (expanded) focusRequester.requestFocus()
    }

    if (!expanded) {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(s.txSearch, style = MoneyTypography.labelMedium)
                // Active query stays visible on the collapsed button
                if (query.isNotEmpty()) {
                    Text(
                        text = query,
                        style = MoneyTypography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        modifier = Modifier.padding(start = 4.dp),
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    } else {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text(s.txSearchPlaceholder) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                IconButton(onClick = {
                    onClearAndCollapse()
                    expanded = false
                }) {
                    Icon(Icons.Rounded.Close, contentDescription = L10n.s.txClear)
                }
            }
        )
    }
}

private enum class DatePickTarget { START, END }

/**
 * v1.10.0: #11 — filter by date range. Each bound is picked with a standard
 * M3 date picker; an unset bound stays unbounded ("—").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangeFilterDialog(
    initialStart: Long,
    initialEnd: Long,
    onDismiss: () -> Unit,
    onApply: (Long, Long) -> Unit,
    onClear: () -> Unit
) {
    val s = L10n.s
    val dateFmt = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }
    // 0 / Long.MAX_VALUE (unbounded) -> null ("not set") inside the dialog
    var start by remember { mutableStateOf<Long?>(if (initialStart > 0L) initialStart else null) }
    var end by remember { mutableStateOf<Long?>(if (initialEnd < Long.MAX_VALUE) initialEnd else null) }
    var target by remember { mutableStateOf<DatePickTarget?>(null) }
    val today = LocalDate.now()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.txDateFilter) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(s.txDateFrom, style = MoneyTypography.bodyMedium, modifier = Modifier.weight(1f))
                    OutlinedButton(
                        onClick = { target = DatePickTarget.START },
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text(
                            text = start?.let { d -> LocalDate.ofEpochDay(d).format(dateFmt) } ?: "—",
                            maxLines = 1
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(s.txDateTo, style = MoneyTypography.bodyMedium, modifier = Modifier.weight(1f))
                    OutlinedButton(
                        onClick = { target = DatePickTarget.END },
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text(
                            text = end?.let { d -> LocalDate.ofEpochDay(d).format(dateFmt) } ?: "—",
                            maxLines = 1
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApply(start ?: 0L, end ?: Long.MAX_VALUE)
                    onDismiss()
                }
            ) {
                Text(s.ok)
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    enabled = start != null || end != null,
                    onClick = {
                        onClear()
                        onDismiss()
                    }
                ) {
                    Text(s.txDateClear)
                }
                TextButton(onClick = onDismiss) {
                    Text(s.cancel)
                }
            }
        }
    )

    val currentTarget = target
    if (currentTarget != null) {
        val initialDays = when (currentTarget) {
            DatePickTarget.START -> start ?: today.toEpochDay()
            DatePickTarget.END -> end ?: today.toEpochDay()
        }
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.ofEpochDay(initialDays)
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { target = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val days = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
                            if (currentTarget == DatePickTarget.START) start = days else end = days
                        }
                        target = null
                    }
                ) { Text(s.ok) }
            },
            dismissButton = {
                TextButton(onClick = { target = null }) { Text(s.cancel) }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun DayHeader(
    day: LocalDate,
    transactions: List<Transaction>,
    formatter: DecimalFormat
) {
    val today = LocalDate.now()
    val title = when (day) {
        today -> L10n.s.today
        today.minusDays(1) -> L10n.s.yesterday
        today.minusDays(2) -> L10n.s.dayBeforeYesterday
        else -> {
            // v1.9.0: localized month name (follows the in-app language)
            val monthName = remember(day, L10n.lang) {
                L10n.monthFull(day.monthValue)
            }
            if (day.year == today.year) "${day.dayOfMonth} $monthName"
            else "${day.dayOfMonth} $monthName ${day.year}"
        }
    }
    // Net for the day: income adds, expense and savings subtract.
    val dayNet = transactions.sumOf { t ->
        if (t.type == TransactionType.INCOME) t.amountKopecks else -t.amountKopecks
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MoneyTypography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Text(
            text = "${formatter.format(dayNet / 100.0)} ₽",
            style = MoneyTypography.amountSmall
        )
    }
}

// The card is intentionally NOT clickable: the row's tap, long-press and
// swipe gestures are handled by a single detector in SwipeToDeleteRow, which
// wraps this composable. A separate clickable node would win the touch slop
// and starve the swipe gesture.
@Composable
private fun TransactionItem(
    transaction: Transaction,
    formatter: DecimalFormat,
    iconKey: String? = null   // v1.9.0
) {
    val typeColor = when (transaction.type) {
        TransactionType.INCOME -> incomeTextColor()
        TransactionType.EXPENSE -> expenseTextColor()
        TransactionType.SAVINGS -> savingsTextColor()
        // v1.11.6: spending from the piggy — savings color, sign "−"
        TransactionType.SAVINGS_SPEND -> savingsTextColor()
    }
    val tint = categoryTint(transaction.category)
    val sign = if (transaction.type == TransactionType.INCOME) "+" else "−"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tinted category icon (one vector style everywhere)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon(transaction.category, iconKey),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = tint
                )
            }
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Empty description: the category becomes the main line
                // (no duplicate second line).
                val mainTitle = transaction.description.ifBlank { transaction.category }
                Text(
                    text = mainTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (transaction.description.isNotBlank()) {
                    Text(
                        text = transaction.category,
                        style = MoneyTypography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "$sign${formatter.format(transaction.amountRubles)} ₽",
                style = MoneyTypography.amountMedium,
                color = typeColor,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

// Swipe a row left to reveal the delete action; releasing past the threshold
// asks for confirmation (same dialog as the long-press path).
//
// One pointerInput owns ALL row gestures (tap = edit, long-press = delete
// confirm, horizontal drag = swipe). A separate clickable node anywhere in
// this subtree consumes the slop-crossing move event and the swipe would
// never be recognized.
//
// The tap is gated on the pointer never leaving the touch slop: a fling or
// a vertical scroll sets `vertical` as soon as the move exceeds the slop,
// so releasing after a scroll never opens the edit sheet. No press-duration
// delay is needed — a true tap already implies the finger stayed still.
@Composable
private fun SwipeToDeleteRow(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val viewConfiguration = LocalViewConfiguration.current
    // ViewConfiguration.touchSlop is already in pixels
    val touchSlop = viewConfiguration.touchSlop.toFloat()
    val maxOffsetPx = with(density) { 120.dp.toPx() }
    val thresholdPx = with(density) { 80.dp.toPx() }
    val offset = remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(false)
                    var horizontal = false
                    var vertical = false
                    var longPressFired = false
                    var lastX = down.position.x
                    // Long-press: the pointer stays within the slop for 500 ms.
                    val longPressJob = scope.launch {
                        delay(500L)
                        if (!horizontal && !vertical) {
                            longPressFired = true
                            onLongClick()
                        }
                    }
                    var pointerReleased = false
                    var finalOffset = 0f
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                                ?: break
                            if (!horizontal && !vertical) {
                                val dx = kotlin.math.abs(change.position.x - down.position.x)
                                val dy = kotlin.math.abs(change.position.y - down.position.y)
                                if (dx > touchSlop && dx >= dy) {
                                    horizontal = true
                                } else if (dy > touchSlop) {
                                    // Vertical movement: leave the event
                                    // unconsumed so the LazyColumn scrolls.
                                    vertical = true
                                }
                            }
                            if (horizontal) {
                                // The row follows the finger: a leftward drag
                                // (negative delta) yields a negative offset.
                                offset.value =
                                    (offset.value + (change.position.x - lastX))
                                        .coerceIn(-maxOffsetPx, 0f)
                                change.consume()
                            }
                            lastX = change.position.x
                            if (!change.pressed) {
                                pointerReleased = true
                                break
                            }
                        }
                        finalOffset = offset.value
                    } finally {
                        longPressJob.cancel()
                        if (horizontal) {
                            scope.launch {
                                Animatable(offset.value).animateTo(0f) { offset.value = value }
                            }
                        }
                    }
                    if (!pointerReleased) return@awaitEachGesture
                    if (horizontal) {
                        if (finalOffset <= -thresholdPx) onDelete()
                    } else if (!vertical && !longPressFired) {
                        // Tap: the pointer never moved beyond the touch slop.
                        // A scroll/fling ends on a release, but `vertical` is
                        // already true, so the edit sheet stays closed.
                        onClick()
                    }
                }
            }
    ) {
        // Delete surface behind the row
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.9f)),
            contentAlignment = Alignment.CenterEnd
        ) {
            Icon(
                imageVector = Icons.Rounded.Delete,
                contentDescription = L10n.s.delete,
                tint = Color.White,
                modifier = Modifier.padding(end = 16.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offset.value.roundToInt(), 0) }
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun AddTransactionSheetContent(
    incomeCategories: List<Category>,
    expenseCategories: List<Category>,
    savingsCategories: List<Category>,
    initialType: TransactionType,
    openCount: Int,
    editing: Transaction? = null,
    onDismiss: () -> Unit,
    onAdd: (Transaction) -> Unit,
    onAddCategory: (TransactionType, String, String?) -> Unit
) {
    // When editing, start from the existing transaction's values.
    var selectedType by remember { mutableStateOf(editing?.type ?: initialType) }
    var selectedCategory by remember {
        mutableStateOf(editing?.category
            ?: categoryListForType(incomeCategories, expenseCategories, savingsCategories, initialType).firstOrNull()?.name ?: "")
    }
    var description by remember { mutableStateOf(editing?.description ?: "") }
    var amount by remember {
        mutableStateOf(editing?.amountKopecks?.let { kopecks ->
            val rubles = kopecks / 100.0
            if (rubles == rubles.toLong().toDouble()) rubles.toLong().toString() else rubles.toString()
        } ?: "")
    }
    var paymentMethod by remember { mutableStateOf(editing?.paymentMethod ?: PaymentMethod.CARD) }
    var note by remember { mutableStateOf(editing?.note ?: "") }
    // Date: defaults to today, user can change it via the picker.
    // runCatching: a corrupted date value must not crash the edit sheet.
    var selectedDate by remember {
        mutableStateOf(
            editing?.date?.let { runCatching { LocalDate.ofEpochDay(it) }.getOrNull() }
                ?: LocalDate.now()
        )
    }
    var showDatePicker by remember { mutableStateOf(false) }
    // v1.7.0: inline "new category" dialog
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    // The sheet content is composed persistently (it survives
    // hidden/peek/expanded), so on every open the form is either pre-filled
    // from the transaction being edited or reset to fresh defaults.
    LaunchedEffect(openCount) {
        if (openCount > 0) {
            val edit = editing
            if (edit != null) {
                selectedType = edit.type
                selectedCategory = edit.category
                selectedDate = runCatching { LocalDate.ofEpochDay(edit.date) }
                    .getOrDefault(LocalDate.now())
                description = edit.description
                val rubles = edit.amountKopecks / 100.0
                amount = if (rubles == rubles.toLong().toDouble()) rubles.toLong().toString() else rubles.toString()
                paymentMethod = edit.paymentMethod
                note = edit.note ?: ""
            } else {
                selectedType = initialType
                selectedCategory = categoryListForType(
                    incomeCategories, expenseCategories, savingsCategories, initialType
                ).firstOrNull()?.name ?: ""
                selectedDate = LocalDate.now()
                description = ""
                amount = ""
                paymentMethod = PaymentMethod.CARD
                note = ""
            }
        }
    }

    // Fresh-DB case: the first sheet open can happen before the category
    // flows have emitted, leaving selectedCategory = "" (Save stays disabled
    // until the next open). If nothing is selected yet and the lists arrived,
    // pick the first category of the current type.
    LaunchedEffect(incomeCategories, expenseCategories, savingsCategories, selectedType) {
        if (selectedCategory.isEmpty()) {
            val list = categoryListForType(incomeCategories, expenseCategories, savingsCategories, selectedType)
            if (list.isNotEmpty()) selectedCategory = list.first().name
        }
    }

    val currentCategories = categoryListForType(
        incomeCategories, expenseCategories, savingsCategories, selectedType
    )

    // Inside a BottomSheetScaffold the sheet content is measured against the
    // available screen area (below the drag handle, above the nav bar), and
    // that area shrinks when the IME opens. So the sheet is exactly the
    // available height: pinned title + amount on top, a scrollable field block
    // in the middle, and a pinned "Сохранить" that never overflows (also above
    // the keyboard).
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val sheetArea = if (maxHeight.value == Float.POSITIVE_INFINITY ||
            maxHeight.value < 1f
        ) {
            Dp(LocalConfiguration.current.screenHeightDp.toFloat())
        } else {
            maxHeight
        }
        // Pinned parts (dp): top padding 16 + title row 40 + spacer 8 + amount
        // caption 20 + amount field 56 + spacer 12 + save button 52 + bottom
        // padding 16 = 220, plus 16dp safety margin.
        val fieldsMax = (sheetArea - 236.dp).coerceIn(140.dp, 640.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editing != null) L10n.s.txEditTitle else L10n.s.txNewTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = L10n.s.close)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Large amount input, pinned on top, numeric keyboard
            Text(
                text = L10n.s.txAmount,
                style = MoneyTypography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() } },
                placeholder = { Text("0", style = MoneyTypography.amountInput) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MoneyTypography.amountInput,
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                )
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Scrollable field block: everything between the pinned amount
            // and the pinned save button.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = fieldsMax)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Type: Доход / Расход / Отложить / Из накоплений (v1.11.6).
                // 2×2 grid instead of one 4-wide row: the M3 label slot is
                // measured at ideal width, so «Из накоплений» would overflow
                // into its neighbour in a quarter-width segment.
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        listOf(
                            TransactionType.INCOME,
                            TransactionType.EXPENSE
                        ).forEachIndexed { index, type ->
                            SegmentedButton(
                                selected = selectedType == type,
                                onClick = {
                                    selectedType = type
                                    selectedCategory = categoryListForType(
                                        incomeCategories, expenseCategories, savingsCategories, type
                                    ).firstOrNull()?.name ?: ""
                                },
                                shape = SegmentedButtonDefaults.itemShape(index, 2),
                                label = { Text(type.label(), style = MoneyTypography.labelMedium) },
                                icon = {
                                    Icon(
                                        imageVector = typeIcon(type),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        listOf(
                            TransactionType.SAVINGS,
                            TransactionType.SAVINGS_SPEND
                        ).forEachIndexed { index, type ->
                            SegmentedButton(
                                selected = selectedType == type,
                                onClick = {
                                    selectedType = type
                                    selectedCategory = categoryListForType(
                                        incomeCategories, expenseCategories, savingsCategories, type
                                    ).firstOrNull()?.name ?: ""
                                },
                                shape = SegmentedButtonDefaults.itemShape(index, 2),
                                label = { Text(type.label(), style = MoneyTypography.labelMedium) },
                                icon = {
                                    Icon(
                                        imageVector = typeIcon(type),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }

                // Categories as icon chips; v1.7.0: the inline «+» chip lets
                // the user create a category without leaving the sheet.
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = L10n.s.txCategory,
                        style = MaterialTheme.typography.labelLarge
                    )
                    if (currentCategories.isEmpty()) {
                        Text(
                            text = L10n.s.txNoCategoriesHint,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        currentCategories.forEach { cat ->
                            CategoryChip(
                                name = cat.name,
                                iconKey = cat.icon,   // v1.9.0
                                selected = selectedCategory == cat.name,
                                onClick = { selectedCategory = cat.name }
                            )
                        }
                        QuickAddCategoryChip(onClick = { showAddCategoryDialog = true })
                    }
                }

                // Payment: Безнал / Наличные
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = paymentMethod == PaymentMethod.CARD,
                        onClick = { paymentMethod = PaymentMethod.CARD },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                        label = { Text(PaymentMethod.CARD.label(), style = MoneyTypography.labelMedium) },
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.CreditCard,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                    SegmentedButton(
                        selected = paymentMethod == PaymentMethod.CASH,
                        onClick = { paymentMethod = PaymentMethod.CASH },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                        label = { Text(PaymentMethod.CASH.label(), style = MoneyTypography.labelMedium) },
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.Money,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }

                // Date: defaults to today, tap to change
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(selectedDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
                }

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(L10n.s.txDescription) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Note
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(L10n.s.txNote) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Save button (pinned: always visible above the nav bar).
            // In edit mode the same id is kept, so the repository updates the
            // existing row instead of inserting a new one.
            Button(
                onClick = {
                    if (amount.isNotBlank() && selectedCategory.isNotEmpty()) {
                        val edit = editing
                        // Empty description and note are allowed (category is
                        // required — enforced by the enabled flag below).
                        val transaction = if (edit != null) {
                            edit.copy(
                                date = selectedDate.toEpochDay(),
                                type = selectedType,
                                category = selectedCategory,
                                description = description,
                                amountKopecks = ((amount.toDoubleOrNull() ?: 0.0) * 100).toLong(),
                                paymentMethod = paymentMethod,
                                note = note.ifBlank { null }
                            )
                        } else {
                            Transaction(
                                date = selectedDate.toEpochDay(),
                                type = selectedType,
                                category = selectedCategory,
                                description = description,
                                amountKopecks = ((amount.toDoubleOrNull() ?: 0.0) * 100).toLong(),
                                paymentMethod = paymentMethod,
                                note = note.ifBlank { null }
                            )
                        }
                        onAdd(transaction)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = amount.isNotBlank() && selectedCategory.isNotEmpty()
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (editing != null) L10n.s.txSaveChanges else L10n.s.save,
                    style = MoneyTypography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC).toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) { Text(L10n.s.ok) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(L10n.s.cancel) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // v1.7.0: inline "new category" dialog (type = the sheet's current type)
    if (showAddCategoryDialog) {
        QuickAddCategoryDialog(
            type = selectedType,
            existingNames = currentCategories.map { it.name },
            onDismiss = { showAddCategoryDialog = false },
            onConfirm = { name, iconKey ->
                onAddCategory(selectedType, name, iconKey)
                // Select the new category right away: a transaction stores the
                // category as a plain name, so "Сохранить" can be enabled even
                // before the category flows re-emit.
                selectedCategory = name
                showAddCategoryDialog = false
            }
        )
    }
}

@Composable
private fun CategoryChip(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
    iconKey: String? = null   // v1.9.0
) {
    val tint = categoryTint(name)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) tint.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) tint else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = categoryIcon(name, iconKey),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = tint
            )
            Text(
                text = name,
                style = MoneyTypography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// v1.7.0: the «+» chip at the end of the category row opens the inline
// "new category" dialog.
@Composable
private fun QuickAddCategoryChip(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = L10n.s.add,
                style = MoneyTypography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

// v1.7.0: minimal "new category" dialog for the add/edit transaction sheet.
// The type is fixed by the sheet's segmented control, so only the name is
// asked. Duplicates within the type are rejected (case-insensitive).
// v1.9.0: the icon is chosen in the same dialog (compact horizontal row).
@Composable
private fun QuickAddCategoryDialog(
    type: TransactionType,
    existingNames: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit
) {
    val s = L10n.s
    var name by remember { mutableStateOf("") }
    var iconKey by remember { mutableStateOf<String?>(null) }
    val duplicate = existingNames.any { it.equals(name.trim(), ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.catNewTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(s.catName) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Text(
                    text = s.catWillBeAddedTo(type.label()),
                    style = MoneyTypography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                CategoryIconPicker(
                    selected = iconKey,
                    onSelect = { iconKey = it },
                    compact = true
                )
                if (duplicate) {
                    Text(
                        text = s.catDuplicate,
                        style = MoneyTypography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim(), iconKey) },
                enabled = name.isNotBlank() && !duplicate
            ) {
                Text(s.add, style = MoneyTypography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(s.cancel) }
        }
    )
}

private fun categoryListForType(
    income: List<Category>,
    expense: List<Category>,
    savings: List<Category>,
    type: TransactionType
): List<Category> = when (type) {
    TransactionType.INCOME -> income
    TransactionType.EXPENSE -> expense
    TransactionType.SAVINGS -> savings
    // v1.11.6: spending from savings uses expense categories
    TransactionType.SAVINGS_SPEND -> expense
}
