package com.financetracker.evolva.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financetracker.evolva.data.model.DateFilter
import com.financetracker.evolva.ui.theme.FinanceColors
import java.time.Instant
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateFilterBar(
    filter: DateFilter,
    onFilterChange: (DateFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    var showMonthPicker by remember { mutableStateOf(false) }
    var showYearPicker by remember { mutableStateOf(false) }
    var showRangePicker by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text("Date filter", fontSize = 12.sp, color = FinanceColors.TextSoft)
        Spacer(Modifier.height(6.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            FilterChip(
                selected = filter is DateFilter.All,
                onClick = { onFilterChange(DateFilter.All) },
                label = { Text("All") }
            )
            FilterChip(
                selected = filter is DateFilter.Month,
                onClick = { showMonthPicker = true },
                label = { Text(if (filter is DateFilter.Month) filter.label() else "Monthly") }
            )
            FilterChip(
                selected = filter is DateFilter.Year,
                onClick = { showYearPicker = true },
                label = { Text(if (filter is DateFilter.Year) filter.label() else "Yearly") }
            )
            FilterChip(
                selected = filter is DateFilter.Range,
                onClick = { showRangePicker = true },
                label = { Text(if (filter is DateFilter.Range) "Range" else "Date range") }
            )
        }
        if (filter !is DateFilter.All) {
            Text(
                filter.label(),
                fontSize = 12.sp,
                color = FinanceColors.TextSoft,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }

    if (showMonthPicker) {
        MonthYearPickerDialog(
            title = "Select month",
            initialMonth = (filter as? DateFilter.Month)?.month ?: YearMonth.now(),
            onDismiss = { showMonthPicker = false },
            onConfirm = {
                onFilterChange(DateFilter.Month(it))
                showMonthPicker = false
            }
        )
    }

    if (showYearPicker) {
        YearPickerDialog(
            initialYear = (filter as? DateFilter.Year)?.year ?: Year.now().value,
            onDismiss = { showYearPicker = false },
            onConfirm = {
                onFilterChange(DateFilter.Year(it))
                showYearPicker = false
            }
        )
    }

    if (showRangePicker) {
        RangePickerDialog(
            initial = filter as? DateFilter.Range,
            onDismiss = { showRangePicker = false },
            onConfirm = { start, end ->
                onFilterChange(DateFilter.Range(start, end))
                showRangePicker = false
            }
        )
    }
}

@Composable
private fun MonthYearPickerDialog(
    title: String,
    initialMonth: YearMonth,
    onDismiss: () -> Unit,
    onConfirm: (YearMonth) -> Unit
) {
    var yearText by remember { mutableStateOf(initialMonth.year.toString()) }
    var monthText by remember { mutableStateOf(initialMonth.monthValue.toString()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = yearText,
                    onValueChange = { yearText = it.filter(Char::isDigit).take(4) },
                    label = { Text("Year") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = monthText,
                    onValueChange = { monthText = it.filter(Char::isDigit).take(2) },
                    label = { Text("Month (1–12)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let { Text(it, color = FinanceColors.Expense, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val y = yearText.toIntOrNull()
                val m = monthText.toIntOrNull()
                if (y == null || y !in 1970..2100 || m == null || m !in 1..12) {
                    error = "Enter a valid year and month."
                    return@TextButton
                }
                onConfirm(YearMonth.of(y, m))
            }) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun YearPickerDialog(
    initialYear: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var yearText by remember { mutableStateOf(initialYear.toString()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select year") },
        text = {
            Column {
                OutlinedTextField(
                    value = yearText,
                    onValueChange = { yearText = it.filter(Char::isDigit).take(4) },
                    label = { Text("Year") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = FinanceColors.Expense, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val y = yearText.toIntOrNull()
                if (y == null || y !in 1970..2100) {
                    error = "Enter a valid year."
                    return@TextButton
                }
                onConfirm(y)
            }) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RangePickerDialog(
    initial: DateFilter.Range?,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, LocalDate) -> Unit
) {
    var start by remember { mutableStateOf(initial?.start ?: LocalDate.now().withDayOfMonth(1)) }
    var end by remember { mutableStateOf(initial?.end ?: LocalDate.now()) }
    var pickingStart by remember { mutableStateOf(true) }
    var showPicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Date range") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = { pickingStart = true; showPicker = true }) {
                    Text("Start: $start")
                }
                TextButton(onClick = { pickingStart = false; showPicker = true }) {
                    Text("End: $end")
                }
                error?.let { Text(it, color = FinanceColors.Expense, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (end.isBefore(start)) {
                    error = "End date must be on or after start date."
                    return@TextButton
                }
                onConfirm(start, end)
            }) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (showPicker) {
        val current = if (pickingStart) start else end
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = current.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        if (pickingStart) start = picked else end = picked
                    }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = pickerState)
        }
    }
}
