package com.financetracker.app.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financetracker.app.ui.theme.FinanceColors

/**
 * All charts here are hand-drawn with Compose's Canvas rather than a third
 * party charting library, so there's no extra dependency and no version
 * drift to worry about — same visual intent as the web app's Chart.js
 * doughnut/bar/line charts, just native.
 */

@Composable
fun DonutChart(
    data: List<Pair<String, Double>>,
    modifier: Modifier = Modifier,
    colors: List<Color> = FinanceColors.CategoryPalette,
    valueFormatter: (Double) -> String = { it.toString() }
) {
    val total = data.sumOf { it.second }
    Column(modifier) {
        if (data.isEmpty() || total <= 0.0) {
            Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                Text("No expense data yet", color = FinanceColors.TextSoft, fontSize = 13.sp)
            }
            return@Column
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .padding(12.dp)
        ) {
            val strokeWidth = size.minDimension * 0.24f
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            var startAngle = -90f
            data.forEachIndexed { index, (_, value) ->
                val sweep = (value / total * 360.0).toFloat()
                drawArc(
                    color = colors[index % colors.size],
                    startAngle = startAngle,
                    sweepAngle = sweep.coerceAtLeast(0.5f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(diameter, diameter),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                )
                startAngle += sweep
            }
        }
        Column(Modifier.padding(top = 4.dp)) {
            data.forEachIndexed { index, (label, value) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 3.dp)
                ) {
                    Dot(colors[index % colors.size])
                    Spacer(Modifier.width(8.dp))
                    Text(label, fontSize = 12.sp, color = FinanceColors.Text, modifier = Modifier.weight(1f))
                    Text(valueFormatter(value), fontSize = 12.sp, color = FinanceColors.TextSoft)
                }
            }
        }
    }
}

private fun axisPaint(): Paint = Paint().apply {
    color = FinanceColors.TextSoft.toArgb()
    textAlign = Paint.Align.CENTER
    isAntiAlias = true
}

@Composable
fun IncomeExpenseBarChart(
    labels: List<String>,
    income: List<Float>,
    expense: List<Float>,
    avgIncome: Float?,
    avgExpense: Float?,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxWidth().height(200.dp).padding(vertical = 8.dp)) {
        val n = labels.size
        if (n == 0) return@Canvas
        val bottomPad = 20.dp.toPx()
        val chartWidth = size.width
        val chartHeight = size.height - bottomPad
        val maxValue = (income + expense + listOfNotNull(avgIncome, avgExpense))
            .maxOrNull()?.coerceAtLeast(1f) ?: 1f

        fun yFor(v: Float): Float = chartHeight - (v / maxValue * chartHeight)

        val groupWidth = chartWidth / n
        val barWidth = groupWidth * 0.28f
        val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))

        avgIncome?.let {
            drawLine(
                FinanceColors.Income, Offset(0f, yFor(it)), Offset(chartWidth, yFor(it)),
                strokeWidth = 1.5.dp.toPx(), pathEffect = dash
            )
        }
        avgExpense?.let {
            drawLine(
                FinanceColors.Expense, Offset(0f, yFor(it)), Offset(chartWidth, yFor(it)),
                strokeWidth = 1.5.dp.toPx(), pathEffect = dash
            )
        }

        val textSizePx = 10.sp.toPx()
        for (i in 0 until n) {
            val groupLeft = i * groupWidth
            val incomeH = (chartHeight - yFor(income.getOrElse(i) { 0f })).coerceAtLeast(0f)
            drawRoundRect(
                color = FinanceColors.Income,
                topLeft = Offset(groupLeft + groupWidth * 0.12f, chartHeight - incomeH),
                size = Size(barWidth, incomeH),
                cornerRadius = CornerRadius(3.dp.toPx())
            )
            val expenseH = (chartHeight - yFor(expense.getOrElse(i) { 0f })).coerceAtLeast(0f)
            drawRoundRect(
                color = FinanceColors.Expense,
                topLeft = Offset(groupLeft + groupWidth * 0.54f, chartHeight - expenseH),
                size = Size(barWidth, expenseH),
                cornerRadius = CornerRadius(3.dp.toPx())
            )
            drawContext.canvas.nativeCanvas.drawText(
                labels[i],
                groupLeft + groupWidth / 2f,
                size.height - 4.dp.toPx(),
                axisPaint().apply { textSize = textSizePx }
            )
        }
    }
}

/**
 * A line/area chart where the segment from [dashedFromIndex] onward is
 * drawn dashed (used for "projected" cash flow) while everything before it
 * is solid. Pass dashedFromIndex = values.size (the default) for a fully
 * solid chart, e.g. the savings growth trend.
 */
@Composable
fun LineChart(
    labels: List<String>,
    values: List<Float>,
    modifier: Modifier = Modifier,
    dashedFromIndex: Int = values.size,
    lineColor: Color = FinanceColors.Savings,
    showZeroLine: Boolean = false
) {
    if (values.isEmpty()) {
        Box(modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            Text("Not enough data yet", color = FinanceColors.TextSoft, fontSize = 13.sp)
        }
        return
    }
    Canvas(modifier = modifier.fillMaxWidth().height(200.dp).padding(vertical = 8.dp)) {
        val bottomPad = 20.dp.toPx()
        val topPad = 12.dp.toPx()
        val chartWidth = size.width
        val chartHeight = size.height - bottomPad - topPad
        val n = values.size

        val minV = minOf(0f, values.min())
        val maxV = maxOf(values.max(), minV + 1f)
        val range = (maxV - minV).coerceAtLeast(1f)

        fun xFor(i: Int): Float = if (n <= 1) chartWidth / 2f else chartWidth * i / (n - 1).toFloat()
        fun yFor(v: Float): Float = topPad + chartHeight - ((v - minV) / range * chartHeight)

        if (showZeroLine && minV < 0f) {
            drawLine(FinanceColors.Border, Offset(0f, yFor(0f)), Offset(chartWidth, yFor(0f)), strokeWidth = 1.dp.toPx())
        }

        val areaPath = Path().apply {
            moveTo(xFor(0), yFor(values[0]))
            for (i in 1 until n) lineTo(xFor(i), yFor(values[i]))
            lineTo(xFor(n - 1), yFor(minV))
            lineTo(xFor(0), yFor(minV))
            close()
        }
        drawPath(areaPath, color = lineColor.copy(alpha = 0.08f))

        val solidEnd = dashedFromIndex.coerceIn(0, n - 1)
        if (solidEnd > 0) {
            val solidPath = Path().apply {
                moveTo(xFor(0), yFor(values[0]))
                for (i in 1..solidEnd) lineTo(xFor(i), yFor(values[i]))
            }
            drawPath(solidPath, color = lineColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        }
        if (dashedFromIndex < n - 1) {
            val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))
            val dashedPath = Path().apply {
                moveTo(xFor(dashedFromIndex), yFor(values[dashedFromIndex]))
                for (i in (dashedFromIndex + 1) until n) lineTo(xFor(i), yFor(values[i]))
            }
            drawPath(dashedPath, color = lineColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, pathEffect = dash))
        }

        values.forEachIndexed { i, v ->
            drawCircle(lineColor, radius = 2.5.dp.toPx(), center = Offset(xFor(i), yFor(v)))
        }

        val labelStep = maxOf(1, n / 6)
        val textSizePx = 10.sp.toPx()
        labels.forEachIndexed { i, label ->
            if (i % labelStep == 0 || i == n - 1) {
                drawContext.canvas.nativeCanvas.drawText(
                    label, xFor(i), size.height - 2.dp.toPx(),
                    axisPaint().apply { textSize = textSizePx }
                )
            }
        }
    }
}
