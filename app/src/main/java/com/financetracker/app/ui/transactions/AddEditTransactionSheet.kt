package com.financetracker.app.ui.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financetracker.app.data.model.Categories
import com.financetracker.app.data.model.Transaction
import com.financetracker.app.data.model.TransactionType
import com.financetracker.app.data.model.TransferDirection
import com.financetracker.app.ui.theme.FinanceColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * One sheet handles both add and edit, mirroring the web app's single
 * modal. When [existing] is null this is "Add Transaction"; otherwise it's
 * pre-filled for editing. "Repeat monthly" only applies when adding a new
 * transaction; editing a transaction that a recurring rule already
 * generated instead offers "Stop repeating".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionSheet(
    existing: Transaction?,
    onDismiss: () -> Unit,
    onSave: (Transaction, Boolean) -> Unit,
    onStopRecurring: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var type by remember { mutableStateOf(existing?.type ?: TransactionType.EXPENSE) }
    var category by remember { mutableStateOf(existing?.category ?: Categories.forType(type).first()) }
    var direction by remember { mutableStateOf(existing?.direction ?: TransferDirection.OUT) }
    var amountText by remember { mutableStateOf(existing?.amount?.let(::formatPlain) ?: "") }
    var date by remember { mutableStateOf(existing?.date ?: LocalDate.now()) }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var repeatMonthly by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }

    val categories = Categories.forType(type)
    if (category !in categories) category = categories.first()

    val amountValid = amountText.toDoubleOrNull()?.let { it > 0.0 } ?: false

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                if (existing == null) "Add Transaction" else "Edit Transaction",
                fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = FinanceColors.Text
            )
            Spacer(Modifier.height(16.dp))

            Text("Type", fontSize = 12.sp, color = FinanceColors.TextSoft)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TransactionType.entries.forEach { t ->
                    FilterChip(
                        selected = type == t,
                        onClick = { type = t },
                        label = { Text(t.name.lowercase().replaceFirstChar { c -> c.uppercase() }) }
                    )
                }
            }

            if (type == TransactionType.TRANSFER) {
                Spacer(Modifier.height(14.dp))
                Text("Direction", fontSize = 12.sp, color = FinanceColors.TextSoft)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = direction == TransferDirection.OUT,
                        onClick = { direction = TransferDirection.OUT },
                        label = { Text("Sent") }
                    )
                    FilterChip(
                        selected = direction == TransferDirection.IN,
                        onClick = { direction = TransferDirection.IN },
                        label = { Text("Received") }
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            ExposedDropdownMenuBox(
                expanded = categoryMenuExpanded,
                onExpandedChange = { categoryMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(if (type == TransactionType.TRANSFER) "Relation" else "Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                androidx.compose.material3.ExposedDropdownMenu(
                    expanded = categoryMenuExpanded,
                    onDismissRequest = { categoryMenuExpanded = false }
                ) {
                    categories.forEach { c ->
                        DropdownMenuItem(text = { Text(c) }, onClick = {
                            category = c
                            categoryMenuExpanded = false
                        })
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = amountText,
                onValueChange = { input -> amountText = input.filter { it.isDigit() || it == '.' } },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                onValueChange = {},
                readOnly = true,
                label = { Text("Date") },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = "Pick date")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
            )

            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            if (existing == null) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = repeatMonthly, onCheckedChange = { repeatMonthly = it })
                    Text("Repeat monthly", fontSize = 13.sp, color = FinanceColors.Text)
                }
            } else if (existing.recurringId != null) {
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = { onStopRecurring(existing.recurringId) }) {
                    Text("Stop repeating this transaction", color = FinanceColors.Expense)
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull() ?: return@Button
                        val tx = Transaction(
                            id = existing?.id ?: UUID.randomUUID().toString(),
                            type = type,
                            category = category,
                            amount = amount,
                            date = date,
                            note = note.ifBlank { null },
                            direction = if (type == TransactionType.TRANSFER) direction else null,
                            recurringId = existing?.recurringId
                        )
                        onSave(tx, repeatMonthly)
                    },
                    enabled = amountValid,
                    modifier = Modifier.weight(1f)
                ) { Text(if (existing == null) "Add" else "Save") }
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

private fun formatPlain(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
