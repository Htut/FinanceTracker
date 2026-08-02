package com.financetracker.evolva.ui.budget

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.clickable
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financetracker.evolva.data.calc.BudgetState
import com.financetracker.evolva.data.calc.FinanceCalculator
import com.financetracker.evolva.data.calc.ForecastCalculator
import com.financetracker.evolva.data.model.Categories
import com.financetracker.evolva.data.model.formatAmount
import com.financetracker.evolva.ui.MainViewModel
import com.financetracker.evolva.ui.components.BudgetProgressBar
import com.financetracker.evolva.ui.components.DateFilterBar
import com.financetracker.evolva.ui.components.LineChart
import com.financetracker.evolva.ui.components.SectionCard
import com.financetracker.evolva.ui.theme.FinanceColors
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(viewModel: MainViewModel) {
    val transactions by viewModel.filteredTransactions.collectAsState()
    val allTransactions by viewModel.transactions.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val currency by viewModel.currency.collectAsState()
    val forecastHorizon by viewModel.forecastHorizon.collectAsState()
    val dateFilter by viewModel.dateFilter.collectAsState()
    val filterAutoCloseSeconds by viewModel.filterAutoCloseSeconds.collectAsState()
    val customExpenseCategories by viewModel.customExpenseCategories.collectAsState()

    fun fmt(v: Double) = formatAmount(v, currency)

    val currentMonth = remember { YearMonth.now() }
    val budgetable = remember(customExpenseCategories) { Categories.budgetable(customExpenseCategories) }
    val budgetRows = remember(transactions, budgets, currentMonth) {
        budgets.map { b ->
            val spent = FinanceCalculator.spendForCategoryMonth(transactions, b.category, currentMonth)
            Triple(b, spent, FinanceCalculator.budgetState(spent, b.limit))
        }.sortedByDescending { (_, spent, _) -> spent }
    }

    // Forecast still uses the full history so projections stay meaningful.
    val forecast = remember(allTransactions, forecastHorizon) {
        ForecastCalculator.compute(allTransactions, forecastHorizon)
    }
    val forecastLabels = remember(forecast) {
        (forecast.pastMonths + forecast.futurePoints.map { it.month })
            .map { it.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
    }
    val forecastValues = remember(forecast) {
        (forecast.pastCumulative + forecast.futurePoints.map { it.balance }).map { it.toFloat() }
    }

    var newCategory by remember(budgets, budgetable) {
        mutableStateOf(budgetable.firstOrNull { c -> budgets.none { it.category == c } } ?: budgetable.first())
    }
    var newLimitText by remember { mutableStateOf("") }
    var categoryMenuExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text("Budget", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = FinanceColors.Text)
                Text("Limits for ${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} and where cash flow is headed", fontSize = 13.sp, color = FinanceColors.TextSoft)
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
            SectionCard(title = "Category Budgets (this month)") {
                if (budgetRows.isEmpty()) {
                    Text(
                        "No budgets set yet. Pick a category below, or load the \"Budget example\" starter template from Settings.",
                        fontSize = 13.sp,
                        color = FinanceColors.TextSoft
                    )
                } else {
                    budgetRows.forEach { (budget, spent, state) ->
                        val color = when (state) {
                            BudgetState.OVER -> FinanceColors.Expense
                            BudgetState.WARN -> FinanceColors.Warn
                            else -> FinanceColors.Income
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    newCategory = budget.category
                                    newLimitText = if (budget.limit == budget.limit.toLong().toDouble()) {
                                        budget.limit.toLong().toString()
                                    } else {
                                        budget.limit.toString()
                                    }
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(budget.category, fontSize = 13.5.sp, color = FinanceColors.Text)
                                    Text(
                                        "${fmt(spent)} / ${fmt(budget.limit)} · Tap to edit",
                                        fontSize = 12.sp,
                                        color = FinanceColors.TextSoft
                                    )
                                }
                                TextButton(onClick = { viewModel.deleteBudget(budget.category) }) {
                                    Text("Delete", color = FinanceColors.Expense)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            BudgetProgressBar(
                                progress = if (budget.limit > 0) (spent / budget.limit).toFloat() else 0f,
                                color = color
                            )
                            if (state == BudgetState.OVER) {
                                Text(
                                    "Over budget",
                                    fontSize = 11.5.sp,
                                    color = FinanceColors.Expense,
                                    modifier = Modifier.padding(top = 3.dp)
                                )
                            } else if (state == BudgetState.WARN) {
                                Text(
                                    "Approaching limit",
                                    fontSize = 11.5.sp,
                                    color = FinanceColors.Warn,
                                    modifier = Modifier.padding(top = 3.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Set or edit a limit", fontSize = 12.sp, color = FinanceColors.TextSoft)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = categoryMenuExpanded,
                        onExpandedChange = { categoryMenuExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = newCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = categoryMenuExpanded,
                            onDismissRequest = { categoryMenuExpanded = false }
                        ) {
                            budgetable.forEach { c ->
                                DropdownMenuItem(text = { Text(c) }, onClick = {
                                    newCategory = c
                                    newLimitText = budgets.find { it.category == c }?.limit?.let { limit ->
                                        if (limit == limit.toLong().toDouble()) limit.toLong().toString() else limit.toString()
                                    } ?: ""
                                    categoryMenuExpanded = false
                                })
                            }
                        }
                    }
                    OutlinedTextField(
                        value = newLimitText,
                        onValueChange = { input -> newLimitText = input.filter { it.isDigit() || it == '.' } },
                        label = { Text("Limit") },
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                        modifier = Modifier.width(120.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        val limit = newLimitText.toDoubleOrNull()
                        if (limit != null && limit > 0) {
                            viewModel.setBudget(newCategory, limit)
                            newLimitText = ""
                        }
                    },
                    enabled = newLimitText.toDoubleOrNull()?.let { it > 0 } ?: false
                ) { Text("Save Limit") }
            }
        }

        item {
            SectionCard(title = "Cash Flow Forecast") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(3, 6, 12).forEach { months ->
                        FilterChip(
                            selected = forecastHorizon == months,
                            onClick = { viewModel.setForecastHorizon(months) },
                            label = { Text("$months mo") }
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                LineChart(
                    labels = forecastLabels,
                    values = forecastValues,
                    dashedFromIndex = (forecast.pastMonths.size - 1).coerceAtLeast(0),
                    lineColor = FinanceColors.Savings,
                    showZeroLine = true
                )

                forecast.firstNegativeMonth?.let { month ->
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(FinanceColors.Expense.copy(alpha = 0.1f))
                            .padding(12.dp)
                    ) {
                        Text(
                            "Projected balance goes negative around ${month.format(DateTimeFormatter.ofPattern("MMM yyyy"))}, at current average spending.",
                            fontSize = 12.5.sp,
                            color = FinanceColors.Expense
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text("Month-by-month projection", fontSize = 12.sp, color = FinanceColors.TextSoft)
                Spacer(Modifier.height(6.dp))
                forecast.futurePoints.forEach { point ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(point.month.format(DateTimeFormatter.ofPattern("MMM yyyy")), fontSize = 12.5.sp, color = FinanceColors.Text)
                        Text(
                            fmt(point.balance),
                            fontSize = 12.5.sp,
                            color = if (point.balance < 0) FinanceColors.Expense else FinanceColors.Text
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}
