package com.financetracker.evolva.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.financetracker.evolva.data.model.Account
import com.financetracker.evolva.data.model.AccountKind
import com.financetracker.evolva.data.model.Budget
import com.financetracker.evolva.data.model.RecurringRule
import com.financetracker.evolva.data.model.Transaction
import com.financetracker.evolva.data.model.TransactionType
import com.financetracker.evolva.data.model.TransferDirection
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val kind: String,
    val openingBalance: Double,
    val archived: Boolean
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val type: String,
    val category: String,
    val amount: Double,
    val date: String, // ISO-8601 date, e.g. "2026-08-01"
    val time: String? = null, // optional "HH:mm:ss" / "HH:mm"
    val note: String?,
    val direction: String?,
    val recurringId: String?,
    val accountId: String? = null,
    val receiptUri: String? = null,
    val currencyCode: String? = null,
    val exchangeRate: Double? = null
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val category: String,
    val limitAmount: Double
)

@Entity(tableName = "recurring_rules")
data class RecurringRuleEntity(
    @PrimaryKey val id: String,
    val type: String,
    val category: String,
    val amount: Double,
    val note: String?,
    val direction: String?,
    val day: Int,
    val startMonth: String, // "yyyy-MM"
    val lastGeneratedMonth: String?,
    val active: Boolean
)

// ---- Mappers between Room entities and plain domain models ----

fun AccountEntity.toDomain() = Account(
    id = id,
    name = name,
    kind = runCatching { AccountKind.valueOf(kind) }.getOrDefault(AccountKind.CASH),
    openingBalance = openingBalance,
    archived = archived
)

fun Account.toEntity() = AccountEntity(
    id = id,
    name = name,
    kind = kind.name,
    openingBalance = openingBalance,
    archived = archived
)

fun TransactionEntity.toDomain() = Transaction(
    id = id,
    type = TransactionType.valueOf(type),
    category = category,
    amount = amount,
    date = LocalDate.parse(date),
    time = time?.let { LocalTime.parse(it) },
    note = note,
    direction = direction?.let { TransferDirection.valueOf(it) },
    recurringId = recurringId,
    accountId = accountId,
    receiptUri = receiptUri,
    currencyCode = currencyCode,
    exchangeRate = exchangeRate
)

fun Transaction.toEntity() = TransactionEntity(
    id = id,
    type = type.name,
    category = category,
    amount = amount,
    date = date.toString(),
    time = time?.toString(),
    note = note,
    direction = direction?.name,
    recurringId = recurringId,
    accountId = accountId,
    receiptUri = receiptUri,
    currencyCode = currencyCode,
    exchangeRate = exchangeRate
)

fun BudgetEntity.toDomain() = Budget(category = category, limit = limitAmount)
fun Budget.toEntity() = BudgetEntity(category = category, limitAmount = limit)

fun RecurringRuleEntity.toDomain() = RecurringRule(
    id = id,
    type = TransactionType.valueOf(type),
    category = category,
    amount = amount,
    note = note,
    direction = direction?.let { TransferDirection.valueOf(it) },
    day = day,
    startMonth = YearMonth.parse(startMonth),
    lastGeneratedMonth = lastGeneratedMonth?.let { YearMonth.parse(it) },
    active = active
)

fun RecurringRule.toEntity() = RecurringRuleEntity(
    id = id,
    type = type.name,
    category = category,
    amount = amount,
    note = note,
    direction = direction?.name,
    day = day,
    startMonth = startMonth.toString(),
    lastGeneratedMonth = lastGeneratedMonth?.toString(),
    active = active
)
