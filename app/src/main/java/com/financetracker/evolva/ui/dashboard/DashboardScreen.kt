package com.financetracker.evolva.ui.dashboard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.financetracker.evolva.R
import com.financetracker.evolva.data.calc.FinanceCalculator
import com.financetracker.evolva.data.calc.InsightsCalculator
import com.financetracker.evolva.data.export.DetailExport
import com.financetracker.evolva.data.export.DetailShareFormat
import com.financetracker.evolva.data.locale.CategoryLabels
import com.financetracker.evolva.data.model.AppCurrency
import com.financetracker.evolva.data.model.Transaction
import com.financetracker.evolva.data.model.TransactionSort
import com.financetracker.evolva.data.model.TransactionType
import com.financetracker.evolva.data.model.TransferDirection
import com.financetracker.evolva.data.model.formatAmount
import com.financetracker.evolva.data.model.formatAmountNumber
import com.financetracker.evolva.data.model.formatRecordedAt
import com.financetracker.evolva.data.model.applySort
import com.financetracker.evolva.ui.MainViewModel
import com.financetracker.evolva.ui.components.DateFilterBar
import com.financetracker.evolva.ui.components.DonutChart
import com.financetracker.evolva.ui.components.IncomeExpenseBarChart
import com.financetracker.evolva.ui.components.LineChart
import com.financetracker.evolva.ui.components.SectionCard
import com.financetracker.evolva.ui.components.StatCard
import com.financetracker.evolva.ui.components.TransactionSortChips
import com.financetracker.evolva.ui.theme.FinanceColors
import java.time.YearMonth
import java.time.format.TextStyle

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
    val locale = LocalConfiguration.current.locales[0]
    val monthLabels = remember(last6Months, locale) {
        last6Months.map { it.month.getDisplayName(TextStyle.SHORT, locale) }
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

    val localeContext = LocalContext.current
    val insights = remember(transactions, budgets, recurringRules, currency, localeContext) {
        InsightsCalculator.build(
            localeContext,
            transactions,
            budgets,
            recurringRules
        ) { v -> formatAmount(v, currency) }
    }

    var detailMetric by remember { mutableStateOf<DashboardMetric?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        DashboardBrandHeader()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        item {
            DateFilterBar(
                filter = dateFilter,
                onFilterChange = viewModel::setDateFilter,
                autoCloseSeconds = filterAutoCloseSeconds
            )
        }

        item {
            Text(
                stringResource(R.string.transactions_in_view, transactions.size, allTransactions.size),
                fontSize = 12.sp,
                color = FinanceColors.TextSoft
            )
        }

        item {
            ResponsiveStatGrid(
                entries = listOf(
                    StatEntry(stringResource(R.string.label_income), fmt(totals.income), FinanceColors.Income, DashboardMetric.INCOME),
                    StatEntry(stringResource(R.string.label_expense), fmt(totals.expense), FinanceColors.Expense, DashboardMetric.EXPENSE),
                    StatEntry(stringResource(R.string.label_savings), fmt(totals.savings), FinanceColors.Savings, DashboardMetric.SAVINGS),
                    StatEntry(
                        stringResource(R.string.label_transfers_net),
                        (if (totals.transferNet >= 0) "+" else "") + fmt(totals.transferNet),
                        FinanceColors.Transfer,
                        DashboardMetric.TRANSFER_NET
                    ),
                    StatEntry(stringResource(R.string.label_net_balance), fmt(totals.net), FinanceColors.Text, DashboardMetric.NET)
                ),
                onCardClick = { detailMetric = it }
            )
        }

        item {
            Column {
                ResponsiveStatGrid(
                    entries = listOf(
                        StatEntry(stringResource(R.string.avg_monthly_income), fmt(averages.income), FinanceColors.Income, DashboardMetric.AVG_INCOME),
                        StatEntry(stringResource(R.string.avg_monthly_expense), fmt(averages.expense), FinanceColors.Expense, DashboardMetric.AVG_EXPENSE),
                        StatEntry(stringResource(R.string.avg_monthly_savings), fmt(averages.savings), FinanceColors.Savings, DashboardMetric.AVG_SAVINGS)
                    ),
                    onCardClick = { detailMetric = it }
                )
                if (averages.months > 0) {
                    Text(
                        stringResource(R.string.averaged_across_months, averages.months),
                        fontSize = 11.5.sp, color = FinanceColors.TextSoft, modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }

        item {
            SectionCard(title = stringResource(R.string.section_expenses_by_category)) {
                DonutChart(data = expenseByCategory, valueFormatter = { fmt(it) })
            }
        }

        item {
            SectionCard(title = stringResource(R.string.section_income_vs_expense)) {
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
            SectionCard(title = stringResource(R.string.section_savings_growth)) {
                LineChart(labels = monthLabels, values = savingsTrend, lineColor = FinanceColors.Savings)
            }
        }

        item {
            SectionCard(title = stringResource(R.string.section_insights)) {
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
    }

    detailMetric?.let { metric ->
        val detailTxs = remember(metric, transactions) { transactionsForMetric(metric, transactions) }
        val title = when (metric) {
            DashboardMetric.INCOME -> stringResource(R.string.details_income)
            DashboardMetric.EXPENSE -> stringResource(R.string.details_expense)
            DashboardMetric.SAVINGS -> stringResource(R.string.details_savings)
            DashboardMetric.TRANSFER_NET -> stringResource(R.string.details_transfer)
            DashboardMetric.NET -> stringResource(R.string.details_all_period)
            DashboardMetric.AVG_INCOME -> stringResource(R.string.details_income_average)
            DashboardMetric.AVG_EXPENSE -> stringResource(R.string.details_expense_average)
            DashboardMetric.AVG_SAVINGS -> stringResource(R.string.details_savings_average)
        }
        MetricDetailDialog(
            title = title,
            subtitle = dateFilter.label(stringResource(R.string.date_filter_all_dates)),
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
private fun DashboardBrandHeader() {
    val appName = stringResource(R.string.dashboard_title)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(
                        FinanceColors.Header,
                        FinanceColors.Accent.copy(alpha = 0.92f)
                    )
                )
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.app_logo),
            contentDescription = appName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                appName,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = FinanceColors.OnHeader,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                stringResource(R.string.dashboard_subtitle),
                fontSize = 13.sp,
                color = FinanceColors.OnHeader.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ResponsiveStatGrid(
    entries: List<StatEntry>,
    onCardClick: (DashboardMetric) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val availableWidth = this.maxWidth
        val columns = when {
            availableWidth >= 900.dp -> 4
            availableWidth >= 600.dp -> 3
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
}

private fun transactionsForMetric(metric: DashboardMetric, transactions: List<Transaction>): List<Transaction> =
    when (metric) {
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
    var sort by remember { mutableStateOf(TransactionSort.DATE_DESC) }
    val sortedTxs = remember(transactions, sort) { transactions.applySort(sort) }

    fun saveBytes(format: DetailShareFormat, uri: android.net.Uri?) {
        if (uri == null) return
        val bytes = DetailExport.buildBytes(context, format, title, subtitle, sortedTxs, currency)
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
                                contentDescription = stringResource(R.string.cd_close),
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
                                stringResource(R.string.filter_prefix, subtitle),
                                fontSize = 12.sp,
                                color = FinanceColors.OnHeader.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                stringResource(
                                    R.string.currency_tx_count,
                                    "${currency.symbol} (${currency.code})",
                                    sortedTxs.size
                                ),
                                fontSize = 11.sp,
                                color = FinanceColors.OnHeader.copy(alpha = 0.75f),
                                modifier = Modifier.padding(top = 2.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = { showShareOptions = true },
                            enabled = sortedTxs.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = stringResource(R.string.cd_share_all),
                                tint = FinanceColors.OnHeader
                            )
                        }
                    }
                    HorizontalDivider(color = FinanceColors.Accent.copy(alpha = 0.45f), thickness = 2.dp)
                }

                if (sortedTxs.isNotEmpty()) {
                    TransactionSortChips(
                        sort = sort,
                        onSortChange = { sort = it },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                if (sortedTxs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(FinanceColors.BandEven)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.no_transactions_period), color = FinanceColors.TextSoft)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        itemsIndexed(sortedTxs, key = { _, tx -> tx.id }) { index, tx ->
                            CompactDetailRow(
                                tx = tx,
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
            title = { Text(stringResource(R.string.share_or_save)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        stringResource(R.string.share_sheet_help),
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
                                    context, format, title, subtitle, sortedTxs, currency
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
                TextButton(onClick = { showShareOptions = false }) { Text(stringResource(R.string.action_cancel)) }
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
    val context = LocalContext.current
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
                text = "${tx.formatRecordedAt()} · ${CategoryLabels.display(context, tx.category)}",
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
