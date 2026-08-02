package com.financetracker.evolva.ui.transactions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financetracker.evolva.data.model.Transaction
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

    var sheetTransaction by remember { mutableStateOf<Transaction?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    var isNew by remember { mutableStateOf(true) }

    val sorted = remember(transactions) {
        transactions.sortedWith(
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
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
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
                        "No transactions in this date filter. Tap + to add one, or load a starter template from Settings.",
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
                    item { androidx.compose.foundation.layout.Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showSheet) {
        AddEditTransactionSheet(
            existing = sheetTransaction,
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
