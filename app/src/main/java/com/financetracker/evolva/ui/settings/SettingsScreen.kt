package com.financetracker.evolva.ui.settings

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
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
import com.financetracker.evolva.data.backup.BackupManager
import com.financetracker.evolva.data.model.AppCurrency
import com.financetracker.evolva.data.model.RecurringRule
import com.financetracker.evolva.data.model.TransactionType
import com.financetracker.evolva.data.model.TransferDirection
import com.financetracker.evolva.data.model.formatAmount
import com.financetracker.evolva.data.templates.TEMPLATES
import com.financetracker.evolva.ui.ImportResult
import com.financetracker.evolva.ui.MainViewModel
import com.financetracker.evolva.ui.components.SectionCard
import com.financetracker.evolva.ui.components.TypeTag
import com.financetracker.evolva.ui.theme.FinanceColors

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val currency by viewModel.currency.collectAsState()
    val recurringRules by viewModel.recurringRules.collectAsState()
    val context = LocalContext.current

    fun fmt(v: Double) = formatAmount(v, currency)

    var importResultMessage by remember { mutableStateOf<String?>(null) }
    var pendingReplace by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

    val createJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { out ->
                out.write(viewModel.exportBackupJson().toByteArray())
            }
        }
    }
    val createCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { out ->
                out.write(viewModel.exportCsv().toByteArray())
            }
        }
    }
    val openJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val text = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> r.readText() }
            if (text != null) {
                when (val result = viewModel.importBackup(text, pendingReplace)) {
                    is ImportResult.Success -> importResultMessage = "Imported ${result.transactionCount} transactions."
                    ImportResult.Invalid -> importResultMessage = "That file doesn't look like a Finance Tracker backup."
                }
            } else {
                importResultMessage = "Couldn't read that file."
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Settings", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = FinanceColors.Text)
        }

        item {
            SectionCard(title = "Currency") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppCurrency.entries.forEach { c ->
                        FilterChip(
                            selected = currency == c,
                            onClick = { viewModel.setCurrency(c) },
                            label = { Text("${c.symbol} ${c.code}") }
                        )
                    }
                }
            }
        }

        item {
            SectionCard(title = "Starter Templates") {
                TEMPLATES.forEachIndexed { index, template ->
                    Column(Modifier.padding(vertical = 8.dp)) {
                        Text(template.label, fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = FinanceColors.Text)
                        Spacer(Modifier.height(3.dp))
                        Text(template.description, fontSize = 12.sp, color = FinanceColors.TextSoft)
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { viewModel.loadTemplate(template) }) {
                            Text("Load ${template.label}")
                        }
                    }
                    if (index != TEMPLATES.lastIndex) Divider(color = FinanceColors.Border)
                }
            }
        }

        item {
            SectionCard(title = "Recurring Transactions") {
                if (recurringRules.isEmpty()) {
                    Text(
                        "No recurring transactions yet. Turn on \"Repeat monthly\" when adding a transaction to create one.",
                        fontSize = 13.sp, color = FinanceColors.TextSoft
                    )
                } else {
                    recurringRules.forEach { rule ->
                        RecurringRuleRow(
                            rule = rule,
                            formattedAmount = fmt(rule.amount),
                            onToggleActive = { viewModel.toggleRecurringActive(rule) },
                            onDelete = { viewModel.deleteRecurringRule(rule.id) }
                        )
                        Divider(color = FinanceColors.Border)
                    }
                }
            }
        }

        item {
            SectionCard(title = "Backup, Restore & Share") {
                Text(
                    "Export a backup file to keep a copy of your data or send it to a family member. Import a backup to restore it, or merge someone else's data into yours.",
                    fontSize = 12.5.sp, color = FinanceColors.TextSoft
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { createJsonLauncher.launch(BackupManager.backupFileName()) }) {
                        Text("Export JSON")
                    }
                    OutlinedButton(onClick = { createCsvLauncher.launch(BackupManager.csvFileName()) }) {
                        Text("Export CSV")
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        pendingReplace = false
                        openJsonLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                    }) { Text("Import & Merge") }
                    OutlinedButton(onClick = {
                        pendingReplace = true
                        openJsonLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                    }) { Text("Import & Replace") }
                }
                importResultMessage?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, fontSize = 12.5.sp, color = FinanceColors.Savings)
                }
            }
        }

        item {
            SectionCard(title = "Danger Zone") {
                Text(
                    "Remove every transaction and start fresh. Budgets and recurring rules are kept. Export a backup first if you want to keep a copy — this can't be undone.",
                    fontSize = 12.5.sp, color = FinanceColors.TextSoft
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { showClearConfirm = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FinanceColors.Expense)
                ) { Text("Clear All Transactions") }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear all transactions?") },
            text = { Text("This permanently deletes every transaction. Budgets and recurring rules are kept. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllTransactions()
                    showClearConfirm = false
                }) { Text("Clear", color = FinanceColors.Expense) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun RecurringRuleRow(
    rule: RecurringRule,
    formattedAmount: String,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            TypeTag(rule.type)
            Spacer(Modifier.height(4.dp))
            val label = if (rule.type == TransactionType.TRANSFER) {
                val arrow = if (rule.direction == TransferDirection.OUT) "↗" else "↙"
                "$arrow ${rule.category}"
            } else rule.category
            Text(label, fontSize = 13.5.sp, color = FinanceColors.Text)
            Text("Day ${rule.day} of each month · $formattedAmount", fontSize = 12.sp, color = FinanceColors.TextSoft)
            if (!rule.active) {
                Text("Paused", fontSize = 11.5.sp, color = FinanceColors.Warn)
            }
        }
        Switch(checked = rule.active, onCheckedChange = { onToggleActive() })
        TextButton(onClick = onDelete) {
            Text("Delete", color = FinanceColors.Expense)
        }
    }
}
