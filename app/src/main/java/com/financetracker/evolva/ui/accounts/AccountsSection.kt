package com.financetracker.evolva.ui.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.financetracker.evolva.data.calc.FinanceCalculator
import com.financetracker.evolva.data.model.Account
import com.financetracker.evolva.data.model.AccountKind
import com.financetracker.evolva.data.model.AppCurrency
import com.financetracker.evolva.data.model.Transaction
import com.financetracker.evolva.data.model.formatAmount
import com.financetracker.evolva.ui.components.SectionCard
import com.financetracker.evolva.ui.theme.FinanceColors
import java.util.UUID

@Composable
fun AccountsSection(
    accounts: List<Account>,
    transactions: List<Transaction>,
    currency: AppCurrency,
    onSave: (Account) -> Unit,
    onDelete: (String) -> Unit
) {
    var editing by remember { mutableStateOf<Account?>(null) }
    var name by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf(AccountKind.CASH) }
    var openingText by remember { mutableStateOf("0") }

    fun startEdit(account: Account?) {
        editing = account
        name = account?.name.orEmpty()
        kind = account?.kind ?: AccountKind.CASH
        openingText = account?.openingBalance?.let {
            if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
        } ?: "0"
    }

    SectionCard(title = "Wallets") {
        Text(
            "Track Cash, Bank, and E-Wallet balances. Assign a wallet when adding a transaction.",
            fontSize = 12.sp,
            color = FinanceColors.TextSoft
        )
        Spacer(modifier = Modifier.height(10.dp))
        accounts.filterNot { it.archived }.forEach { account ->
            val balance = FinanceCalculator.accountBalance(account, transactions)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(account.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = FinanceColors.Text)
                    Text(
                        account.kind.name.lowercase().replaceFirstChar { it.uppercase() },
                        fontSize = 12.sp,
                        color = FinanceColors.TextSoft
                    )
                }
                Text(
                    formatAmount(balance, currency),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (balance >= 0) FinanceColors.Income else FinanceColors.Expense
                )
                TextButton(onClick = { startEdit(account) }) { Text("Edit") }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            if (editing == null) "Add wallet" else "Edit wallet",
            fontSize = 12.sp,
            color = FinanceColors.TextSoft
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AccountKind.entries.forEach { k ->
                FilterChip(
                    selected = kind == k,
                    onClick = { kind = k },
                    label = { Text(k.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = openingText,
            onValueChange = { openingText = it.filter { ch -> ch.isDigit() || ch == '.' || ch == '-' } },
            label = { Text("Opening balance") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val opening = openingText.toDoubleOrNull() ?: 0.0
                    val trimmed = name.trim()
                    if (trimmed.isEmpty()) return@Button
                    onSave(
                        Account(
                            id = editing?.id ?: UUID.randomUUID().toString(),
                            name = trimmed,
                            kind = kind,
                            openingBalance = opening,
                            archived = false
                        )
                    )
                    startEdit(null)
                    name = ""
                    openingText = "0"
                },
                enabled = name.isNotBlank()
            ) { Text(if (editing == null) "Add" else "Save") }
            if (editing != null && editing?.id !in listOf(Account.ID_CASH, Account.ID_BANK, Account.ID_EWALLET)) {
                TextButton(onClick = {
                    editing?.id?.let(onDelete)
                    startEdit(null)
                }) { Text("Delete", color = FinanceColors.Expense) }
            }
            if (editing != null) {
                TextButton(onClick = { startEdit(null); name = ""; openingText = "0" }) {
                    Text("Cancel")
                }
            }
        }
    }
}
