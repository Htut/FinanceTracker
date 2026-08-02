package com.financetracker.evolva.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financetracker.evolva.data.model.Transaction
import com.financetracker.evolva.data.model.TransactionType
import com.financetracker.evolva.data.model.TransferDirection
import com.financetracker.evolva.data.model.formatRecordedAt
import com.financetracker.evolva.ui.theme.FinanceColors

@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = FinanceColors.Text,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = FinanceColors.Surface),
        border = BorderStroke(1.dp, FinanceColors.Border)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, fontSize = 12.sp, color = FinanceColors.TextSoft)
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = valueColor)
            if (onClick != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Tap for details", fontSize = 10.5.sp, color = FinanceColors.TextSoft)
            }
        }
    }
}

@Composable
fun SectionCard(
    title: String? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = FinanceColors.Surface),
        border = BorderStroke(1.dp, FinanceColors.Border)
    ) {
        Column(Modifier.padding(20.dp)) {
            if (title != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = FinanceColors.Text)
                    trailing?.invoke()
                }
                Spacer(Modifier.height(14.dp))
            }
            content()
        }
    }
}

@Composable
fun BudgetProgressBar(progress: Float, color: Color, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = progress.coerceIn(0f, 1f),
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(100.dp)),
        color = color,
        trackColor = FinanceColors.Background
    )
}

@Composable
fun Dot(color: Color, size: androidx.compose.ui.unit.Dp = 9.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
    )
}

fun typeColor(type: TransactionType): Color = when (type) {
    TransactionType.INCOME -> FinanceColors.Income
    TransactionType.EXPENSE -> FinanceColors.Expense
    TransactionType.SAVINGS -> FinanceColors.Savings
    TransactionType.TRANSFER -> FinanceColors.Transfer
}

@Composable
fun TypeTag(type: TransactionType, modifier: Modifier = Modifier) {
    val color = typeColor(type)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            type.name.lowercase().replaceFirstChar { it.uppercase() },
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

@Composable
fun TransactionRow(
    transaction: Transaction,
    formattedAmount: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = typeColor(transaction.type)
    val sign = when {
        transaction.type == TransactionType.EXPENSE -> "-"
        transaction.type == TransactionType.TRANSFER && transaction.direction == TransferDirection.OUT -> "-"
        else -> "+"
    }
    val categoryLabel = if (transaction.type == TransactionType.TRANSFER) {
        val arrow = if (transaction.direction == TransferDirection.OUT) "↗" else "↙"
        "$arrow ${transaction.category}"
    } else transaction.category

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(transaction.formatRecordedAt(), fontSize = 12.sp, color = FinanceColors.TextSoft)
                if (transaction.recurringId != null) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Filled.Repeat, contentDescription = "Recurring",
                        tint = FinanceColors.TextSoft, modifier = Modifier.size(12.dp)
                    )
                }
            }
            Spacer(Modifier.height(3.dp))
            TypeTag(transaction.type)
            Spacer(Modifier.height(3.dp))
            Text(categoryLabel, fontSize = 13.5.sp, color = FinanceColors.Text)
            if (!transaction.note.isNullOrBlank()) {
                Text(transaction.note, fontSize = 12.sp, color = FinanceColors.TextSoft)
            }
        }
        Text(
            "$sign $formattedAmount",
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
        IconButton(onClick = onClick) {
            Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = FinanceColors.TextSoft, modifier = Modifier.size(18.dp))
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = FinanceColors.Expense, modifier = Modifier.size(18.dp))
        }
    }
}
