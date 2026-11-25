package ru.moneyapp.ui.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Percent
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.moneyapp.domain.model.*
import ru.moneyapp.ui.l10n.L10n
import ru.moneyapp.ui.charts.CategoryDonutChart
import ru.moneyapp.ui.charts.CumulativeSavingsChart
import ru.moneyapp.ui.charts.MonthlyBarChart
import ru.moneyapp.ui.dashboard.MonthlyChartData
import ru.moneyapp.ui.theme.MoneyTypography
import ru.moneyapp.ui.theme.categoryIcon
import ru.moneyapp.ui.theme.categoryTint
import ru.moneyapp.ui.theme.expenseColor
import ru.moneyapp.ui.theme.expenseTextColor
import ru.moneyapp.ui.theme.incomeColor
import ru.moneyapp.ui.theme.incomeTextColor
import ru.moneyapp.ui.theme.savingsColor
import ru.moneyapp.ui.theme.savingsTextColor
import java.text.DecimalFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.abs

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val monthlyData by viewModel.monthlyComparison.collectAsStateWithLifecycle(emptyList())
    val topCategories by viewModel.topExpenseCategories.collectAsStateWithLifecycle(emptyList())
    val savingsTrend by viewModel.savingsRateTrend.collectAsStateWithLifecycle(emptyList())
    // v1.11.0: the 10 extended analytics cards (mirror of the mini-app)
    val extended by viewModel.extendedAnalytics.collectAsStateWithLifecycle(null)

    val formatter = remember { DecimalFormat("#,###") }
    // v1.9.0: localized month names (follows the in-app language)
    val monthNames = L10n.s.monthsShort

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val activeMonths = monthlyData.filter {
            it.incomeKopecks != 0L || it.expensesKopecks != 0L || it.savingsKopecks != 0L
        }

        // Monthly bar chart
        item {
            AnalyticsCard(title = L10n.s.anaIncomeExpenses, icon = Icons.Rounded.BarChart, explanation = L10n.s.anaExBars) {
                if (activeMonths.isEmpty()) {
                    EmptyHint(L10n.s.anaNoDataYear)
                } else {
                    MonthlyBarChart(
                        monthlyData = monthlyData.map {
                            MonthlyChartData(
                                month = it.month,
                                incomeKopecks = it.incomeKopecks,
                                expensesKopecks = it.expensesKopecks,
                                savingsKopecks = it.savingsKopecks
                            )
                        },
                        monthNames = monthNames
                    )
                }
            }
        }

        // Cumulative savings line
        item {
            AnalyticsCard(title = L10n.s.anaCumulative, icon = Icons.Rounded.TrendingUp, explanation = L10n.s.anaExCum) {
                // v1.11.6: the piggy line is the NET of SAVINGS_SPEND.
                val cumulative = buildList {
                    var sum = 0L
                    monthlyData.sortedBy { it.month }.forEach { row ->
                        sum += row.savingsKopecks - row.savingsSpendKopecks
                        add(CumulativeSavingsPoint(month = row.month, cumulativeKopecks = sum))
                    }
                }.filter { it.cumulativeKopecks > 0 }
                if (cumulative.size < 2) {
                    EmptyHint(L10n.s.noData)
                } else {
                    CumulativeSavingsChart(cumulativeData = cumulative, monthNames = monthNames)
                }
            }
        }

        // Donut by category
        item {
            AnalyticsCard(title = L10n.s.dashExpensesByCategory, icon = Icons.Rounded.PieChart, explanation = L10n.s.anaExDonut) {
                val totalExpenses = monthlyData.sumOf { it.expensesKopecks }
                val donutSegments = buildList {
                    topCategories.forEach { add(Pair(it.category, it.amountKopecks)) }
                    val rest = totalExpenses - topCategories.sumOf { it.amountKopecks }
                    if (rest > 0) {
                        // v1.10.0: #9 — if a segment already carries the rest's
                        // label (a real «Прочее» category), fold the rest into
                        // it instead of showing a second «Прочее» slice.
                        val idx = indexOfFirst { it.first == L10n.s.anaOther }
                        if (idx >= 0) {
                            this[idx] = Pair(L10n.s.anaOther, this[idx].second + rest)
                        } else {
                            add(Pair(L10n.s.anaOther, rest))
                        }
                    }
                }.sortedByDescending { it.second }
                if (donutSegments.isEmpty()) {
                    EmptyHint(L10n.s.noData)
                } else {
                    CategoryDonutChart(
                        segments = donutSegments,
                        centerTitle = L10n.s.anaTotal,
                        centerValue = "${formatter.format(totalExpenses / 100.0)} ₽",
                        colors = { name -> categoryTint(name) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    donutSegments.forEach { (name, value) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(categoryTint(name))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = name,
                                style = MoneyTypography.labelLarge,
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .padding(end = 8.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${formatter.format(value / 100.0)} ₽",
                                style = MoneyTypography.amountSmall
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${String.format("%.0f", value.toFloat() / (totalExpenses.coerceAtLeast(1L)).toFloat() * 100)}%",
                                style = MoneyTypography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Top 5 expense categories
        item {
            AnalyticsCard(title = L10n.s.anaTop5, icon = Icons.Rounded.EmojiEvents, explanation = L10n.s.anaExTop5) {
                if (topCategories.isEmpty()) {
                    EmptyHint(L10n.s.noData)
                } else {
                    val maxAmount = topCategories.maxOf { it.amountKopecks }
                    topCategories.forEachIndexed { index, cat ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(categoryTint(cat.category).copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        // v1.9.0: user-chosen icon key wins over the name map
                                        imageVector = categoryIcon(cat.category, cat.iconKey),
                                        contentDescription = null,
                                        tint = categoryTint(cat.category),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = "${index + 1}. ${cat.category}",
                                    style = MoneyTypography.bodyMedium,
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
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth((cat.amountKopecks.toFloat() / maxAmount).coerceIn(0f, 1f))
                                        .fillMaxHeight()
                                        .background(
                                            categoryTint(cat.category).copy(alpha = 0.55f),
                                            RoundedCornerShape(3.dp)
                                        )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }

        // Month-over-month comparison table
        item {
            AnalyticsCard(title = L10n.s.anaMonthly, icon = Icons.Rounded.TableChart, explanation = L10n.s.anaExMTable) {
                if (activeMonths.isEmpty()) {
                    EmptyHint(L10n.s.anaNoDataCompare)
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(L10n.s.anaMonth, style = MoneyTypography.labelSmall, modifier = Modifier.width(40.dp))
                        Text(L10n.s.anaIncome, style = MoneyTypography.labelSmall, modifier = Modifier.weight(1f), color = incomeTextColor())
                        Text(L10n.s.anaExpense, style = MoneyTypography.labelSmall, modifier = Modifier.weight(1f), color = expenseTextColor())
                        Text(L10n.s.anaSavings, style = MoneyTypography.labelSmall, modifier = Modifier.weight(1f), color = savingsTextColor())
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 6.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    activeMonths.forEach { data ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(monthNames[data.month], style = MoneyTypography.bodySmall, modifier = Modifier.width(40.dp))
                            Text(
                                "${formatter.format(data.incomeRubles)} ₽",
                                style = MoneyTypography.bodySmall,
                                modifier = Modifier.weight(1f),
                                color = incomeTextColor(),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "${formatter.format(data.expensesRubles)} ₽",
                                style = MoneyTypography.bodySmall,
                                modifier = Modifier.weight(1f),
                                color = expenseTextColor(),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "${formatter.format(data.savingsRubles)} ₽",
                                style = MoneyTypography.bodySmall,
                                modifier = Modifier.weight(1f),
                                color = savingsTextColor(),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Savings rate trend
        item {
            AnalyticsCard(title = L10n.s.anaSavingsTrend, icon = Icons.Rounded.Percent, explanation = L10n.s.anaExRates) {
                val monthsWithData = savingsTrend.filter { it.savingsRate > 0 }
                if (monthsWithData.isEmpty()) {
                    EmptyHint(L10n.s.noData)
                } else {
                    val maxRate = savingsTrend.maxOfOrNull { it.savingsRate } ?: 1.0
                    monthsWithData.forEach { trend ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                monthNames[trend.month],
                                style = MoneyTypography.labelSmall,
                                modifier = Modifier.width(40.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth((trend.savingsRate / maxRate).coerceIn(0.0, 1.0).toFloat())
                                        .fillMaxHeight()
                                        .background(
                                            if (trend.savingsRate >= 0.2) incomeColor().copy(alpha = 0.7f)
                                            else expenseColor().copy(alpha = 0.7f),
                                            RoundedCornerShape(4.dp)
                                        )
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${String.format("%.1f", trend.savingsRate * 100)}%",
                                style = MoneyTypography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (trend.savingsRate >= 0.2) incomeColor() else expenseColor()
                            )
                        }
                    }
                }
            }
        }

        // Average monthly figures (over the months that have data this year)
        item {
            AnalyticsCard(title = L10n.s.anaMonthlyAverage, icon = Icons.Rounded.Insights, explanation = L10n.s.anaExAverages) {
                val activeCount = activeMonths.size
                val avgIncome = if (activeCount > 0)
                    monthlyData.sumOf { it.incomeKopecks }.toDouble() / activeCount / 100.0
                else 0.0
                val avgExpenses = if (activeCount > 0)
                    monthlyData.sumOf { it.expensesKopecks }.toDouble() / activeCount / 100.0
                else 0.0
                // v1.11.6: average savings is the NET of SAVINGS_SPEND.
                val avgSavings = if (activeCount > 0)
                    monthlyData.sumOf { it.savingsKopecks - it.savingsSpendKopecks }.toDouble() / activeCount / 100.0
                else 0.0

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AverageKpi(L10n.s.anaIncome, avgIncome, incomeTextColor(), formatter, Modifier.weight(1f))
                    AverageKpi(L10n.s.anaExpense, avgExpenses, expenseTextColor(), formatter, Modifier.weight(1f))
                    AverageKpi(L10n.s.anaSavings, avgSavings, savingsTextColor(), formatter, Modifier.weight(1f))
                }
            }
        }

        // ====================================================================
        // v1.11.0: extended analytics — 10 new cards (mirror of the mini-app
        // /api/analytics; the mini-app shows them after the averages card).
        // ====================================================================
        if (extended != null) {
            val ext = extended!!

            // 1) Average check by category (top-6 by count)
            //    v1.11.2: current month / last 12 months toggle
            item {
                AnalyticsCard(
                    title = L10n.s.anaAvgCheck,
                    icon = Icons.Rounded.ReceiptLong,
                    explanation = L10n.s.anaExAvgCheck
                ) {
                    var period by remember { mutableStateOf(0) } // 0 = month, 1 = 12 months
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = period == 0,
                            onClick = { period = 0 },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Text(L10n.s.anaAvgCheckPeriodMonth)
                        }
                        SegmentedButton(
                            selected = period == 1,
                            onClick = { period = 1 },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Text(L10n.s.anaAvgCheckPeriodYear)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    val avgRows = if (period == 0) ext.avgCheck else ext.avgCheckYear
                    if (avgRows.isEmpty()) {
                        EmptyHint(
                            if (period == 0) L10n.s.anaAvgCheckEmptyMonth
                            else L10n.s.anaAvgCheckEmptyYear
                        )
                    } else {
                        avgRows.forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CategoryChip(row.category, row.iconKey)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = row.category,
                                    style = MoneyTypography.bodyMedium,
                                    modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = L10n.s.anaOpsCount(row.count),
                                    style = MoneyTypography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${formatter.format(row.avgKopecks / 100.0)} ₽",
                                    style = MoneyTypography.amountSmall,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            // 2) Top-5 of the month with the trend vs the previous month
            item {
                AnalyticsCard(title = L10n.s.anaTop5Month, icon = Icons.Rounded.QueryStats, explanation = L10n.s.anaExTop5Trend) {
                    if (ext.top5Trend.isEmpty()) {
                        EmptyHint(L10n.s.noData)
                    } else {
                        ext.top5Trend.forEachIndexed { index, cat ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MoneyTypography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(18.dp)
                                )
                                Text(
                                    text = cat.category,
                                    style = MoneyTypography.bodyMedium,
                                    modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${formatter.format(cat.amountKopecks / 100.0)} ₽",
                                    style = MoneyTypography.bodySmall,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                TrendBadge(cat)
                            }
                        }
                    }
                }
            }

            // 3) Financial cushion (runway)
            item {
                AnalyticsCard(title = L10n.s.anaCushion, icon = Icons.Rounded.Shield, explanation = L10n.s.anaExCushion) {
                    val c = ext.cushion
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                L10n.s.anaCushionBase,
                                style = MoneyTypography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "${formatter.format(c.baseKopecks / 100.0)} ₽",
                                style = MoneyTypography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                L10n.s.anaCushionAvgExp,
                                style = MoneyTypography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "${formatter.format(c.avgMonthlyExpenseKopecks / 100.0)} ₽",
                                style = MoneyTypography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = expenseTextColor(),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (c.runwayMonths == null) {
                        EmptyHint(L10n.s.anaCushionLowData)
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = formatRunway(c.runwayMonths),
                                style = MoneyTypography.displaySmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = savingsTextColor()
                            )
                            Text(
                                L10n.s.anaCushionMonths(formatRunway(c.runwayMonths)),
                                style = MoneyTypography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 4) Budget pace
            //    v1.11.2: verdict row + bar with plan marker + spent/plan-today/limit tricol
            item {
                AnalyticsCard(
                    title = L10n.s.anaPace,
                    icon = Icons.Rounded.Speed,
                    explanation = L10n.s.anaExPace
                ) {
                    val p = ext.pace
                    when {
                        p.state == "Disabled" -> EmptyHint(L10n.s.anaPaceDisabled)
                        p.spentPct == null -> EmptyHint(L10n.s.anaPaceAwaiting)
                        else -> {
                            val today = LocalDate.now()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when (p.status) {
                                        "AHEAD" -> L10n.s.anaPaceSlow
                                        "BEHIND" -> L10n.s.anaPaceFast
                                        else -> L10n.s.anaPaceOn
                                    },
                                    style = MoneyTypography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = when (p.status) {
                                        "AHEAD" -> incomeColor()
                                        "BEHIND" -> expenseColor()
                                        else -> savingsColor()
                                    }
                                )
                                Text(
                                    text = L10n.s.anaPaceDayOf(today.dayOfMonth, today.lengthOfMonth()),
                                    style = MoneyTypography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            BoxWithConstraints(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .fillMaxWidth()
                                        .height(12.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        )
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .fillMaxWidth(p.spentPct.coerceIn(0.0, 1.0).toFloat())
                                        .height(12.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(expenseColor().copy(alpha = 0.75f))
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .offset(x = maxWidth * p.expectedPct.coerceIn(0.0, 1.0).toFloat() - 1.dp)
                                        .width(2.dp)
                                        .height(18.dp)
                                        .background(MaterialTheme.colorScheme.onSurface)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = L10n.s.anaPaceSpentPlan(pctText(p.spentPct), pctText(p.expectedPct)),
                                style = MoneyTypography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                PaceKpi(
                                    L10n.s.anaPaceSpent,
                                    "${formatter.format(p.spentKopecks / 100.0)} ₽",
                                    expenseTextColor(),
                                    Modifier.weight(1f)
                                )
                                PaceKpi(
                                    L10n.s.anaPacePlanToday,
                                    p.capKopecks?.let {
                                        val planKopecks = Math.round(it * p.expectedPct).toLong()
                                        "${formatter.format(planKopecks / 100.0)} ₽"
                                    } ?: "—",
                                    MaterialTheme.colorScheme.onSurface,
                                    Modifier.weight(1f)
                                )
                                PaceKpi(
                                    L10n.s.anaPaceLimit,
                                    p.capKopecks?.let { "${formatter.format(it / 100.0)} ₽" } ?: "—",
                                    MaterialTheme.colorScheme.onSurface,
                                    Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // 5) Big expenses (top-10 of the last 3 months)
            item {
                AnalyticsCard(title = L10n.s.anaBigExpenses, icon = Icons.Rounded.AttachMoney, explanation = L10n.s.anaExBigExp) {
                    if (ext.bigExpenses.isEmpty()) {
                        EmptyHint(L10n.s.noData)
                    } else {
                        ext.bigExpenses.forEach { b ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = b.category,
                                    style = MoneyTypography.bodyMedium,
                                    modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (b.description.isNotEmpty()) {
                                    Text(
                                        text = b.description,
                                        style = MoneyTypography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = 96.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    text = LocalDate.ofEpochDay(b.date).format(
                                        DateTimeFormatter.ofPattern("dd.MM", Locale.getDefault())
                                    ),
                                    style = MoneyTypography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(44.dp),
                                    textAlign = TextAlign.End
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${formatter.format(b.amountKopecks / 100.0)} ₽",
                                    style = MoneyTypography.bodySmall,
                                    color = expenseTextColor(),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            // v1.11.2: subscriptions (auto-detection) and year-end balance forecast removed
            // (user request: they were unclear / not needed)

            // 8) Balance curve of the current month
            item {
                AnalyticsCard(title = L10n.s.anaBalanceCurve, icon = Icons.Rounded.ShowChart, explanation = L10n.s.anaExCurve) {
                    if (ext.balanceCurve.size < 2) {
                        EmptyHint(L10n.s.noData)
                    } else {
                        BalanceCurveChart(points = ext.balanceCurve)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "1",
                                style = MoneyTypography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                (ext.balanceCurve.size / 2).coerceAtLeast(1).toString(),
                                style = MoneyTypography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                ext.balanceCurve.size.toString(),
                                style = MoneyTypography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 9) Expenses by weekday (current year)
            item {
                AnalyticsCard(title = L10n.s.anaWeekday, icon = Icons.Rounded.CalendarMonth, explanation = L10n.s.anaExWeekday) {
                    val wd = ext.weekday
                    val total = wd.sumOf { it.amountKopecks }
                    if (total == 0L) {
                        EmptyHint(L10n.s.noData)
                    } else {
                        val max = wd.maxOf { it.amountKopecks }.coerceAtLeast(1L)
                        wd.forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = L10n.s.weekdayShort[row.weekday],
                                    style = MoneyTypography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(40.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        )
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(
                                                (row.amountKopecks.toFloat() / max.toFloat()).coerceIn(0f, 1f)
                                            )
                                            .fillMaxHeight()
                                            .background(
                                                expenseColor().copy(alpha = 0.7f),
                                                RoundedCornerShape(4.dp)
                                            )
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${formatter.format(row.amountKopecks / 100.0)} ₽",
                                    style = MoneyTypography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = L10n.s.anaWeekdayTotal("${formatter.format(total / 100.0)} ₽"),
                            style = MoneyTypography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 10) Calendar heatmap (12 months, GitHub style, Mon–Sun rows)
            //     v1.11.2: month labels, tap a day → detail line, legend; only the
            //     grid scrolls horizontally, the rows below stay in place
            item {
                AnalyticsCard(
                    title = L10n.s.anaCalendar,
                    icon = Icons.Rounded.GridOn,
                    explanation = L10n.s.anaExHeatmap
                ) {
                    val cal = ext.calendar
                    if (cal.days.isEmpty()) {
                        EmptyHint(L10n.s.noData)
                    } else {
                        val byDay = remember(cal.days) {
                            cal.days.associate { it.date to it.amountKopecks }
                        }
                        val today = remember { LocalDate.now() }
                        val rangeStart = remember { YearMonth.from(today).minusMonths(11).atDay(1) }
                        val rangeEnd = remember { YearMonth.from(today).atEndOfMonth() }
                        val firstMonday = remember {
                            rangeStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                        }
                        val weeks = remember(rangeStart, rangeEnd, firstMonday) {
                            buildList {
                                var d = firstMonday
                                val endExclusive = rangeEnd.plusDays(1)
                                while (d < endExclusive) {
                                    add((0 until 7).map { d.plusDays(it.toLong()) })
                                    d = d.plusWeeks(1)
                                }
                            }
                        }
                        // Month labels: shown at the first week column where the month changes
                        val monthLabels = remember(weeks, rangeStart, rangeEnd) {
                            var prevMonth = -1
                            weeks.map { week ->
                                val firstInRange = week.firstOrNull { it >= rangeStart && it <= rangeEnd }
                                if (firstInRange != null && firstInRange.monthValue != prevMonth) {
                                    prevMonth = firstInRange.monthValue
                                    // v1.11.3: monthsShort is 1-based ("" at 0) — use the
                                    // accessor; the old [monthValue - 1] showed the PREVIOUS month
                                    // v1.11.4: 3-letter short names, like the other chart axes
                                    L10n.monthShort(firstInRange.monthValue)
                                } else {
                                    ""
                                }
                            }
                        }
                        var selected by remember { mutableStateOf<LocalDate?>(null) }
                        val track = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        val maxF = cal.maxKopecks.coerceAtLeast(1L).toFloat()
                        val levelColors = listOf(
                            track,
                            mixColor(track, expenseColor(), 0.35f),
                            mixColor(track, expenseColor(), 0.55f),
                            mixColor(track, expenseColor(), 0.8f),
                            expenseColor()
                        )
                        // Only the grid is inside the horizontal scroller
                        // v1.11.3: start scrolled to the RIGHT edge so the current
                        // month (the last columns) is visible without scrolling
                        val hmScroll = rememberScrollState()
                        LaunchedEffect(weeks) {
                            hmScroll.scrollTo(hmScroll.maxValue)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(hmScroll),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            weeks.forEachIndexed { w, week ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    // v1.11.4: month label as a custom Layout slot.
                                    // A plain Text inside the 15.dp column was clamped
                                    // to 15.dp ("Апр" → "Ап"). The slot still occupies
                                    // 15x14dp in the grid, but the Text is measured with
                                    // LOOSE constraints and overflows to the right into
                                    // the next (empty) label columns. The 3-letter name
                                    // is fully visible, like the other chart axes.
                                    Layout(
                                        content = {
                                            if (monthLabels[w].isNotEmpty()) {
                                                Text(
                                                    text = monthLabels[w],
                                                    style = MoneyTypography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1
                                                )
                                            }
                                        },
                                        modifier = Modifier.width(15.dp).height(14.dp)
                                    ) { measurables, constraints ->
                                        if (measurables.isEmpty()) {
                                            layout(constraints.minWidth, constraints.maxHeight) { }
                                        } else {
                                            // NOTE: this build's compose is a local fork —
                                            // Constraints is a value class in ui.unit,
                                            // no LayoutResult: placement is scope.layout(w,h){}
                                            val p = measurables.first().measure(
                                                Constraints(0, Int.MAX_VALUE, 0, constraints.maxHeight)
                                            )
                                            layout(constraints.minWidth, constraints.maxHeight) {
                                                p.place(0, constraints.maxHeight - p.height)
                                            }
                                        }
                                    }
                                    week.forEach { d ->
                                        val inRange = d >= rangeStart && d <= rangeEnd
                                        val v = if (inRange) byDay[d.toEpochDay()] ?: 0L else 0L
                                        val level = if (!inRange) 0
                                        else {
                                            val f = v.toFloat() / maxF
                                            when {
                                                v <= 0L -> 0
                                                f < 0.15f -> 1
                                                f < 0.4f -> 2
                                                f < 0.7f -> 3
                                                else -> 4
                                            }
                                        }
                                        val isSel = inRange && selected == d
                                        Box(
                                            modifier = Modifier
                                                .width(15.dp)
                                                .height(15.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .then(
                                                    if (inRange) {
                                                        Modifier.clickable {
                                                            selected = if (selected == d) null else d
                                                        }
                                                    } else Modifier
                                                )
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(11.dp)
                                                    .align(Alignment.Center)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(
                                                        if (inRange) levelColors[level] else Color.Transparent
                                                    )
                                                    .then(
                                                        if (isSel) {
                                                            Modifier.border(
                                                                1.5.dp,
                                                                MaterialTheme.colorScheme.onSurface,
                                                                RoundedCornerShape(3.dp)
                                                            )
                                                        } else Modifier
                                                    )
                                            ) {
                                                // leaf cell: background only
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        val sel = selected
                        if (sel != null) {
                            val sv = byDay[sel.toEpochDay()]
                            Text(
                                text = "${sel.dayOfMonth} ${L10n.monthFull(sel.monthValue)} ${sel.year} · " +
                                    (if (sv != null && sv > 0L) {
                                        "${formatter.format(sv / 100.0)} ₽"
                                    } else L10n.s.anaHeatNoSpend),
                                style = MoneyTypography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = L10n.s.anaHeatLess,
                                style = MoneyTypography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            levelColors.forEach { c ->
                                Box(
                                    modifier = Modifier
                                        .size(11.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(c)
                                )
                            }
                            Text(
                                text = L10n.s.anaHeatMore,
                                style = MoneyTypography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = L10n.s.anaHeatTapHint,
                            style = MoneyTypography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = L10n.s.anaCalendarMax("${formatter.format(cal.maxKopecks / 100.0)} ₽"),
                            style = MoneyTypography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
private fun AnalyticsCard(
    title: String,
    icon: ImageVector,
    // v1.11.2: info button in the header → dialog with «what it shows + formula»
    explanation: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var showInfo by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                // v1.11.3: the title gets the FULL remaining width (before it was
                // split 50/50 with the spacer → 2–4 line wraps on phones);
                // max 2 lines, the info button stays on the right of the row
                Text(
                    text = title,
                    style = MoneyTypography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (explanation != null) {
                    IconButton(
                        onClick = { showInfo = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = L10n.s.anaInfo,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
    if (showInfo && explanation != null) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text(title, style = MoneyTypography.titleMedium) },
            text = { Text(explanation, style = MoneyTypography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = { showInfo = false }) {
                    Text(L10n.s.ok)
                }
            }
        )
    }
}

@Composable
private fun EmptyHint(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            style = MoneyTypography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AverageKpi(
    label: String,
    valueRubles: Double,
    color: androidx.compose.ui.graphics.Color,
    formatter: DecimalFormat,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = MoneyTypography.labelSmall, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            "${formatter.format(valueRubles)} ₽",
            style = MoneyTypography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// Data class for monthly comparison
data class MonthlyComparisonData(
    val month: Int,
    val year: Int,
    val incomeKopecks: Long,
    val expensesKopecks: Long,
    val savingsKopecks: Long,
    val savingsSpendKopecks: Long = 0
) {
    val incomeRubles: Double get() = incomeKopecks / 100.0
    val expensesRubles: Double get() = expensesKopecks / 100.0
    val savingsRubles: Double get() = savingsKopecks / 100.0
}

// Data class for savings rate trend
data class SavingsRateTrend(
    val month: Int,
    val savingsRate: Double  // 0..1
)

// ============================================================================
// v1.11.0: helpers for the extended analytics cards
// ============================================================================

/** Rounded category icon chip (same look as the Top-5 list above). */
@Composable
private fun CategoryChip(name: String, iconKey: String?, size: Int = 32) {
    val tint = categoryTint(name)
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size * 0.3).dp))
            .background(tint.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = categoryIcon(name, iconKey),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size((size * 0.56).dp)
        )
    }
}

/** ▲/▼ delta vs the previous month, or «без истории» when there is none. */
@Composable
private fun TrendBadge(cat: TrendCategory) {
    when {
        cat.deltaPct == null -> Text(
            text = L10n.s.anaNoPrev,
            style = MoneyTypography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(84.dp),
            textAlign = TextAlign.End
        )
        cat.deltaKopecks > 0L -> Text(
            text = "▲ ${String.format(Locale.getDefault(), "%.1f", abs(cat.deltaPct))}%",
            style = MoneyTypography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = expenseTextColor(),
            modifier = Modifier.width(84.dp),
            textAlign = TextAlign.End
        )
        cat.deltaKopecks < 0L -> Text(
            text = "▼ ${String.format(Locale.getDefault(), "%.1f", abs(cat.deltaPct))}%",
            style = MoneyTypography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = incomeTextColor(),
            modifier = Modifier.width(84.dp),
            textAlign = TextAlign.End
        )
        else -> Text(
            text = "— 0%",
            style = MoneyTypography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(84.dp),
            textAlign = TextAlign.End
        )
    }
}

/** Small label + value cell (no ₽ — the value string is preformatted). */
@Composable
private fun PaceKpi(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = MoneyTypography.labelSmall, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            value,
            style = MoneyTypography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * v1.11.0: balance curve of the current month — a plain Canvas line chart
 * (carry-over + daily net), with a zero line when the range goes negative.
 */
@Composable
private fun BalanceCurveChart(points: List<BalanceCurvePoint>) {
    val maxV = points.maxOf { it.balanceKopecks }
    val minV = points.minOf { it.balanceKopecks }
    val lo = minOf(minV, 0L)
    val hi = maxOf(maxV, 0L)
    val span = (hi - lo).coerceAtLeast(1L)
    val lineColor = incomeColor()
    Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
        val padTop = 10f
        val padBottom = 10f
        val n = points.size
        fun x(i: Int): Float = if (n <= 1) size.width / 2f else size.width * i / (n - 1)
        fun y(v: Long): Float =
            padTop + (size.height - padTop - padBottom) *
                (1f - ((v - lo).toFloat() / span.toFloat()))
        if (lo < 0L) {
            drawLine(
                color = Color(0x6696969E),
                start = Offset(0f, y(0L)),
                end = Offset(size.width, y(0L)),
                strokeWidth = 1f
            )
        }
        val path = Path().apply {
            points.forEachIndexed { i, p ->
                if (i == 0) moveTo(x(i), y(p.balanceKopecks))
                else lineTo(x(i), y(p.balanceKopecks))
            }
        }
        drawPath(
            path,
            color = lineColor,
            style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        points.forEachIndexed { i, p ->
            drawCircle(color = lineColor, radius = 2.5f, center = Offset(x(i), y(p.balanceKopecks)))
        }
    }
}

/** v1.11.0: manual RGB mix (Color.blend is not available in this Compose version). */
private fun mixColor(a: Color, b: Color, weight: Float): Color =
    Color(
        a.red + (b.red - a.red) * weight,
        a.green + (b.green - a.green) * weight,
        a.blue + (b.blue - a.blue) * weight,
        a.alpha + (b.alpha - a.alpha) * weight
    )

/** JS-parity runway format: "19,8" (no trailing ",0"). */
private fun formatRunway(runway: Double): String {
    val rounded = Math.round(runway * 10) / 10.0
    return if (rounded % 1.0 == 0.0)
        rounded.toLong().toString()
    else String.format(Locale.getDefault(), "%.1f", rounded).replace('.', ',')
}

/** JS-parity percent: Math.round(x * 100) + "%". */
private fun pctText(fraction: Double?): String =
    if (fraction == null) "—"
    else "${Math.round(fraction * 100)}%"

