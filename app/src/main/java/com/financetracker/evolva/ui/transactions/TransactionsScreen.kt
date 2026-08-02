package com.financetracker.evolva.ui.transactions

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financetracker.evolva.data.model.Categories
import com.financetracker.evolva.data.model.Transaction
import com.financetracker.evolva.data.model.TransactionQuery
import com.financetracker.evolva.data.model.TransactionType
import com.financetracker.evolva.data.model.filteredByQuery
import com.financetracker.evolva.data.model.formatAmount
import com.financetracker.evolva.ui.MainViewModel
import com.financetracker.evolva.ui.components.DateFilterBar
import com.financetracker.evolva.ui.components.TransactionRow
import com.financetracker.evolva.ui.theme.FinanceColors
import java.time.LocalTime

@Composable
fun TransactionsScreen(viewModel: MainViewModel) {
    val transactions by viewModel.filteredTransactions.collectAsState()
    val allCount by viewModel.transactions.collectAsState()
    val currency by viewModel.currency.collectAsState()
    val dateFilter by viewModel.dateFilter.collectAsState()
    val filterAutoCloseSeconds by viewModel.filterAutoCloseSeconds.collectAsState()
    val customExpenseCategories by viewModel.customExpenseCategories.collectAsState()

    var sheetTransaction by remember { mutableStateOf<Transaction?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    var isNew by remember { mutableStateOf(true) }

    var search by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf<TransactionType?>(null) }
    var categoryFilter by remember { mutableStateOf<String?>(null) }
    var minAmountText by remember { mutableStateOf("") }
    var maxAmountText by remember { mutableStateOf("") }
    var showAdvanced by remember { mutableStateOf(false) }

    val categoryOptions = remember(transactions, customExpenseCategories, typeFilter) {
        val fromData = transactions
            .filter { typeFilter == null || it.type == typeFilter }
            .map { it.category }
            .distinct()
            .sorted()
        if (fromData.isNotEmpty()) fromData
        else when (typeFilter) {
            TransactionType.EXPENSE -> Categories.expenseCategories(customExpenseCategories)
            TransactionType.INCOME -> Categories.income
            TransactionType.SAVINGS -> Categories.savings
            TransactionType.TRANSFER -> Categories.transfer
            null -> (Categories.expenseCategories(customExpenseCategories) + Categories.income +
                Categories.savings + Categories.transfer).distinct().sorted()
        }
    }

    val query = remember(search, typeFilter, categoryFilter, minAmountText, maxAmountText) {
        TransactionQuery(
            search = search,
            type = typeFilter,
            category = categoryFilter,
            minAmount = minAmountText.toDoubleOrNull(),
            maxAmount = maxAmountText.toDoubleOrNull()
        )
    }

    val sorted = remember(transactions, query) {
        transactions.filteredByQuery(query).sortedWith(
            compareByDescending<Transaction> { it.date }
                .thenByDescending { it.time ?: LocalTime.MIN }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                sheetTransaction = null
                isNew = true
                showSheet = true
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Add transaction")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            DateFilterBar(
                filter = dateFilter,
                onFilterChange = viewModel::setDateFilter,
                autoCloseSeconds = filterAutoCloseSeconds,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                singleLine = true,
                label = { Text("Search note, category…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (search.isNotEmpty()) {
                        IconButton(onClick = { search = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                        }
                    }
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = typeFilter == null,
                    onClick = { typeFilter = null; categoryFilter = null },
                    label = { Text("All types") }
                )
                TransactionType.entries.forEach { t ->
                    FilterChip(
                        selected = typeFilter == t,
                        onClick = {
                            typeFilter = if (typeFilter == t) null else t
                            categoryFilter = null
                        },
                        label = {
                            Text(t.name.lowercase().replaceFirstChar { c -> c.uppercase() })
                        }
                    )
                }
            }

            TextButton(
                onClick = { showAdvanced = !showAdvanced },
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(if (showAdvanced) "Hide amount & category filters" else "Amount & category filters")
            }

            if (showAdvanced) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = categoryFilter == null,
                        onClick = { categoryFilter = null },
                        label = { Text("All categories") }
                    )
                    categoryOptions.forEach { c ->
                        FilterChip(
                            selected = categoryFilter == c,
                            onClick = { categoryFilter = if (categoryFilter == c) null else c },
                            label = { Text(c) }
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = minAmountText,
                        onValueChange = { minAmountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Min amount") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = maxAmountText,
                        onValueChange = { maxAmountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Max amount") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (!query.isDefault) {
                TextButton(
                    onClick = {
                        search = ""
                        typeFilter = null
                        categoryFilter = null
                        minAmountText = ""
                        maxAmountText = ""
                    },
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) { Text("Clear filters") }
            }

            Text(
                "${sorted.size} of ${allCount.size} transactions",
                fontSize = 12.sp,
                color = FinanceColors.TextSoft,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            if (sorted.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (transactions.isEmpty()) {
                            "No transactions in this date filter. Tap + to add one, or load a starter template from Settings."
                        } else {
                            "No transactions match your search/filters."
                        },
                        color = FinanceColors.TextSoft,
                        fontSize = 13.5.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(sorted, key = { it.id }) { tx ->
                        TransactionRow(
                            transaction = tx,
                            formattedAmount = formatAmount(tx.amount, currency),
                            onClick = {
                                sheetTransaction = tx
                                isNew = false
                                showSheet = true
                            },
                            onDelete = { viewModel.deleteTransaction(tx.id) }
                        )
                        HorizontalDivider(color = FinanceColors.Border)
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showSheet) {
        AddEditTransactionSheet(
            existing = sheetTransaction,
            customExpenseCategories = customExpenseCategories,
            onAddExpenseCategory = viewModel::addCustomExpenseCategory,
            onDismiss = { showSheet = false },
            onSave = { tx, repeatMonthly ->
                if (isNew) viewModel.addTransaction(tx, repeatMonthly) else viewModel.updateTransaction(tx)
                showSheet = false
            },
            onStopRecurring = { ruleId ->
                viewModel.stopRecurring(ruleId)
                showSheet = false
            }
        )
    }
}
