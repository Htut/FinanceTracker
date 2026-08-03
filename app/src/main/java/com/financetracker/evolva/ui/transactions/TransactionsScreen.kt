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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financetracker.evolva.R
import com.financetracker.evolva.data.locale.CategoryLabels
import com.financetracker.evolva.data.model.Categories
import com.financetracker.evolva.data.model.Transaction
import com.financetracker.evolva.data.model.TransactionQuery
import com.financetracker.evolva.data.model.TransactionType
import com.financetracker.evolva.data.model.AppCurrency
import com.financetracker.evolva.data.model.filteredByQuery
import com.financetracker.evolva.data.model.formatAmount
import com.financetracker.evolva.data.model.homeAmount
import com.financetracker.evolva.ui.MainViewModel
import com.financetracker.evolva.ui.UndoAction
import com.financetracker.evolva.ui.components.DateFilterBar
import com.financetracker.evolva.ui.components.TransactionRow
import com.financetracker.evolva.ui.theme.FinanceColors
import java.time.LocalTime

private enum class ActivitySort {
    DATE_DESC,
    DATE_ASC,
    AMOUNT_DESC,
    AMOUNT_ASC
}

@Composable
fun TransactionsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val transactions by viewModel.filteredTransactions.collectAsState()
    val allCount by viewModel.transactions.collectAsState()
    val currency by viewModel.currency.collectAsState()
    val dateFilter by viewModel.dateFilter.collectAsState()
    val filterAutoCloseSeconds by viewModel.filterAutoCloseSeconds.collectAsState()
    val customExpenseCategories by viewModel.customExpenseCategories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val exchangeRates by viewModel.exchangeRates.collectAsState()
    val undoAction by viewModel.undoAction.collectAsState()
    val viewOnly by viewModel.viewOnlyMode.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(undoAction) {
        if (undoAction is UndoAction.DeleteTransaction) {
            val result = snackbarHostState.showSnackbar(
                message = context.getString(R.string.transaction_deleted),
                actionLabel = context.getString(R.string.action_undo)
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.undoLastAction()
            else viewModel.dismissUndo()
        }
    }

    var sheetTransaction by remember { mutableStateOf<Transaction?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    var isNew by remember { mutableStateOf(true) }
    var pendingDelete by remember { mutableStateOf<Transaction?>(null) }

    var search by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf<TransactionType?>(null) }
    var categoryFilter by remember { mutableStateOf<String?>(null) }
    var minAmountText by remember { mutableStateOf("") }
    var maxAmountText by remember { mutableStateOf("") }
    var showAdvanced by remember { mutableStateOf(false) }
    var sort by remember { mutableStateOf(ActivitySort.DATE_DESC) }

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

    val sorted = remember(transactions, query, sort) {
        val filtered = transactions.filteredByQuery(query)
        when (sort) {
            ActivitySort.DATE_DESC -> filtered.sortedWith(
                compareByDescending<Transaction> { it.date }
                    .thenByDescending { it.time ?: LocalTime.MIN }
            )
            ActivitySort.DATE_ASC -> filtered.sortedWith(
                compareBy<Transaction> { it.date }
                    .thenBy { it.time ?: LocalTime.MIN }
            )
            ActivitySort.AMOUNT_DESC -> filtered.sortedWith(
                compareByDescending<Transaction> { it.homeAmount() }
                    .thenByDescending { it.date }
            )
            ActivitySort.AMOUNT_ASC -> filtered.sortedWith(
                compareBy<Transaction> { it.homeAmount() }
                    .thenByDescending { it.date }
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!viewOnly) {
                FloatingActionButton(onClick = {
                    sheetTransaction = null
                    isNew = true
                    showSheet = true
                }) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_transaction))
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (viewOnly) {
                Text(
                    stringResource(R.string.view_only_banner),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = FinanceColors.TextSoft,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
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
                label = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (search.isNotEmpty()) {
                        IconButton(onClick = { search = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.cd_clear_search))
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
                    label = { Text(stringResource(R.string.all_types)) }
                )
                TransactionType.entries.forEach { t ->
                    FilterChip(
                        selected = typeFilter == t,
                        onClick = {
                            typeFilter = if (typeFilter == t) null else t
                            categoryFilter = null
                        },
                        label = {
                            Text(CategoryLabels.typeLabel(context, t))
                        }
                    )
                }
            }

            Text(
                stringResource(R.string.sort_label),
                fontSize = 12.sp,
                color = FinanceColors.TextSoft,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = sort == ActivitySort.DATE_DESC,
                    onClick = { sort = ActivitySort.DATE_DESC },
                    label = { Text(stringResource(R.string.sort_date_newest)) }
                )
                FilterChip(
                    selected = sort == ActivitySort.DATE_ASC,
                    onClick = { sort = ActivitySort.DATE_ASC },
                    label = { Text(stringResource(R.string.sort_date_oldest)) }
                )
                FilterChip(
                    selected = sort == ActivitySort.AMOUNT_DESC,
                    onClick = { sort = ActivitySort.AMOUNT_DESC },
                    label = { Text(stringResource(R.string.sort_amount_high)) }
                )
                FilterChip(
                    selected = sort == ActivitySort.AMOUNT_ASC,
                    onClick = { sort = ActivitySort.AMOUNT_ASC },
                    label = { Text(stringResource(R.string.sort_amount_low)) }
                )
            }

            TextButton(
                onClick = { showAdvanced = !showAdvanced },
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    stringResource(
                        if (showAdvanced) R.string.hide_advanced_filters else R.string.show_advanced_filters
                    )
                )
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
                        label = { Text(stringResource(R.string.all_categories)) }
                    )
                    categoryOptions.forEach { c ->
                        FilterChip(
                            selected = categoryFilter == c,
                            onClick = { categoryFilter = if (categoryFilter == c) null else c },
                            label = { Text(CategoryLabels.display(context, c)) }
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
                        label = { Text(stringResource(R.string.min_amount)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = maxAmountText,
                        onValueChange = { maxAmountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text(stringResource(R.string.max_amount)) },
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
                ) { Text(stringResource(R.string.clear_filters)) }
            }

            Text(
                stringResource(R.string.transactions_count_of, sorted.size, allCount.size),
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
                        stringResource(
                            if (transactions.isEmpty()) R.string.no_transactions_filter
                            else R.string.no_transactions_match
                        ),
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
                        val txCurrency = AppCurrency.fromCode(tx.currencyCode ?: currency.code)
                        val amountLabel = buildString {
                            append(formatAmount(tx.amount, txCurrency))
                            if (txCurrency != currency) {
                                append(" (")
                                append(formatAmount(tx.homeAmount(), currency))
                                append(")")
                            }
                        }
                        TransactionRow(
                            transaction = tx,
                            formattedAmount = amountLabel,
                            readOnly = viewOnly,
                            onClick = {
                                if (!tx.locked) {
                                    sheetTransaction = tx
                                    isNew = false
                                    showSheet = true
                                }
                            },
                            onDelete = {
                                if (!tx.locked) pendingDelete = tx
                            },
                            onToggleLock = {
                                viewModel.setTransactionLocked(tx.id, !tx.locked)
                            }
                        )
                        HorizontalDivider(color = FinanceColors.Border)
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    pendingDelete?.let { tx ->
        val label = CategoryLabels.display(context, tx.category)
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.delete_transaction_title)) },
            text = {
                Text(stringResource(R.string.delete_transaction_message, label))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransaction(tx.id, context)
                        pendingDelete = null
                    }
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showSheet && !viewOnly) {
        AddEditTransactionSheet(
            existing = sheetTransaction,
            accounts = accounts,
            homeCurrency = currency,
            exchangeRates = exchangeRates,
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
