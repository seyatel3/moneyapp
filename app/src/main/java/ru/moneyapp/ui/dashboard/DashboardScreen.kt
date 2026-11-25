package ru.moneyapp.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Percent
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.moneyapp.domain.model.BudgetCapInfo
import ru.moneyapp.domain.model.BudgetCapStatus
import ru.moneyapp.domain.model.CategoryExpenseBreakdown
import ru.moneyapp.domain.model.DashboardKPIs
import ru.moneyapp.domain.model.TransactionType
import ru.moneyapp.ui.l10n.L10n
import ru.moneyapp.ui.theme.MoneyTypography
import ru.moneyapp.ui.theme.balanceCardGradient
import ru.moneyapp.ui.theme.balanceCardSubTextColor
import ru.moneyapp.ui.theme.balanceCardTextColor
import ru.moneyapp.ui.theme.categoryIcon
import ru.moneyapp.ui.theme.categoryTint
import ru.moneyapp.ui.theme.expenseColor
import ru.moneyapp.ui.theme.incomeColor
import ru.moneyapp.ui.theme.savingsColor
import ru.moneyapp.ui.theme.typeIcon
import ru.moneyapp.ui.theme.warningColor
import java.text.DecimalFormat
import java.time.LocalDate
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val kpis by viewModel.kpis.collectAsStateWithLifecycle(DashboardKPIs(0, 0, 0, 0, 0.0))
    val categoryData by viewModel.categoryBreakdown.collectAsStateWithLifecycle(emptyList())
    val selectedYear by viewModel.selectedYear.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val capInfo by viewModel.capInfo.collectAsStateWithLifecycle(null)
    // v1.10.0: month / year period mode
    val periodMode by viewModel.periodMode.collectAsStateWithLifecycle(PeriodMode.MONTH)

    // v1.9.0: localized month name (follows the in-app language)
    val monthName = remember(selectedMonth, L10n.lang) {
        L10n.monthFull(selectedMonth)
    }
    // v1.10.0: the period label used in the header + summary title
    val periodLabel = if (periodMode == PeriodMode.YEAR) selectedYear.toString() else monthName
    val sumFormatter = remember { DecimalFormat("#,###") }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToTransactions,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Rounded.Add, contentDescription = L10n.s.add)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            // v1.10.0: #6 — no gap between the header and the period selector
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Period selector: month is primary, year is secondary (or the year alone)
            item {
                MonthYearSelector(
                    mode = periodMode,
                    monthName = monthName,
                    year = selectedYear,
                    onPrev = { viewModel.changePeriod(-1) },
                    onNext = { viewModel.changePeriod(1) }
                )
            }

            // v1.10.0: #7 — month / year toggle
            item {
                PeriodModeToggle(
                    mode = periodMode,
                    onSelect = { viewModel.setPeriodMode(it) }
                )
            }

            // KPI: hero balance + 2x2 grid
            item {
                KpiSection(kpis = kpis)
            }

            // v1.6.0: monthly cap card (only when the cap is enabled)
            capInfo?.let { cap ->
                item {
                    CapCard(cap = cap, formatter = sumFormatter)
                }
            }

            // v1.10.0: summary for the selected period (month OR year). The
            // values come from the period KPIs, so they follow the toggle.
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ReceiptLong,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = L10n.s.dashSummaryFor(periodLabel),
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MonthSummaryValue(
                                label = L10n.s.txTypeIncome,
                                kopecks = kpis.totalIncomeKopecks,
                                color = incomeColor(),
                                formatter = sumFormatter,
                                modifier = Modifier.weight(1f)
                            )
                            MonthSummaryValue(
                                label = L10n.s.txTypeExpense,
                                kopecks = kpis.totalExpensesKopecks,
                                color = expenseColor(),
                                formatter = sumFormatter,
                                modifier = Modifier.weight(1f)
                            )
                            MonthSummaryValue(
                                label = L10n.s.dashSavings,
                                kopecks = kpis.totalSavingsKopecks,
                                color = savingsColor(),
                                formatter = sumFormatter,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Category breakdown for the selected month
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PieChart,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = L10n.s.dashExpensesByCategory,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        if (categoryData.isEmpty()) {
                            EmptyState(
                                icon = Icons.Rounded.PieChart,
                                title = L10n.s.dashNoExpensesTitle,
                                hint = L10n.s.dashNoExpensesHint
                            )
                        } else {
                            CategoryBreakdownList(categoryData = categoryData, formatter = sumFormatter)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthYearSelector(
    mode: PeriodMode,
    monthName: String,
    year: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    // v1.10.0: in YEAR mode the year is the primary title (no second line);
    // the arrows step the year instead of the month.
    val isYear = mode == PeriodMode.YEAR
    val prevDesc = if (isYear) L10n.s.prevYear else L10n.s.prevMonth
    val nextDesc = if (isYear) L10n.s.nextYear else L10n.s.nextMonth
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.Rounded.ChevronLeft, contentDescription = prevDesc)
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(
                text = if (isYear) year.toString() else monthName,
                style = MoneyTypography.titleLarge
            )
            if (!isYear) {
                Text(
                    text = year.toString(),
                    style = MoneyTypography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        IconButton(onClick = onNext) {
            Icon(Icons.Rounded.ChevronRight, contentDescription = nextDesc)
        }
    }
}

// v1.10.0: #7 — switch the dashboard between a single month and the whole year.
@Composable
private fun PeriodModeToggle(
    mode: PeriodMode,
    onSelect: (PeriodMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = mode == PeriodMode.MONTH,
            onClick = { onSelect(PeriodMode.MONTH) },
            label = { Text(L10n.s.dashPeriodMonth, style = MoneyTypography.labelMedium) }
        )
        FilterChip(
            selected = mode == PeriodMode.YEAR,
            onClick = { onSelect(PeriodMode.YEAR) },
            label = { Text(L10n.s.dashPeriodYear, style = MoneyTypography.labelMedium) }
        )
    }
}

@Composable
private fun KpiSection(kpis: DashboardKPIs) {
    val formatter = remember { DecimalFormat("#,###") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Hero: balance, soft gradient instead of a flat fill
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(balanceCardGradient())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(Color.White.copy(alpha = 0.14f))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Wallet,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(7.dp)
                                .align(Alignment.Center),
                            tint = balanceCardTextColor()
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = L10n.s.dashBalance,
                        style = MoneyTypography.labelLarge,
                        color = balanceCardSubTextColor()
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${formatter.format(kpis.balanceRubles)} ₽",
                    style = MoneyTypography.balanceHero,
                    color = balanceCardTextColor(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 2x2 grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiCard(
                    icon = typeIcon(TransactionType.INCOME),
                    title = L10n.s.dashIncome,
                    value = "${formatter.format(kpis.totalIncomeRubles)} ₽",
                    accent = incomeColor()
                )
                KpiCard(
                    icon = Icons.Rounded.Percent,
                    title = L10n.s.dashSavingsRate,
                    value = String.format("%.0f%%", kpis.savingsRate * 100),
                    accent = warningColor()
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiCard(
                    icon = typeIcon(TransactionType.EXPENSE),
                    title = L10n.s.dashExpenses,
                    value = "${formatter.format(kpis.totalExpensesRubles)} ₽",
                    accent = expenseColor()
                )
                KpiCard(
                    icon = typeIcon(TransactionType.SAVINGS),
                    title = L10n.s.dashSavings,
                    value = "${formatter.format(kpis.totalSavingsRubles)} ₽",
                    accent = savingsColor()
                )
            }
        }
    }
}

// v1.6.0: total monthly cap card (compact variant for the dashboard)
@Composable
private fun CapCard(
    cap: BudgetCapInfo,
    formatter: DecimalFormat
) {
    val statusColor = when (cap.status) {
        BudgetCapStatus.OK -> incomeColor()
        BudgetCapStatus.CLOSE -> warningColor()
        BudgetCapStatus.OVER -> expenseColor()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
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
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                Text(
                    text = "${formatter.format(cap.spentRubles)} / ${formatter.format(cap.capRubles)} ₽",
                    style = MoneyTypography.amountSmall,
                    color = MaterialTheme.colorScheme.onSurface,
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
                    L10n.s.dashLeft(
                        formatter.format(cap.remainingKopecks / 100.0),
                        String.format("%.0f", cap.percentage * 100)
                    )
                } else {
                    L10n.s.dashOver(
                        formatter.format((-cap.remainingKopecks) / 100.0),
                        String.format("%.0f", cap.percentage * 100)
                    )
                },
                style = MoneyTypography.labelSmall,
                color = if (cap.remainingKopecks >= 0) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    expenseColor()
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun KpiCard(icon: ImageVector, title: String, value: String, accent: Color) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = accent
                )
                Text(
                    text = title,
                    style = MoneyTypography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MoneyTypography.amountMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MonthSummaryValue(
    label: String,
    kopecks: Long,
    color: Color,
    formatter: DecimalFormat,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MoneyTypography.labelMedium,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${formatter.format(kopecks / 100.0)} ₽",
            style = MoneyTypography.amountMedium,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CategoryBreakdownList(
    categoryData: List<CategoryExpenseBreakdown>,
    formatter: DecimalFormat
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        categoryData.forEach { cat ->
            val tint = categoryTint(cat.category)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(tint.copy(alpha = 0.14f))
                ) {
                    Icon(
                        // v1.9.0: user-chosen icon key wins over the name map
                    imageVector = categoryIcon(cat.category, cat.iconKey),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(7.dp)
                            .align(Alignment.Center),
                        tint = tint
                    )
                }
                Text(
                    text = cat.category,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(end = 8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${formatter.format(cat.amountRubles)} ₽",
                    style = MoneyTypography.amountSmall,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { cat.percentageOfExpenses.toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(MaterialTheme.shapes.extraSmall),
                color = tint,
                trackColor = MaterialTheme.colorScheme.outlineVariant,
                gapSize = 0.dp
            )
        }
    }
}

@Composable
private fun EmptyState(icon: ImageVector, title: String, hint: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.Center),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
