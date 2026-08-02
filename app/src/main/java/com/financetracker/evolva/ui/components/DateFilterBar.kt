package com.financetracker.evolva.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financetracker.evolva.data.model.DateFilter
import com.financetracker.evolva.ui.theme.FinanceColors
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.Year
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.util.Locale
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateFilterBar(
    filter: DateFilter,
    onFilterChange: (DateFilter) -> Unit,
    modifier: Modifier = Modifier,
    autoCloseSeconds: Int = 7
) {
    var expanded by remember { mutableStateOf(true) }
    var interactionTick by remember { mutableIntStateOf(0) }
    var showMonthPicker by remember { mutableStateOf(false) }
    var showYearPicker by remember { mutableStateOf(false) }
    var showRangePicker by remember { mutableStateOf(false) }
    var secondsLeft by remember { mutableIntStateOf(autoCloseSeconds) }

    fun resetAutoHide() {
        interactionTick++
    }

    val pickerOpen = showMonthPicker || showYearPicker || showRangePicker
    LaunchedEffect(expanded, autoCloseSeconds, interactionTick, pickerOpen) {
        if (!expanded || autoCloseSeconds <= 0 || pickerOpen) {
            if (!pickerOpen) secondsLeft = autoCloseSeconds
            return@LaunchedEffect
        }
        secondsLeft = autoCloseSeconds
        while (secondsLeft > 0) {
            delay(1.seconds)
            secondsLeft--
        }
        expanded = false
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    expanded = !expanded
                    if (expanded) resetAutoHide()
                }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Date filter", fontSize = 12.sp, color = FinanceColors.TextSoft)
                Text(
                    filter.label(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = FinanceColors.Text
                )
            }
            if (expanded && autoCloseSeconds > 0) {
                Text(
                    "Auto-hide ${secondsLeft}s",
                    fontSize = 11.sp,
                    color = FinanceColors.TextSoft,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "Collapse filter" else "Expand filter",
                tint = FinanceColors.TextSoft
            )
        }

        if (expanded) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                FilterChip(
                    selected = filter is DateFilter.All,
                    onClick = {
                        resetAutoHide()
                        onFilterChange(DateFilter.All)
                    },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = filter is DateFilter.Month,
                    onClick = {
                        resetAutoHide()
                        showMonthPicker = true
                    },
                    label = { Text(if (filter is DateFilter.Month) filter.label() else "Monthly") }
                )
                FilterChip(
                    selected = filter is DateFilter.Year,
                    onClick = {
                        resetAutoHide()
                        showYearPicker = true
                    },
                    label = { Text(if (filter is DateFilter.Year) filter.label() else "Yearly") }
                )
                FilterChip(
                    selected = filter is DateFilter.Range,
                    onClick = {
                        resetAutoHide()
                        showRangePicker = true
                    },
                    label = { Text(if (filter is DateFilter.Range) "Range" else "Date range") }
                )
            }
        }
    }

    if (showMonthPicker) {
        MonthWheelPickerDialog(
            initialMonth = (filter as? DateFilter.Month)?.month ?: YearMonth.now(),
            onDismiss = { showMonthPicker = false },
            onConfirm = {
                onFilterChange(DateFilter.Month(it))
                showMonthPicker = false
                resetAutoHide()
            }
        )
    }

    if (showYearPicker) {
        YearWheelPickerDialog(
            initialYear = (filter as? DateFilter.Year)?.year ?: Year.now().value,
            onDismiss = { showYearPicker = false },
            onConfirm = {
                onFilterChange(DateFilter.Year(it))
                showYearPicker = false
                resetAutoHide()
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
                resetAutoHide()
            }
        )
    }
}

@Composable
private fun MonthWheelPickerDialog(
    initialMonth: YearMonth,
    onDismiss: () -> Unit,
    onConfirm: (YearMonth) -> Unit
) {
    val years = remember { (Year.now().value - 15..Year.now().value + 2).toList() }
    val months = remember { Month.entries.toList() }
    var selectedYear by remember { mutableIntStateOf(initialMonth.year) }
    var selectedMonth by remember { mutableIntStateOf(initialMonth.monthValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select month") },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth().height(220.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ScrollSelectColumn(
                    title = "Month",
                    items = months.map {
                        it.getDisplayName(TextStyle.FULL, Locale.getDefault())
                    },
                    selectedIndex = selectedMonth - 1,
                    onSelected = { selectedMonth = it + 1 },
                    modifier = Modifier.weight(1.2f)
                )
                ScrollSelectColumn(
                    title = "Year",
                    items = years.map { it.toString() },
                    selectedIndex = years.indexOf(selectedYear).coerceAtLeast(0),
                    onSelected = { selectedYear = years[it] },
                    modifier = Modifier.weight(1f)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(YearMonth.of(selectedYear, selectedMonth)) }) {
                Text("Apply")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun YearWheelPickerDialog(
    initialYear: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val years = remember { (Year.now().value - 20..Year.now().value + 5).toList() }
    var selectedYear by remember { mutableIntStateOf(initialYear.coerceIn(years.first(), years.last())) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select year") },
        text = {
            Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                ScrollSelectColumn(
                    title = "Year",
                    items = years.map { it.toString() },
                    selectedIndex = years.indexOf(selectedYear).coerceAtLeast(0),
                    onSelected = { selectedYear = years[it] },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedYear) }) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ScrollSelectColumn(
    title: String,
    items: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex.coerceAtLeast(0))
    LaunchedEffect(selectedIndex) {
        listState.animateScrollToItem((selectedIndex - 1).coerceAtLeast(0))
    }

    Column(modifier = modifier) {
        Text(
            title,
            fontSize = 12.sp,
            color = FinanceColors.TextSoft,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .border(1.dp, FinanceColors.Border, RoundedCornerShape(10.dp))
                .padding(vertical = 4.dp)
        ) {
            itemsIndexed(items) { index, label ->
                val selected = index == selectedIndex
                Text(
                    text = label,
                    fontSize = if (selected) 16.sp else 14.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) FinanceColors.Text else FinanceColors.TextSoft,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelected(index) }
                        .padding(vertical = 10.dp, horizontal = 8.dp)
                )
            }
        }
    }
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
