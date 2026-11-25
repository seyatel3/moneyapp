package ru.moneyapp.ui.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.moneyapp.domain.model.*
import ru.moneyapp.ui.l10n.L10n
import ru.moneyapp.ui.l10n.label
import ru.moneyapp.ui.theme.MoneyTypography
import ru.moneyapp.ui.theme.categoryIcon
import ru.moneyapp.ui.theme.categoryTint
import ru.moneyapp.ui.theme.expenseTextColor
import ru.moneyapp.ui.theme.incomeTextColor
import ru.moneyapp.ui.theme.savingsTextColor
import ru.moneyapp.ui.theme.warningTextColor
import java.text.DecimalFormat

@Composable
fun BudgetScreen(
    viewModel: BudgetViewModel = hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }

    val budgetStatus by viewModel.budgetStatus.collectAsStateWithLifecycle(emptyList())
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val selectedYear by viewModel.selectedYear.collectAsStateWithLifecycle()
    val capState by viewModel.capState.collectAsStateWithLifecycle(BudgetCapState.Disabled)
    // v1.11.5: categories for the plan dialogs (visible ones, by type)
    val categories by viewModel.categories.collectAsStateWithLifecycle(emptyList())

    // v1.11.5: plan dialogs — the mini-app's add/edit plan flow, ported here.
    // null = closed; non-null = editing that effective plan's card.
    var planDialog by remember { mutableStateOf<BudgetCategoryStatus?>(null) }
    var showAddPlan by remember { mutableStateOf(false) }

    // One-shot UI events → snackbar (same pattern as SettingsScreen)
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            val message = when (event) {
                is BudgetViewModel.BudgetEvent.Success -> event.message
                is BudgetViewModel.BudgetEvent.Error -> event.message
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    // v1.9.0: localized month name (follows the in-app language)
    val monthName = L10n.monthFull(selectedMonth)

    val formatter = remember { DecimalFormat("#,###") }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Month selector: chevrons + month, year secondary
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.previousMonth() }) {
                        Icon(Icons.Rounded.ChevronLeft, contentDescription = L10n.s.prevMonth)
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = monthName,
                            style = MoneyTypography.titleLarge
                        )
                        Text(
                            text = selectedYear.toString(),
                            style = MoneyTypography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { viewModel.nextMonth() }) {
                        Icon(Icons.Rounded.ChevronRight, contentDescription = L10n.s.nextMonth)
                    }
                }
            }

            // v1.6.0: total monthly cap card; v1.7.0: an income-linked cap with no
            // income yet shows the "awaiting income" card instead of vanishing.
            val capReady = capState as? BudgetCapState.Ready
            if (capReady != null) {
                item {
                    CapCard(cap = capReady.info, formatter = formatter)
                }
            }
            val capAwaiting = capState as? BudgetCapState.AwaitingIncome
            if (capAwaiting != null) {
                item {
                    AwaitingIncomeCard(
                        pct = capAwaiting.pct,
                        spentKopecks = capAwaiting.spentKopecks,
                        formatter = formatter
                    )
                }
            }
            // v1.10.0: no income this month — the cap is based on the median of the
            // year's monthly incomes, so the bar stays meaningful instead of waiting.
            val capMedian = capState as? BudgetCapState.MedianBased
            if (capMedian != null) {
                item {
                    MedianCapCard(
                        cap = capMedian.info,
                        pct = capMedian.pct,
                        medianKopecks = capMedian.medianIncomeKopecks,
                        formatter = formatter
                    )
                }
            }

            // v1.11.5: add an explicit plan for the selected month — the same
            // dialog edits the cards below (parity with the mini-app).
            item {
                FilledTonalButton(
                    onClick = { showAddPlan = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(L10n.s.budAddPlan, style = MoneyTypography.labelLarge)
                }
            }

            // "Not configured" only when nothing is configured at all: no plans,
            // no cap, no income link. A linked cap with zero income so far is
            // AwaitingIncome, not Disabled.
            if (budgetStatus.isEmpty() && capState is BudgetCapState.Disabled) {
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
                                    imageVector = Icons.Rounded.ReceiptLong,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(L10n.s.budNotConfigured, style = MoneyTypography.titleMedium)
                            Text(
                                L10n.s.budNotConfiguredHint,
                                style = MoneyTypography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // Budget items grouped by type
                val incomePlans = budgetStatus.filter { it.type == BudgetType.INCOME }
                val expensePlans = budgetStatus.filter { it.type == BudgetType.EXPENSE }
                val savingsPlans = budgetStatus.filter { it.type == BudgetType.SAVINGS }
                val summaryPlans = budgetStatus.filter { it.type == BudgetType.SUMMARY }

                if (incomePlans.isNotEmpty()) {
                    item {
                        BudgetSectionHeader(
                            icon = Icons.Rounded.TrendingUp,
                            label = L10n.s.txTypeIncomePlural,
                            color = incomeTextColor()
                        )
                    }
                    items(incomePlans) { status ->
                        BudgetCategoryCard(
                            status = status,
                            formatter = formatter,
                            onCard = { planDialog = status }
                        )
                    }
                }

                if (expensePlans.isNotEmpty()) {
                    item {
                        BudgetSectionHeader(
                            icon = Icons.Rounded.TrendingDown,
                            label = L10n.s.txTypeExpensePlural,
                            color = expenseTextColor()
                        )
                    }
                    items(expensePlans) { status ->
                        BudgetCategoryCard(
                            status = status,
                            formatter = formatter,
                            onCard = { planDialog = status }
                        )
                    }
                }

                if (savingsPlans.isNotEmpty()) {
                    item {
                        BudgetSectionHeader(
                            icon = Icons.Rounded.Savings,
                            label = L10n.s.txTypeSavingsPlural,
                            color = savingsTextColor()
                        )
                    }
                    items(savingsPlans) { status ->
                        BudgetCategoryCard(
                            status = status,
                            formatter = formatter,
                            onCard = { planDialog = status }
                        )
                    }
                }

                // Summary
                if (summaryPlans.isNotEmpty()) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                    items(summaryPlans) { status ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = MaterialTheme.shapes.large,
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = status.category,
                                    style = MoneyTypography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .weight(1f, fill = false)
                                        .padding(end = 8.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${formatter.format(status.actualRubles)} ₽",
                                    style = MoneyTypography.amountMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    // v1.11.5: plan dialogs (parity with the mini-app's plan dialog)
    if (showAddPlan) {
        PlanDialog(
            categories = categories,
            status = null,
            onDismiss = { showAddPlan = false },
            onSave = { cat, type, kopecks ->
                viewModel.updateBudgetPlan(cat, type, kopecks)
                showAddPlan = false
            },
            onDelete = {}
        )
    }
    planDialog?.let { status ->
        PlanDialog(
            categories = categories,
            status = status,
            onDismiss = { planDialog = null },
            onSave = { cat, type, kopecks ->
                viewModel.updateBudgetPlan(cat, type, kopecks)
                planDialog = null
            },
            onDelete = {
                viewModel.deleteBudgetPlan(status)
                planDialog = null
            }
        )
    }
}

// v1.11.5: add/edit an explicit monthly plan. Mirrors the mini-app dialog:
// type chips, category chips of that type (visible ones only), numeric ₽
// amount, a hint for template / previous-month rows, and "delete" only for
// an explicit plan (template / prev rows have nothing to delete — saving
// them just writes a monthly override).
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlanDialog(
    categories: List<Category>,
    // null = new plan; non-null = the effective plan being edited
    status: BudgetCategoryStatus?,
    onDismiss: () -> Unit,
    onSave: (category: String, type: BudgetType, amountKopecks: Long) -> Unit,
    onDelete: () -> Unit
) {
    val s = L10n.s
    var type by remember { mutableStateOf(status?.type ?: BudgetType.EXPENSE) }
    var category by remember { mutableStateOf(status?.category.orEmpty()) }
    var amountText by remember {
        mutableStateOf(if (status != null) (status.plannedKopecks / 100).toString() else "")
    }
    val amountKopecks = (amountText.filter { it.isDigit() }.toLongOrNull() ?: 0L) * 100
    val txType = TransactionType.valueOf(type.name)
    val canSave = category.isNotEmpty() && amountKopecks > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (status == null) s.budNewPlan else s.budEditPlan) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        BudgetType.INCOME,
                        BudgetType.EXPENSE,
                        BudgetType.SAVINGS
                    ).forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = {
                                type = t
                                category = ""
                            },
                            label = { Text(t.label(), style = MoneyTypography.labelMedium) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Text(s.budCategory, style = MoneyTypography.labelMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories
                        .filter { it.isVisible && it.type == txType }
                        .forEach { c ->
                            FilterChip(
                                selected = category == c.name,
                                onClick = { category = c.name },
                                label = { Text(c.name, style = MoneyTypography.labelMedium) }
                            )
                        }
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() } },
                    label = { Text(s.budPlanAmount) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                // The row's origin decides the hint (and whether delete exists)
                when (status?.source) {
                    "template" -> Text(
                        s.budPlanFromTemplate,
                        style = MoneyTypography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    "prev" -> Text(
                        s.budPlanFromPrev,
                        style = MoneyTypography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    else -> Unit
                }
            }
        },
        confirmButton = {
            if (status != null && status.source == "explicit") {
                TextButton(onClick = onDelete) {
                    Text(s.delete, color = MaterialTheme.colorScheme.error)
                }
            }
            Button(
                onClick = { onSave(category, type, amountKopecks) },
                enabled = canSave
            ) {
                Text(s.save, style = MoneyTypography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(s.cancel)
            }
        }
    )
}

// v1.6.0: total monthly cap card — spent/cap, progress, remaining/overrun
@Composable
private fun CapCard(
    cap: BudgetCapInfo,
    formatter: DecimalFormat
) {
    val statusColor = when (cap.status) {
        BudgetCapStatus.OK -> incomeTextColor()
        BudgetCapStatus.CLOSE -> warningTextColor()
        BudgetCapStatus.OVER -> expenseTextColor()
    }
    val statusLabel = when (cap.status) {
        BudgetCapStatus.OK -> L10n.s.budOnTrack
        BudgetCapStatus.CLOSE -> L10n.s.budCloseToLimit
        BudgetCapStatus.OVER -> L10n.s.budLimitExceeded
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = L10n.s.dashMonthlyLimit,
                    style = MoneyTypography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = statusLabel,
                    style = MoneyTypography.labelMedium,
                    color = statusColor,
                    maxLines = 1
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${formatter.format(cap.spentRubles)} / ${formatter.format(cap.capRubles)} ₽",
                    style = MoneyTypography.amountMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = "${String.format("%.0f", cap.percentage * 100)}%",
                    style = MoneyTypography.labelLarge,
                    color = statusColor,
                    maxLines = 1,
                    softWrap = false
                )
            }

            LinearProgressIndicator(
                progress = { (cap.percentage.coerceIn(0.0, 1.0)).toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = statusColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                gapSize = 0.dp
            )

            Text(
                text = if (cap.remainingKopecks >= 0) {
                    L10n.s.budLeft(formatter.format(cap.remainingKopecks / 100.0))
                } else {
                    L10n.s.budOver(formatter.format((-cap.remainingKopecks) / 100.0))
                },
                style = MoneyTypography.labelSmall,
                color = if (cap.remainingKopecks >= 0) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    expenseTextColor()
                }
            )
        }
    }
}

// v1.7.0: the cap is linked to income (N%), but the month has no income
// recorded yet — the limit cannot be computed, yet the budget IS configured.
@Composable
private fun AwaitingIncomeCard(
    pct: Int,
    spentKopecks: Long,
    formatter: DecimalFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = L10n.s.dashMonthlyLimit,
                    style = MoneyTypography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = L10n.s.budAwaitingIncome,
                    style = MoneyTypography.labelMedium,
                    color = warningTextColor(),
                    maxLines = 1
                )
            }

            Text(
                text = L10n.s.budAwaitingIncomeText(pct),
                style = MoneyTypography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = L10n.s.budSpentThisMonth(formatter.format(spentKopecks / 100.0)),
                style = MoneyTypography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// v1.10.0: income-linked cap with no income this month — the limit is computed
// from the median of the year's monthly incomes (CapCard layout + the note).
@Composable
private fun MedianCapCard(
    cap: BudgetCapInfo,
    pct: Int,
    medianKopecks: Long,
    formatter: DecimalFormat
) {
    val statusColor = when (cap.status) {
        BudgetCapStatus.OK -> incomeTextColor()
        BudgetCapStatus.CLOSE -> warningTextColor()
        BudgetCapStatus.OVER -> expenseTextColor()
    }
    val statusLabel = when (cap.status) {
        BudgetCapStatus.OK -> L10n.s.budOnTrack
        BudgetCapStatus.CLOSE -> L10n.s.budCloseToLimit
        BudgetCapStatus.OVER -> L10n.s.budLimitExceeded
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = L10n.s.dashMonthlyLimit,
                    style = MoneyTypography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = statusLabel,
                    style = MoneyTypography.labelMedium,
                    color = statusColor,
                    maxLines = 1
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${formatter.format(cap.spentRubles)} / ${formatter.format(cap.capRubles)} ₽",
                    style = MoneyTypography.amountMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = "${String.format("%.0f", cap.percentage * 100)}%",
                    style = MoneyTypography.labelLarge,
                    color = statusColor,
                    maxLines = 1,
                    softWrap = false
                )
            }

            LinearProgressIndicator(
                progress = { (cap.percentage.coerceIn(0.0, 1.0)).toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = statusColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                gapSize = 0.dp
            )

            Text(
                text = L10n.s.budMedianIncomeText(pct, formatter.format(medianKopecks / 100.0)),
                style = MoneyTypography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BudgetSectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            style = MoneyTypography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun BudgetCategoryCard(
    status: BudgetCategoryStatus,
    formatter: DecimalFormat,
    // v1.11.5: tapping the card opens the plan dialog for this effective plan
    onCard: () -> Unit
) {
    val typeColor = when (status.type) {
        BudgetType.INCOME -> incomeTextColor()
        BudgetType.EXPENSE -> expenseTextColor()
        BudgetType.SAVINGS -> savingsTextColor()
        BudgetType.SUMMARY -> incomeTextColor()
    }

    val statusColor = when (status.status) {
        BudgetStatus.OK -> incomeTextColor()
        BudgetStatus.CLOSE -> warningTextColor()
        BudgetStatus.OVER -> expenseTextColor()
        BudgetStatus.UNDER -> warningTextColor()
    }
    val statusIcon = when (status.status) {
        BudgetStatus.OK -> Icons.Rounded.Check
        BudgetStatus.CLOSE -> Icons.Rounded.PriorityHigh
        BudgetStatus.OVER -> Icons.Rounded.Error
        BudgetStatus.UNDER -> Icons.Rounded.TrendingDown
    }
    val tint = categoryTint(status.category)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCard() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header row: tinted category icon + name, status icon + amounts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(end = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(tint.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            // v1.9.0: user-chosen icon key wins over the name map
                            imageVector = categoryIcon(status.category, status.iconKey),
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = status.category,
                        style = MoneyTypography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${formatter.format(status.actualRubles)} / ${formatter.format(status.plannedRubles)} ₽",
                        style = MoneyTypography.amountSmall,
                        color = typeColor,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // Progress bar
            LinearProgressIndicator(
                progress = { status.percentage.coerceIn(0.0, 1.5).toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = statusColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                gapSize = 0.dp
            )

            // Footer: difference and percentage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = L10n.s.budDifference(
                        "${if (status.differenceRubles >= 0) "+" else ""}${formatter.format(status.differenceRubles)}"
                    ),
                    style = MoneyTypography.labelSmall,
                    color = if (status.differenceRubles >= 0) incomeTextColor() else expenseTextColor(),
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(end = 8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${String.format("%.0f", status.percentage * 100)}%",
                    style = MoneyTypography.labelSmall,
                    color = statusColor,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}
