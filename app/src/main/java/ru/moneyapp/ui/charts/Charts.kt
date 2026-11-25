package ru.moneyapp.ui.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import ru.moneyapp.domain.model.CumulativeSavingsPoint
import ru.moneyapp.ui.dashboard.MonthlyChartData
import ru.moneyapp.ui.l10n.L10n
import ru.moneyapp.ui.theme.MoneyTypography
import ru.moneyapp.ui.theme.MotionTokens
import ru.moneyapp.ui.theme.expenseColor
import ru.moneyapp.ui.theme.incomeColor
import ru.moneyapp.ui.theme.savingsColor
import java.text.DecimalFormat

/**
 * Vertical grouped bar chart: income / expense per month, 12 slots.
 * Canvas-drawn, bars grow in over 300 ms.
 */
@Composable
fun MonthlyBarChart(
    monthlyData: List<MonthlyChartData>,
    monthNames: List<String>,
    modifier: Modifier = Modifier
) {
    val incomeC = incomeColor()
    val expenseC = expenseColor()
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (progress.value < 1f) progress.animateTo(1f, MotionTokens.slow)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        val chartData = (1..12).map { m ->
            monthlyData.firstOrNull { it.month == m }
                ?: MonthlyChartData(m, 0L, 0L, 0L)
        }
        val maxVal = (chartData.maxOfOrNull { maxOf(it.incomeKopecks, it.expensesKopecks) } ?: 0L)
            .coerceAtLeast(1L)

        val baselineColor = MaterialTheme.colorScheme.outlineVariant
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            val slot = size.width / 12f
            val barW = (slot * 0.30f).coerceAtMost(18.dp.toPx())
            val gap = 2.dp.toPx()
            val baseY = size.height - 4.dp.toPx()
            chartData.forEachIndexed { i, data ->
                val cx = slot * i + slot / 2f
                // Income bar
                val ih = (data.incomeKopecks.toFloat() / maxVal.toFloat()) *
                    (size.height - 16.dp.toPx()) * progress.value
                if (ih > 0f) {
                    drawRoundRect(
                        color = incomeC,
                        topLeft = Offset(cx - gap / 2f - barW, baseY - ih),
                        size = Size(barW, ih),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
                    )
                }
                // Expense bar
                val eh = (data.expensesKopecks.toFloat() / maxVal.toFloat()) *
                    (size.height - 16.dp.toPx()) * progress.value
                if (eh > 0f) {
                    drawRoundRect(
                        color = expenseC,
                        topLeft = Offset(cx + gap / 2f, baseY - eh),
                        size = Size(barW, eh),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
                    )
                }
                // Baseline tick
                drawLine(
                    color = baselineColor,
                    start = Offset(cx - slot * 0.32f, baseY),
                    end = Offset(cx + slot * 0.32f, baseY),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            (1..12).forEach { m ->
                Text(
                    text = monthNames[m],
                    style = MoneyTypography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .weight(1f),
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // v1.9.0: localized legend (follows the in-app language)
            listOf(L10n.s.anaIncome to incomeC, L10n.s.anaExpense to expenseC).forEach { (label, color) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(color, RoundedCornerShape(3.dp))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = label, style = MoneyTypography.labelSmall)
                }
            }
        }
    }
}

/**
 * Cumulative savings line chart: the line draws itself over 300 ms.
 * v1.10.0: tapping a month's point shows its cumulative amount below the axis
 * (tap the same point again to deselect).
 */
@Composable
fun CumulativeSavingsChart(
    cumulativeData: List<CumulativeSavingsPoint>,
    monthNames: List<String>,
    modifier: Modifier = Modifier
) {
    val lineColor = savingsColor()
    val formatter = remember { DecimalFormat("#,###") }
    val density = LocalDensity.current
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (progress.value < 1f) progress.animateTo(1f, MotionTokens.slow)
    }

    // v1.10.0: #8 — index of the tapped point (-1 = nothing selected).
    var selected by remember { mutableIntStateOf(-1) }
    LaunchedEffect(cumulativeData) {
        if (selected >= cumulativeData.size) selected = -1
    }

    val maxVal = (cumulativeData.maxOfOrNull { it.cumulativeKopecks } ?: 0L).coerceAtLeast(1L)
    val n = cumulativeData.size
    if (n < 2) return

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                // v1.10.0: #8 — map the tap's x offset to the nearest point
                .pointerInput(n) {
                    detectTapGestures { offset ->
                        val padHpx = with(density) { 8.dp.toPx() }
                        val stepX = (size.width - 2f * padHpx) / (n - 1)
                        val idx = ((offset.x - padHpx) / stepX).roundToInt().coerceIn(0, n - 1)
                        selected = if (selected == idx) -1 else idx
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val padH = 8.dp.toPx()
                val padTop = 16.dp.toPx()
                val padBottom = 8.dp.toPx()
                val stepX = (size.width - 2f * padH) / (n - 1)
                val pts = (0 until n).map { i ->
                    val v = cumulativeData[i].cumulativeKopecks.toFloat() / maxVal.toFloat()
                    Offset(
                        padH + i * stepX,
                        padTop + (1f - v) * (size.height - padTop - padBottom)
                    )
                }
                // v1.10.0: vertical guide for the selected point
                if (selected in 0 until n) {
                    drawLine(
                        color = lineColor.copy(alpha = 0.30f),
                        start = Offset(pts[selected].x, padTop),
                        end = Offset(pts[selected].x, size.height - padBottom),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                // Partial line: progress * (n-1) full segments + one fractional
                val totalSeg = (n - 1) * progress.value
                val lastSeg = (totalSeg.toInt() + 1).coerceIn(0, n - 1)
                for (i in 0 until lastSeg) {
                    val segProgress = (totalSeg - i).coerceIn(0f, 1f)
                    val end = Offset(
                        pts[i].x + (pts[i + 1].x - pts[i].x) * segProgress,
                        pts[i].y + (pts[i + 1].y - pts[i].y) * segProgress
                    )
                    drawLine(
                        color = lineColor,
                        start = pts[i],
                        end = end,
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    if (segProgress >= 1f) {
                        drawCircle(color = lineColor, radius = 3.dp.toPx(), center = pts[i + 1])
                    }
                }
                if (progress.value > 0f) {
                    drawCircle(color = lineColor, radius = 3.dp.toPx(), center = pts[0])
                }
                // v1.10.0: emphasized dot on the selected point
                if (selected in 0 until n) {
                    drawCircle(color = Color.White, radius = 5.dp.toPx(), center = pts[selected])
                    drawCircle(color = lineColor, radius = 4.dp.toPx(), center = pts[selected])
                }
            }
            // Final value (hidden while a month is selected — its value is shown below)
            if (selected < 0) {
                Text(
                    text = "${formatter.format(cumulativeData.last().cumulativeRubles)} ₽",
                    style = MoneyTypography.amountSmall,
                    color = lineColor,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            cumulativeData.forEachIndexed { i, point ->
                Text(
                    text = monthNames[point.month],
                    style = MoneyTypography.labelSmall,
                    color = if (selected == i) lineColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (selected == i) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
            }
        }

        // v1.10.0: #8 — the selected month's cumulative amount
        if (selected in cumulativeData.indices) {
            val point = cumulativeData[selected]
            Text(
                text = "${L10n.monthFull(point.month)}: ${formatter.format(point.cumulativeRubles)} ₽",
                style = MoneyTypography.labelMedium,
                color = lineColor,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

/**
 * Donut chart of expense categories (top N + "Прочее").
 * Canvas-drawn arcs sweep in over 300 ms.
 */
@Composable
fun CategoryDonutChart(
    segments: List<Pair<String, Long>>,   // name -> kopecks, non-empty
    centerTitle: String,
    centerValue: String,
    colors: (String) -> Color,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (progress.value < 1f) progress.animateTo(1f, MotionTokens.slow)
    }
    val total = (segments.sumOf { it.second }.coerceAtLeast(1L)).toFloat()

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .size(220.dp)
        ) {
            val stroke = 26.dp.toPx()
            val gapDeg = 2.5f
            val r = (size.minDimension - stroke) / 2f
            var start = -90f
            segments.forEach { (name, value) ->
                val sweep = (value / total * 360f) * progress.value
                val inner = (sweep - gapDeg).coerceAtLeast(0.5f)
                drawArc(
                    color = colors(name),
                    startAngle = start + gapDeg / 2f,
                    sweepAngle = inner,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Butt)
                )
                start += sweep
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.Center)
        ) {
            Text(text = centerTitle, style = MoneyTypography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = centerValue, style = MoneyTypography.amountMedium)
        }
    }
}
