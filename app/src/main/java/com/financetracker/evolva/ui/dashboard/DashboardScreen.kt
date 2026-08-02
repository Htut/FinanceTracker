package com.financetracker.evolva.ui.dashboard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.financetracker.evolva.data.calc.FinanceCalculator
import com.financetracker.evolva.data.calc.InsightsCalculator
import com.financetracker.evolva.data.export.DetailExport
import com.financetracker.evolva.data.export.DetailShareFormat
import com.financetracker.evolva.data.model.AppCurrency
import com.financetracker.evolva.data.model.Transaction
import com.financetracker.evolva.data.model.TransactionType
import com.financetracker.evolva.data.model.TransferDirection
import com.financetracker.evolva.data.model.formatAmount
import com.financetracker.evolva.data.model.formatAmountNumber
import com.financetracker.evolva.data.model.formatRecordedAt
import com.financetracker.evolva.ui.MainViewModel
import com.financetracker.evolva.ui.components.DateFilterBar
import com.financetracker.evolva.ui.components.DonutChart
import com.financetracker.evolva.ui.components.IncomeExpenseBarChart
import com.financetracker.evolva.ui.components.LineChart
import com.financetracker.evolva.ui.components.SectionCard
import com.financetracker.evolva.ui.components.StatCard
import com.financetracker.evolva.ui.theme.FinanceColors
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private enum class DashboardMetric {
    INCOME, EXPENSE, SAVINGS, TRANSFER_NET, NET, AVG_INCOME, AVG_EXPENSE, AVG_SAVINGS
}

@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val transactions by viewModel.filteredTransactions.collectAsState()
    val allTransactions by viewModel.transactions.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val recurringRules by viewModel.recurringRules.collectAsState()
    val currency by viewModel.currency.collectAsState()
    val dateFilter by viewModel.dateFilter.collectAsState()
    val filterAutoCloseSeconds by viewModel.filterAutoCloseSeconds.collectAsState()

    fun fmt(v: Double) = formatAmount(v, currency)

    val totals = remember(transactions) { FinanceCalculator.totals(transactions) }
    val averages = remember(transactions) { FinanceCalculator.averages(totals, transactions) }

    val expenseByCategory = remember(transactions) {
        transactions.filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    val last6Months = remember(transactions) { FinanceCalculator.lastNMonths(6) }
    val monthLabels = remember(last6Months) {
        last6Months.map { it.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
    }
    val incomeSeries = remember(transactions, last6Months) {
        last6Months.map { FinanceCalculator.sumFor(transactions, TransactionType.INCOME, it).toFloat() }
    }
    val expenseSeries = remember(transactions, last6Months) {
        last6Months.map { FinanceCalculator.sumFor(transactions, TransactionType.EXPENSE, it).toFloat() }
    }
    val savingsTrend = remember(transactions, last6Months) {
        var cumulative = transactions
            .filter { it.type == TransactionType.SAVINGS && YearMonth.from(it.date) < last6Months.first() }
            .sumOf { it.amount }
        last6Months.map { m ->
            cumulative += FinanceCalculator.sumFor(transactions, TransactionType.SAVINGS, m)
            cumulative.toFloat()
        }
    }

    val insights = remember(transactions, budgets, recurringRules, currency) {
        InsightsCalculator.build(transactions, budgets, recurringRules) { v -> formatAmount(v, currency) }
    }

    var detailMetric by remember { mutableStateOf<DashboardMetric?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text("Finance Tracker", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = FinanceColors.Text)
                Text("Your money, at a glance", fontSize = 13.sp, color = FinanceColors.TextSoft)
            }
        }

        item {
            DateFilterBar(
                filter = dateFilter,
                onFilterChange = viewModel::setDateFilter,
                autoCloseSeconds = filterAutoCloseSeconds
            )
        }

        item {
            Text(
                "${transactions.size} of ${allTransactions.size} transactions in view",
                fontSize = 12.sp,
                color = FinanceColors.TextSoft
            )
        }

        item {
            ResponsiveStatGrid(
                entries = listOf(
                    StatEntry("Income", fmt(totals.income), FinanceColors.Income, DashboardMetric.INCOME),
                    StatEntry("Expense", fmt(totals.expense), FinanceColors.Expense, DashboardMetric.EXPENSE),
                    StatEntry("Savings", fmt(totals.savings), FinanceColors.Savings, DashboardMetric.SAVINGS),
                    StatEntry(
                        "Transfers (net)",
                        (if (totals.transferNet >= 0) "+" else "") + fmt(totals.transferNet),
                        FinanceColors.Transfer,
                        DashboardMetric.TRANSFER_NET
                    ),
                    StatEntry("Net Balance", fmt(totals.net), FinanceColors.Text, DashboardMetric.NET)
                ),
                onCardClick = { detailMetric = it }
            )
        }

        item {
            Column {
                ResponsiveStatGrid(
                    entries = listOf(
                        StatEntry("Avg Monthly Income", fmt(averages.income), FinanceColors.Income, DashboardMetric.AVG_INCOME),
                        StatEntry("Avg Monthly Expense", fmt(averages.expense), FinanceColors.Expense, DashboardMetric.AVG_EXPENSE),
                        StatEntry("Avg Monthly Savings", fmt(averages.savings), FinanceColors.Savings, DashboardMetric.AVG_SAVINGS)
                    ),
                    onCardClick = { detailMetric = it }
                )
                if (averages.months > 0) {
                    Text(
                        "Averaged across ${averages.months} month${if (averages.months == 1) "" else "s"} with activity.",
                        fontSize = 11.5.sp, color = FinanceColors.TextSoft, modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }

        item {
            SectionCard(title = "Expenses by Category") {
                DonutChart(data = expenseByCategory, valueFormatter = { fmt(it) })
            }
        }

        item {
            SectionCard(title = "Income vs Expense (6 mo, dashed = average)") {
                IncomeExpenseBarChart(
                    labels = monthLabels,
                    income = incomeSeries,
                    expense = expenseSeries,
                    avgIncome = if (averages.months > 0) averages.income.toFloat() else null,
                    avgExpense = if (averages.months > 0) averages.expense.toFloat() else null
                )
            }
        }

        item {
            SectionCard(title = "Savings Growth") {
                LineChart(labels = monthLabels, values = savingsTrend, lineColor = FinanceColors.Savings)
            }
        }

        item {
            SectionCard(title = "Insights") {
                insights.forEach { text ->
                    Row(modifier = Modifier.padding(vertical = 6.dp)) {
                        Text("•  ", color = FinanceColors.Text)
                        Text(text, fontSize = 13.5.sp, color = FinanceColors.Text)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(70.dp)) }
    }

    detailMetric?.let { metric ->
        val detailTxs = remember(metric, transactions) { transactionsForMetric(metric, transactions) }
        val title = when (metric) {
            DashboardMetric.INCOME -> "Income details"
            DashboardMetric.EXPENSE -> "Expense details"
            DashboardMetric.SAVINGS -> "Savings details"
            DashboardMetric.TRANSFER_NET -> "Transfer details"
            DashboardMetric.NET -> "All transactions in period"
            DashboardMetric.AVG_INCOME -> "Income (for average)"
            DashboardMetric.AVG_EXPENSE -> "Expense (for average)"
            DashboardMetric.AVG_SAVINGS -> "Savings (for average)"
        }
        MetricDetailDialog(
            title = title,
            subtitle = dateFilter.label(),
            transactions = detailTxs,
            currency = currency,
            onDismiss = { detailMetric = null }
        )
    }
}

private data class StatEntry(
    val label: String,
    val value: String,
    val color: Color,
    val metric: DashboardMetric
)

@Composable
private fun ResponsiveStatGrid(
    entries: List<StatEntry>,
    onCardClick: (DashboardMetric) -> Unit
) {
    val widthDp = LocalConfiguration.current.screenWidthDp
    val columns = when {
        widthDp >= 900 -> 4
        widthDp >= 600 -> 3
        else -> 2
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        entries.chunked(columns).forEach { rowEntries ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowEntries.forEach { entry ->
                    StatCard(
                        label = entry.label,
                        value = entry.value,
                        valueColor = entry.color,
                        modifier = Modifier.weight(1f),
                        onClick = { onCardClick(entry.metric) }
                    )
                }
                repeat(columns - rowEntries.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private fun transactionsForMetric(metric: DashboardMetric, transactions: List<Transaction>): List<Transaction> {
    val filtered = when (metric) {
        DashboardMetric.INCOME, DashboardMetric.AVG_INCOME ->
            transactions.filter { it.type == TransactionType.INCOME }
        DashboardMetric.EXPENSE, DashboardMetric.AVG_EXPENSE ->
            transactions.filter { it.type == TransactionType.EXPENSE }
        DashboardMetric.SAVINGS, DashboardMetric.AVG_SAVINGS ->
            transactions.filter { it.type == TransactionType.SAVINGS }
        DashboardMetric.TRANSFER_NET ->
            transactions.filter { it.type == TransactionType.TRANSFER }
        DashboardMetric.NET -> transactions
    }
    return filtered.sortedWith(
        compareByDescending<Transaction> { it.date }
            .thenByDescending { it.time ?: java.time.LocalTime.MIN }
    )
}

@Composable
private fun MetricDetailDialog(
    title: String,
    subtitle: String,
    transactions: List<Transaction>,
    currency: AppCurrency,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showShareOptions by remember { mutableStateOf(false) }

    fun saveBytes(format: DetailShareFormat, uri: android.net.Uri?) {
        if (uri == null) return
        val bytes = DetailExport.buildBytes(format, title, subtitle, transactions, currency)
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            color = FinanceColors.Background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FinanceColors.Header)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close",
                                tint = FinanceColors.OnHeader
                            )
                        }
                        Column(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
                            Text(
                                title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = FinanceColors.OnHeader,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "Filter: $subtitle",
                                fontSize = 12.sp,
                                color = FinanceColors.OnHeader.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "Currency: ${currency.symbol} (${currency.code}) · ${transactions.size} transaction(s)",
                                fontSize = 11.sp,
                                color = FinanceColors.OnHeader.copy(alpha = 0.75f),
                                modifier = Modifier.padding(top = 2.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = { showShareOptions = true },
                            enabled = transactions.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = "Share all",
                                tint = FinanceColors.OnHeader
                            )
                        }
                    }
                    HorizontalDivider(color = FinanceColors.Accent.copy(alpha = 0.45f), thickness = 2.dp)
                }

                if (transactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(FinanceColors.BandEven)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No transactions in this period.", color = FinanceColors.TextSoft)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        items(transactions.size, key = { transactions[it].id }) { index ->
                            CompactDetailRow(
                                tx = transactions[index],
                                currency = currency,
                                bandColor = if (index % 2 == 0) FinanceColors.BandEven else FinanceColors.BandOdd
                            )
                        }
                    }
                }
            }
        }
    }

    if (showShareOptions) {
        AlertDialog(
            onDismissRequest = { showShareOptions = false },
            title = { Text("Share or save") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Opens the Android share sheet so you can send to any app.",
                        fontSize = 12.5.sp,
                        color = FinanceColors.TextSoft
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Share via",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FinanceColors.Text
                    )
                    DetailShareFormat.entries.forEach { format ->
                        TextButton(
                            onClick = {
                                showShareOptions = false
                                DetailExport.share(
                                    context, format, title, subtitle, transactions, currency
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Share ${format.label}") }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Save to device",
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
                    ) { Text("Save Text") }
                    TextButton(
                        onClick = {
                            showShareOptions = false
                            saveCsvLauncher.launch(DetailExport.fileName(title, DetailShareFormat.CSV))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Save CSV") }
                    TextButton(
                        onClick = {
                            showShareOptions = false
                            savePdfLauncher.launch(DetailExport.fileName(title, DetailShareFormat.PDF))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Save PDF") }
                }
            },
            confirmButton = {
                TextButton(onClick = { showShareOptions = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun CompactDetailRow(
    tx: Transaction,
    currency: AppCurrency,
    bandColor: Color
) {
    val sign = when {
        tx.type == TransactionType.EXPENSE -> "-"
        tx.type == TransactionType.TRANSFER && tx.direction == TransferDirection.OUT -> "-"
        else -> "+"
    }
    val note = tx.note?.takeIf { it.isNotBlank() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bandColor)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${tx.formatRecordedAt()} · ${tx.category}",
                fontSize = 13.sp,
                color = FinanceColors.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            )
            Text(
                text = "$sign ${formatAmountNumber(tx.amount, currency)}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = FinanceColors.Text
            )
        }
        if (note != null) {
            Text(
                note,
                fontSize = 12.sp,
                color = FinanceColors.TextSoft,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
