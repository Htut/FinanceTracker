package com.financetracker.evolva.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.financetracker.evolva.R
import com.financetracker.evolva.data.model.TransactionSort

/** Two chips: Date and Amount. Tap selected chip to flip ↑/↓. */
@Composable
fun TransactionSortChips(
    sort: TransactionSort,
    onSortChange: (TransactionSort) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateLabel = if (sort.isDate) {
        stringResource(R.string.sort_date_with_arrow, sort.arrow)
    } else {
        stringResource(R.string.sort_date)
    }
    val amountLabel = if (sort.isAmount) {
        stringResource(R.string.sort_amount_with_arrow, sort.arrow)
    } else {
        stringResource(R.string.sort_amount)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = sort.isDate,
            onClick = { onSortChange(sort.onDateClick()) },
            label = { Text(dateLabel) }
        )
        FilterChip(
            selected = sort.isAmount,
            onClick = { onSortChange(sort.onAmountClick()) },
            label = { Text(amountLabel) }
        )
    }
}
