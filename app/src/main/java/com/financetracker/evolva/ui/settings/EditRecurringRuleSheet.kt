package com.financetracker.evolva.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financetracker.evolva.data.model.Categories
import com.financetracker.evolva.data.model.RecurringRule
import com.financetracker.evolva.data.model.TransactionType
import com.financetracker.evolva.data.model.TransferDirection
import com.financetracker.evolva.ui.theme.FinanceColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRecurringRuleSheet(
    rule: RecurringRule,
    customExpenseCategories: List<String>,
    onDismiss: () -> Unit,
    onSave: (RecurringRule) -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var type by remember { mutableStateOf(rule.type) }
    var category by remember { mutableStateOf(rule.category) }
    var direction by remember { mutableStateOf(rule.direction ?: TransferDirection.OUT) }
    var amountText by remember {
        mutableStateOf(
            if (rule.amount == rule.amount.toLong().toDouble()) rule.amount.toLong().toString()
            else rule.amount.toString()
        )
    }
    var day by remember { mutableIntStateOf(rule.day.coerceIn(1, 28)) }
    var note by remember { mutableStateOf(rule.note.orEmpty()) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var dayMenuExpanded by remember { mutableStateOf(false) }

    val categories = Categories.forType(type, customExpenseCategories)
    if (category !in categories) category = categories.first()

    val amountValid = amountText.toDoubleOrNull()?.let { it > 0 } ?: false

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Edit recurring rule",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = FinanceColors.Text
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text("Type", fontSize = 12.sp, color = FinanceColors.TextSoft)
            Spacer(modifier = Modifier.height(6.dp))
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
                Spacer(modifier = Modifier.height(14.dp))
                Text("Direction", fontSize = 12.sp, color = FinanceColors.TextSoft)
                Spacer(modifier = Modifier.height(6.dp))
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

            Spacer(modifier = Modifier.height(14.dp))
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
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
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

            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))
            ExposedDropdownMenuBox(
                expanded = dayMenuExpanded,
                onExpandedChange = { dayMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = "Day $day",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Day of month") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayMenuExpanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = dayMenuExpanded,
                    onDismissRequest = { dayMenuExpanded = false }
                ) {
                    (1..28).forEach { d ->
                        DropdownMenuItem(text = { Text("Day $d") }, onClick = {
                            day = d
                            dayMenuExpanded = false
                        })
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: return@Button
                    onSave(
                        rule.copy(
                            type = type,
                            category = category,
                            amount = amount,
                            note = note.ifBlank { null },
                            direction = if (type == TransactionType.TRANSFER) direction else null,
                            day = day
                        )
                    )
                },
                enabled = amountValid,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save changes") }

            TextButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Delete rule", color = FinanceColors.Expense) }

            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel")
            }
        }
    }
}
