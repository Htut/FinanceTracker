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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financetracker.evolva.data.calc.FinanceCalculator
import com.financetracker.evolva.data.calc.InsightsCalculator
import com.financetracker.evolva.data.model.TransactionType
import com.financetracker.evolva.data.model.formatAmount
import com.financetracker.evolva.ui.MainViewModel
import com.financetracker.evolva.ui.components.DonutChart
import com.financetracker.evolva.ui.components.IncomeExpenseBarChart
import com.financetracker.evolva.ui.components.LineChart
import com.financetracker.evolva.ui.components.SectionCard
import com.financetracker.evolva.ui.components.StatCard
import com.financetracker.evolva.ui.theme.FinanceColors
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val transactions by viewModel.transactions.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val recurringRules by viewModel.recurringRules.collectAsState()
    val currency by viewModel.currency.collectAsState()

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
            StatGrid(
                listOf(
                    Triple("Income", fmt(totals.income), FinanceColors.Income),
                    Triple("Expense", fmt(totals.expense), FinanceColors.Expense),
                    Triple("Savings", fmt(totals.savings), FinanceColors.Savings),
                    Triple("Transfers (net)", (if (totals.transferNet >= 0) "+" else "") + fmt(totals.transferNet), FinanceColors.Transfer),
                    Triple("Net Balance", fmt(totals.net), FinanceColors.Text)
                )
            )
        }

        item {
            Column {
                StatGrid(
                    listOf(
                        Triple("Avg Monthly Income", fmt(averages.income), FinanceColors.Income),
                        Triple("Avg Monthly Expense", fmt(averages.expense), FinanceColors.Expense),
                        Triple("Avg Monthly Savings", fmt(averages.savings), FinanceColors.Savings)
                    )
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

        item { Spacer(Modifier.height(70.dp)) }
    }
}

@Composable
private fun StatGrid(entries: List<Triple<String, String, Color>>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { (label, value, color) ->
                    StatCard(label = label, value = value, valueColor = color, modifier = Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
