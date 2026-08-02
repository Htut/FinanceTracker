package com.financetracker.evolva.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financetracker.evolva.data.calc.FinanceCalculator
import com.financetracker.evolva.data.calc.InsightsCalculator
import com.financetracker.evolva.data.model.Transaction
import com.financetracker.evolva.data.model.TransactionType
import com.financetracker.evolva.data.model.TransferDirection
import com.financetracker.evolva.data.model.formatAmount
import com.financetracker.evolva.data.model.formatRecordedAt
import com.financetracker.evolva.ui.MainViewModel
import com.financetracker.evolva.ui.components.DateFilterBar
import com.financetracker.evolva.ui.components.DonutChart
import com.financetracker.evolva.ui.components.IncomeExpenseBarChart
import com.financetracker.evolva.ui.components.LineChart
import com.financetracker.evolva.ui.components.SectionCard
import com.financetracker.evolva.ui.components.StatCard
import com.financetracker.evolva.ui.components.TypeTag
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
                onFilterChange = viewModel::setDateFilter
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
            currencyFormatter = { fmt(it) },
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
    currencyFormatter: (Double) -> String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(title)
                Text(subtitle, fontSize = 12.sp, color = FinanceColors.TextSoft)
            }
        },
        text = {
            if (transactions.isEmpty()) {
                Text("No transactions in this period.", color = FinanceColors.TextSoft)
            } else {
                LazyColumn(modifier = Modifier.height(360.dp)) {
                    items(transactions, key = { it.id }) { tx ->
                        val sign = when {
                            tx.type == TransactionType.EXPENSE -> "-"
                            tx.type == TransactionType.TRANSFER && tx.direction == TransferDirection.OUT -> "-"
                            else -> "+"
                        }
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(tx.formatRecordedAt(), fontSize = 11.5.sp, color = FinanceColors.TextSoft)
                            Spacer(modifier = Modifier.height(3.dp))
                            TypeTag(tx.type)
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(tx.category, fontSize = 13.sp, color = FinanceColors.Text)
                            Text(
                                "$sign ${currencyFormatter(tx.amount)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (!tx.note.isNullOrBlank()) {
                                Text(tx.note, fontSize = 12.sp, color = FinanceColors.TextSoft)
                            }
                        }
                        HorizontalDivider(color = FinanceColors.Border)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
