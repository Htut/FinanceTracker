package com.financetracker.evolva.ui.transactions

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financetracker.evolva.R
import com.financetracker.evolva.data.locale.AccountLabels
import com.financetracker.evolva.data.locale.CategoryLabels
import com.financetracker.evolva.data.model.Categories
import com.financetracker.evolva.data.model.Account
import com.financetracker.evolva.data.model.AppCurrency
import com.financetracker.evolva.data.model.formatExchangeRate
import com.financetracker.evolva.data.model.Transaction
import com.financetracker.evolva.data.model.TransactionType
import com.financetracker.evolva.data.model.TransferDirection
import com.financetracker.evolva.data.receipt.ReceiptStore
import com.financetracker.evolva.ui.theme.FinanceColors
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.io.File
import java.util.UUID

/**
 * One sheet handles both add and edit. Time is optional — leave "Include time"
 * unchecked for a date-only record (the usual case).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionSheet(
    existing: Transaction?,
    accounts: List<Account>,
    homeCurrency: AppCurrency,
    exchangeRates: Map<String, Double>,
    customExpenseCategories: List<String> = emptyList(),
    onAddExpenseCategory: ((String) -> Unit)? = null,
    onDismiss: () -> Unit,
    onSave: (Transaction, Boolean) -> Unit,
    onStopRecurring: (String) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var type by remember { mutableStateOf(existing?.type ?: TransactionType.EXPENSE) }
    var category by remember {
        mutableStateOf(
            existing?.category ?: Categories.forType(TransactionType.EXPENSE, customExpenseCategories).first()
        )
    }
    var direction by remember { mutableStateOf(existing?.direction ?: TransferDirection.OUT) }
    var amountText by remember { mutableStateOf(existing?.amount?.let(::formatPlain) ?: "") }
    var date by remember { mutableStateOf(existing?.date ?: LocalDate.now()) }
    var includeTime by remember { mutableStateOf(existing?.time != null) }
    var time by remember { mutableStateOf(existing?.time ?: LocalTime.now().withSecond(0).withNano(0)) }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var repeatMonthly by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var showAddExpenseType by remember { mutableStateOf(false) }
    var newExpenseTypeText by remember { mutableStateOf("") }
    var accountId by remember(existing, accounts) {
        mutableStateOf(existing?.accountId ?: accounts.firstOrNull { !it.archived }?.id)
    }
    var accountMenuExpanded by remember { mutableStateOf(false) }
    var selectedCurrency by remember(existing, homeCurrency) {
        mutableStateOf(AppCurrency.fromCode(existing?.currencyCode ?: homeCurrency.code))
    }
    var rateText by remember(existing, homeCurrency, exchangeRates) {
        val selected = AppCurrency.fromCode(existing?.currencyCode ?: homeCurrency.code)
        val rate = if (selected == homeCurrency) 1.0
        else existing?.exchangeRate ?: exchangeRates[selected.code] ?: 1.0
        mutableStateOf(formatExchangeRate(rate))
    }
    var inverseRateText by remember(existing, homeCurrency, exchangeRates) {
        val selected = AppCurrency.fromCode(existing?.currencyCode ?: homeCurrency.code)
        val rate = if (selected == homeCurrency) 1.0
        else existing?.exchangeRate ?: exchangeRates[selected.code] ?: 1.0
        mutableStateOf(
            if (rate > 0.0) formatExchangeRate(1.0 / rate) else ""
        )
    }
    var receiptUri by remember(existing) { mutableStateOf(existing?.receiptUri) }
    val receiptPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            receiptUri = ReceiptStore.copyFromUri(context, it)
        }
    }

    val categories = Categories.forType(type, customExpenseCategories)
    if (category !in categories) {
        category = categories.first()
    }

    val amountValid = amountText.toDoubleOrNull()?.let { it > 0.0 } ?: false
    val rateValid = selectedCurrency == homeCurrency ||
        (rateText.toDoubleOrNull()?.let { it > 0.0 } == true)
    val timeLabel = time.format(DateTimeFormatter.ofPattern("HH:mm"))

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                stringResource(if (existing == null) R.string.add_transaction_title else R.string.edit_transaction),
                fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = FinanceColors.Text
            )
            Spacer(Modifier.height(16.dp))

            Text(stringResource(R.string.label_type), fontSize = 12.sp, color = FinanceColors.TextSoft)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TransactionType.entries.forEach { t ->
                    FilterChip(
                        selected = type == t,
                        onClick = { type = t },
                        label = { Text(CategoryLabels.typeLabel(context, t)) }
                    )
                }
            }

            if (type == TransactionType.TRANSFER) {
                Spacer(Modifier.height(14.dp))
                Text(stringResource(R.string.label_direction), fontSize = 12.sp, color = FinanceColors.TextSoft)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = direction == TransferDirection.OUT,
                        onClick = { direction = TransferDirection.OUT },
                        label = { Text(stringResource(R.string.direction_sent)) }
                    )
                    FilterChip(
                        selected = direction == TransferDirection.IN,
                        onClick = { direction = TransferDirection.IN },
                        label = { Text(stringResource(R.string.direction_received)) }
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            ExposedDropdownMenuBox(
                expanded = accountMenuExpanded,
                onExpandedChange = { accountMenuExpanded = it }
            ) {
                val selectedAccount = accounts.find { it.id == accountId }
                OutlinedTextField(
                    value = selectedAccount?.let { AccountLabels.display(context, it) }
                        ?: stringResource(R.string.label_unspecified),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.label_account)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(accountMenuExpanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = accountMenuExpanded,
                    onDismissRequest = { accountMenuExpanded = false }
                ) {
                    accounts.filterNot { it.archived }.forEach { account ->
                        DropdownMenuItem(
                            text = { Text(AccountLabels.display(context, account)) },
                            onClick = {
                                accountId = account.id
                                accountMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            ExposedDropdownMenuBox(
                expanded = categoryMenuExpanded,
                onExpandedChange = { categoryMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = CategoryLabels.display(context, category),
                    onValueChange = {},
                    readOnly = true,
                    label = {
                        Text(
                            stringResource(
                                if (type == TransactionType.TRANSFER) R.string.label_relation
                                else R.string.label_category
                            )
                        )
                    },
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
                        DropdownMenuItem(text = { Text(CategoryLabels.display(context, c)) }, onClick = {
                            category = c
                            categoryMenuExpanded = false
                        })
                    }
                    if (type == TransactionType.EXPENSE && onAddExpenseCategory != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.add_expense_type)) },
                            onClick = {
                                categoryMenuExpanded = false
                                newExpenseTypeText = ""
                                showAddExpenseType = true
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = amountText,
                onValueChange = { input -> amountText = input.filter { it.isDigit() || it == '.' } },
                label = { Text(stringResource(R.string.label_amount)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(14.dp))
            Text(stringResource(R.string.label_currency), fontSize = 12.sp, color = FinanceColors.TextSoft)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (listOf(homeCurrency) + AppCurrency.entries.filter { it != homeCurrency }).forEach { currency ->
                    FilterChip(
                        selected = selectedCurrency == currency,
                        onClick = {
                            selectedCurrency = currency
                            val rate = if (currency == homeCurrency) 1.0
                            else exchangeRates[currency.code] ?: 1.0
                            rateText = formatExchangeRate(rate)
                            inverseRateText = if (rate > 0.0) formatExchangeRate(1.0 / rate) else ""
                        },
                        label = { Text("${currency.symbol} ${currency.code}") }
                    )
                }
            }
            if (selectedCurrency != homeCurrency) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = rateText,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() || it == '.' }
                        rateText = filtered
                        filtered.toDoubleOrNull()?.takeIf { it > 0.0 }?.let { rate ->
                            inverseRateText = formatExchangeRate(1.0 / rate)
                        }
                    },
                    label = {
                        Text(
                            stringResource(
                                R.string.rate_one_in_home,
                                selectedCurrency.code,
                                homeCurrency.code
                            )
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = inverseRateText,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() || it == '.' }
                        inverseRateText = filtered
                        filtered.toDoubleOrNull()?.takeIf { it > 0.0 }?.let { inverse ->
                            rateText = formatExchangeRate(1.0 / inverse)
                        }
                    },
                    label = {
                        Text(
                            stringResource(
                                R.string.rate_one_in_home,
                                homeCurrency.code,
                                selectedCurrency.code
                            )
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.label_date)) },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = stringResource(R.string.cd_pick_date))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
            )

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = includeTime, onCheckedChange = { includeTime = it })
                Text(stringResource(R.string.include_time), fontSize = 13.sp, color = FinanceColors.Text)
            }
            if (includeTime) {
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = timeLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.label_time)) },
                    trailingIcon = {
                        IconButton(onClick = { showTimePicker = true }) {
                            Icon(Icons.Filled.Schedule, contentDescription = stringResource(R.string.cd_pick_time))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTimePicker = true }
                )
            }

            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.label_note)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(14.dp))
            Text(stringResource(R.string.label_receipt_optional), fontSize = 12.sp, color = FinanceColors.TextSoft)
            receiptUri?.let { value ->
                Text(
                    File(value).name.ifBlank { value },
                    fontSize = 12.sp,
                    color = FinanceColors.Text,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        receiptPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                ) {
                    Text(
                        stringResource(if (receiptUri == null) R.string.add_image else R.string.replace_image)
                    )
                }
                if (receiptUri != null) {
                    TextButton(onClick = { receiptUri = null }) { Text(stringResource(R.string.action_clear)) }
                }
            }

            if (existing == null) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = repeatMonthly, onCheckedChange = { repeatMonthly = it })
                    Text(stringResource(R.string.repeat_monthly), fontSize = 13.sp, color = FinanceColors.Text)
                }
            } else if (existing.recurringId != null) {
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = { onStopRecurring(existing.recurringId) }) {
                    Text(stringResource(R.string.stop_repeating), color = FinanceColors.Expense)
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.action_cancel))
                }
                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull() ?: return@Button
                        val tx = Transaction(
                            id = existing?.id ?: UUID.randomUUID().toString(),
                            type = type,
                            category = category,
                            amount = amount,
                            date = date,
                            time = if (includeTime) time else null,
                            note = note.ifBlank { null },
                            direction = if (type == TransactionType.TRANSFER) direction else null,
                            recurringId = existing?.recurringId,
                            accountId = accountId,
                            receiptUri = receiptUri,
                            currencyCode = selectedCurrency.code,
                            exchangeRate = if (selectedCurrency == homeCurrency) {
                                1.0
                            } else {
                                rateText.toDoubleOrNull() ?: return@Button
                            },
                            locked = existing?.locked ?: false
                        )
                        onSave(tx, repeatMonthly)
                    },
                    enabled = amountValid && rateValid,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        stringResource(if (existing == null) R.string.action_add else R.string.action_save)
                    )
                }
            }
        }
    }

    if (showAddExpenseType) {
        AlertDialog(
            onDismissRequest = { showAddExpenseType = false },
            title = { Text(stringResource(R.string.new_expense_type_title)) },
            text = {
                OutlinedTextField(
                    value = newExpenseTypeText,
                    onValueChange = { newExpenseTypeText = it },
                    label = { Text(stringResource(R.string.label_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = newExpenseTypeText.trim().replace(Regex("\\s+"), " ")
                    if (name.isNotEmpty()) {
                        onAddExpenseCategory?.invoke(name)
                        category = name
                        showAddExpenseType = false
                    }
                }) { Text(stringResource(R.string.action_add)) }
            },
            dismissButton = {
                TextButton(onClick = { showAddExpenseType = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
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
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = time.hour,
            initialMinute = time.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.select_time)) },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    time = LocalTime.of(timeState.hour, timeState.minute)
                    showTimePicker = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

private fun formatPlain(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
