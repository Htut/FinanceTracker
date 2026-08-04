package com.financetracker.evolva.ui.report

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financetracker.evolva.R
import com.financetracker.evolva.data.calc.FinanceCalculator
import com.financetracker.evolva.data.export.DetailExport
import com.financetracker.evolva.data.export.DetailShareFormat
import com.financetracker.evolva.data.model.DateFilter
import com.financetracker.evolva.data.model.Transaction
import com.financetracker.evolva.data.model.TransactionType
import com.financetracker.evolva.data.model.filteredBy
import com.financetracker.evolva.data.model.formatAmount
import com.financetracker.evolva.ui.MainViewModel
import com.financetracker.evolva.ui.components.CategoryBarChart
import com.financetracker.evolva.ui.components.ChartTypeChips
import com.financetracker.evolva.ui.components.DonutChart
import com.financetracker.evolva.ui.components.DualLineChart
import com.financetracker.evolva.ui.components.IncomeExpenseBarChart
import com.financetracker.evolva.ui.components.SectionCard
import com.financetracker.evolva.ui.components.SeriesBarChart
import com.financetracker.evolva.ui.theme.FinanceColors
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private enum class ReportCategoryChartType { DONUT, BARS }
private enum class ReportTrendChartType { BARS, LINES }

@Composable
fun ReportScreen(viewModel: MainViewModel) {
    val transactions by viewModel.transactions.collectAsState()
    val currency by viewModel.currency.collectAsState()
    val context = LocalContext.current
    var month by remember { mutableStateOf(YearMonth.now()) }
    var showShareOptions by remember { mutableStateOf(false) }
    var categoryChartType by remember { mutableStateOf(ReportCategoryChartType.DONUT) }
    var trendChartType by remember { mutableStateOf(ReportTrendChartType.BARS) }

    fun fmt(v: Double) = formatAmount(v, currency)

    val monthTxs = remember(transactions, month) {
        transactions.filteredBy(DateFilter.Month(month))
            .sortedWith(
                compareByDescending<Transaction> { it.date }
                    .thenByDescending { it.time ?: java.time.LocalTime.MIN }
            )
    }
    val income = FinanceCalculator.sumFor(transactions, TransactionType.INCOME, month)
    val expense = FinanceCalculator.sumFor(transactions, TransactionType.EXPENSE, month)
    val savings = FinanceCalculator.sumFor(transactions, TransactionType.SAVINGS, month)
    val transferNet = FinanceCalculator.transferNetFor(transactions, month)
    val net = income - expense - savings + transferNet

    val topCategories = remember(monthTxs) {
        monthTxs
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .map { (category, rows) -> category to rows.sumOf { it.amount } }
            .sortedByDescending { it.second }
            .take(8)
    }

    val locale = LocalConfiguration.current.locales[0]
    val trendMonths = remember(month) { FinanceCalculator.lastNMonths(6, month) }
    val trendLabels = remember(trendMonths, locale) {
        trendMonths.map { it.month.getDisplayName(TextStyle.SHORT, locale) }
    }
    val incomeSeries = remember(transactions, trendMonths) {
        trendMonths.map { FinanceCalculator.sumFor(transactions, TransactionType.INCOME, it).toFloat() }
    }
    val expenseSeries = remember(transactions, trendMonths) {
        trendMonths.map { FinanceCalculator.sumFor(transactions, TransactionType.EXPENSE, it).toFloat() }
    }
    val breakdownLabels = listOf(
        stringResource(R.string.label_income),
        stringResource(R.string.label_expense),
        stringResource(R.string.label_savings)
    )
    val breakdownValues = listOf(income.toFloat(), expense.toFloat(), savings.toFloat())

    val monthLabel = remember(month) {
        month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
    }
    val title = stringResource(R.string.monthly_report)
    val subtitle = monthLabel
    val incomeLine = stringResource(R.string.report_line_income, fmt(income))
    val expenseLine = stringResource(R.string.report_line_expense, fmt(expense))
    val savingsLine = stringResource(R.string.report_line_savings, fmt(savings))
    val transferLine = stringResource(R.string.report_line_transfer, fmt(transferNet))
    val netLine = stringResource(R.string.report_line_net, fmt(net))
    val summaryLines = listOf(incomeLine, expenseLine, savingsLine, transferLine, netLine)

    fun saveBytes(format: DetailShareFormat, uri: android.net.Uri?) {
        if (uri == null) return
        val bytes = DetailExport.buildBytes(context, format, title, subtitle, monthTxs, currency, summaryLines)
        context.contentResolver.openOutputStream(uri)?.let { stream ->
            DetailExport.writeToStream(stream, bytes)
        }
    }

    val saveTextLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(DetailShareFormat.TEXT.mimeType)
    ) { uri -> saveBytes(DetailShareFormat.TEXT, uri) }
    val saveCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(DetailShareFormat.CSV.mimeType)
    ) { uri -> saveBytes(DetailShareFormat.CSV, uri) }
    val savePdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(DetailShareFormat.PDF.mimeType)
    ) { uri -> saveBytes(DetailShareFormat.PDF, uri) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                stringResource(R.string.report_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = FinanceColors.Text
            )
        }

        item {
            SectionCard(title = stringResource(R.string.month_label)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { month = month.minusMonths(1) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = stringResource(R.string.cd_previous_month),
                            tint = FinanceColors.Text
                        )
                    }
                    Text(
                        monthLabel,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FinanceColors.Text
                    )
                    IconButton(onClick = { month = month.plusMonths(1) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = stringResource(R.string.cd_next_month),
                            tint = FinanceColors.Text
                        )
                    }
                }
                if (month != YearMonth.now()) {
                    TextButton(onClick = { month = YearMonth.now() }) {
                        Text(stringResource(R.string.jump_to_this_month))
                    }
                }
            }
        }

        item {
            SectionCard(title = stringResource(R.string.report_summary)) {
                SummaryRow(stringResource(R.string.label_income), fmt(income), FinanceColors.Income)
                SummaryRow(stringResource(R.string.label_expense), fmt(expense), FinanceColors.Expense)
                SummaryRow(stringResource(R.string.label_savings), fmt(savings), FinanceColors.Text)
                SummaryRow(stringResource(R.string.label_transfer_net), fmt(transferNet), FinanceColors.Text)
                Spacer(modifier = Modifier.height(6.dp))
                SummaryRow(stringResource(R.string.label_net), fmt(net), FinanceColors.Accent)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    stringResource(R.string.tx_count, monthTxs.size),
                    fontSize = 12.sp,
                    color = FinanceColors.TextSoft
                )
            }
        }

        item {
            SectionCard(title = stringResource(R.string.report_month_breakdown)) {
                SeriesBarChart(
                    labels = breakdownLabels,
                    values = breakdownValues,
                    barColor = FinanceColors.Accent,
                    barColors = listOf(
                        FinanceColors.Income,
                        FinanceColors.Expense,
                        FinanceColors.Savings
                    )
                )
            }
        }

        item {
            SectionCard(title = stringResource(R.string.report_top_categories)) {
                if (topCategories.isEmpty()) {
                    Text(
                        stringResource(R.string.no_expenses_month),
                        fontSize = 13.sp,
                        color = FinanceColors.TextSoft
                    )
                } else {
                    ChartTypeChips(
                        options = listOf(
                            stringResource(R.string.chart_type_donut) to
                                (categoryChartType == ReportCategoryChartType.DONUT),
                            stringResource(R.string.chart_type_bars) to
                                (categoryChartType == ReportCategoryChartType.BARS)
                        ),
                        onSelect = {
                            categoryChartType =
                                if (it == 0) ReportCategoryChartType.DONUT else ReportCategoryChartType.BARS
                        }
                    )
                    when (categoryChartType) {
                        ReportCategoryChartType.DONUT ->
                            DonutChart(data = topCategories, valueFormatter = { fmt(it) })
                        ReportCategoryChartType.BARS ->
                            CategoryBarChart(data = topCategories, valueFormatter = { fmt(it) })
                    }
                }
            }
        }

        item {
            SectionCard(title = stringResource(R.string.report_income_vs_expense)) {
                ChartTypeChips(
                    options = listOf(
                        stringResource(R.string.chart_type_bars) to
                            (trendChartType == ReportTrendChartType.BARS),
                        stringResource(R.string.chart_type_lines) to
                            (trendChartType == ReportTrendChartType.LINES)
                    ),
                    onSelect = {
                        trendChartType =
                            if (it == 0) ReportTrendChartType.BARS else ReportTrendChartType.LINES
                    }
                )
                when (trendChartType) {
                    ReportTrendChartType.BARS ->
                        IncomeExpenseBarChart(
                            labels = trendLabels,
                            income = incomeSeries,
                            expense = expenseSeries,
                            avgIncome = null,
                            avgExpense = null
                        )
                    ReportTrendChartType.LINES ->
                        DualLineChart(
                            labels = trendLabels,
                            seriesA = incomeSeries,
                            seriesB = expenseSeries,
                            colorA = FinanceColors.Income,
                            colorB = FinanceColors.Expense
                        )
                }
            }
        }

        item {
            Button(
                onClick = { showShareOptions = true },
                enabled = monthTxs.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.share_or_save_report))
            }
        }
    }

    if (showShareOptions) {
        AlertDialog(
            onDismissRequest = { showShareOptions = false },
            title = { Text(stringResource(R.string.share_or_save)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        stringResource(R.string.report_share_help, monthLabel),
                        fontSize = 12.5.sp,
                        color = FinanceColors.TextSoft
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.share_via),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FinanceColors.Text
                    )
                    DetailShareFormat.entries.forEach { format ->
                        val formatLabel = when (format) {
                            DetailShareFormat.TEXT -> stringResource(R.string.format_text)
                            DetailShareFormat.CSV -> stringResource(R.string.format_csv)
                            DetailShareFormat.PDF -> stringResource(R.string.format_pdf)
                        }
                        TextButton(
                            onClick = {
                                showShareOptions = false
                                DetailExport.share(
                                    context, format, title, subtitle, monthTxs, currency, summaryLines
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(stringResource(R.string.share_format, formatLabel)) }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.save_to_device),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FinanceColors.Text
                    )
                    TextButton(
                        onClick = {
                            showShareOptions = false
                            saveTextLauncher.launch(DetailExport.fileName(title, DetailShareFormat.TEXT))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.save_text)) }
                    TextButton(
                        onClick = {
                            showShareOptions = false
                            saveCsvLauncher.launch(DetailExport.fileName(title, DetailShareFormat.CSV))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.save_csv)) }
                    TextButton(
                        onClick = {
                            showShareOptions = false
                            savePdfLauncher.launch(DetailExport.fileName(title, DetailShareFormat.PDF))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.save_pdf)) }
                }
            },
            confirmButton = {
                TextButton(onClick = { showShareOptions = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun SummaryRow(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.5.sp, color = FinanceColors.TextSoft)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}
