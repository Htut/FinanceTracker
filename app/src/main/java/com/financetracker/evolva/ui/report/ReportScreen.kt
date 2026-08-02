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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financetracker.evolva.data.calc.FinanceCalculator
import com.financetracker.evolva.data.export.DetailExport
import com.financetracker.evolva.data.export.DetailShareFormat
import com.financetracker.evolva.data.model.DateFilter
import com.financetracker.evolva.data.model.Transaction
import com.financetracker.evolva.data.model.TransactionType
import com.financetracker.evolva.data.model.filteredBy
import com.financetracker.evolva.data.model.formatAmount
import com.financetracker.evolva.ui.MainViewModel
import com.financetracker.evolva.ui.components.SectionCard
import com.financetracker.evolva.ui.theme.FinanceColors
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ReportScreen(viewModel: MainViewModel) {
    val transactions by viewModel.transactions.collectAsState()
    val currency by viewModel.currency.collectAsState()
    val context = LocalContext.current
    var month by remember { mutableStateOf(YearMonth.now()) }
    var showShareOptions by remember { mutableStateOf(false) }

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

    val monthLabel = remember(month) {
        month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
    }
    val title = "Monthly report"
    val subtitle = monthLabel
    val summaryLines = remember(income, expense, savings, transferNet, net, currency) {
        listOf(
            "Income: ${fmt(income)}",
            "Expense: ${fmt(expense)}",
            "Savings: ${fmt(savings)}",
            "Transfer net: ${fmt(transferNet)}",
            "Net: ${fmt(net)}"
        )
    }

    fun saveBytes(format: DetailShareFormat, uri: android.net.Uri?) {
        if (uri == null) return
        val bytes = DetailExport.buildBytes(format, title, subtitle, monthTxs, currency, summaryLines)
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
                "Report",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = FinanceColors.Text
            )
        }

        item {
            SectionCard(title = "Month") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { month = month.minusMonths(1) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous month",
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
                            contentDescription = "Next month",
                            tint = FinanceColors.Text
                        )
                    }
                }
                if (month != YearMonth.now()) {
                    TextButton(onClick = { month = YearMonth.now() }) {
                        Text("Jump to this month")
                    }
                }
            }
        }

        item {
            SectionCard(title = "Summary") {
                SummaryRow("Income", fmt(income), FinanceColors.Income)
                SummaryRow("Expense", fmt(expense), FinanceColors.Expense)
                SummaryRow("Savings", fmt(savings), FinanceColors.Text)
                SummaryRow("Transfer net", fmt(transferNet), FinanceColors.Text)
                Spacer(modifier = Modifier.height(6.dp))
                SummaryRow("Net", fmt(net), FinanceColors.Accent)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${monthTxs.size} transaction(s)",
                    fontSize = 12.sp,
                    color = FinanceColors.TextSoft
                )
            }
        }

        item {
            SectionCard(title = "Top expense categories") {
                if (topCategories.isEmpty()) {
                    Text(
                        "No expenses in this month.",
                        fontSize = 13.sp,
                        color = FinanceColors.TextSoft
                    )
                } else {
                    topCategories.forEach { (category, amount) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(category, fontSize = 13.5.sp, color = FinanceColors.Text)
                            Text(fmt(amount), fontSize = 13.5.sp, color = FinanceColors.Expense)
                        }
                    }
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
                Text("Share or save report")
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
                        "Includes the month summary header and all transactions for $monthLabel.",
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
                                    context, format, title, subtitle, monthTxs, currency, summaryLines
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
