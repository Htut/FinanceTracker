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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financetracker.evolva.R
import com.financetracker.evolva.data.calc.FinanceCalculator
import com.financetracker.evolva.data.locale.AccountLabels
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
    val context = androidx.compose.ui.platform.LocalContext.current
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

    SectionCard(title = stringResource(R.string.section_wallets)) {
        Text(
            stringResource(R.string.wallets_help),
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
                    Text(
                        AccountLabels.display(context, account),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FinanceColors.Text
                    )
                    Text(
                        AccountLabels.kindLabel(context, account.kind),
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
                TextButton(onClick = { startEdit(account) }) {
                    Text(stringResource(R.string.action_edit))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            if (editing == null) stringResource(R.string.add_wallet)
            else stringResource(R.string.edit_wallet),
            fontSize = 12.sp,
            color = FinanceColors.TextSoft
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.label_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AccountKind.entries.forEach { k ->
                FilterChip(
                    selected = kind == k,
                    onClick = { kind = k },
                    label = { Text(AccountLabels.kindLabel(context, k)) }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = openingText,
            onValueChange = { openingText = it.filter { ch -> ch.isDigit() || ch == '.' || ch == '-' } },
            label = { Text(stringResource(R.string.opening_balance)) },
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
                    val id = editing?.id ?: UUID.randomUUID().toString()
                    val storedName = when (id) {
                        Account.ID_CASH -> "Cash"
                        Account.ID_BANK -> "Bank"
                        Account.ID_EWALLET -> "E-Wallet"
                        else -> trimmed
                    }
                    onSave(
                        Account(
                            id = id,
                            name = storedName,
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
            ) {
                Text(
                    if (editing == null) stringResource(R.string.action_add)
                    else stringResource(R.string.action_save)
                )
            }
            if (editing != null && editing?.id !in listOf(Account.ID_CASH, Account.ID_BANK, Account.ID_EWALLET)) {
                TextButton(onClick = {
                    editing?.id?.let(onDelete)
                    startEdit(null)
                }) { Text(stringResource(R.string.action_delete), color = FinanceColors.Expense) }
            }
            if (editing != null) {
                TextButton(onClick = { startEdit(null); name = ""; openingText = "0" }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        }
    }
}
