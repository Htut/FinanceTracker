package com.financetracker.evolva.data.backup

import com.financetracker.evolva.data.model.Budget
import com.financetracker.evolva.data.model.RecurringRule
import com.financetracker.evolva.data.model.Transaction
import com.financetracker.evolva.data.model.TransactionType
import com.financetracker.evolva.data.model.TransferDirection
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

@Serializable
data class TransactionDto(
    val id: String,
    val type: String,
    val category: String,
    val amount: Double,
    val date: String,
    val time: String? = null,
    val note: String? = null,
    val direction: String? = null,
    val recurringId: String? = null
)

@Serializable
data class BudgetDto(
    val category: String,
    val limit: Double
)

@Serializable
data class RecurringRuleDto(
    val id: String,
    val type: String,
    val category: String,
    val amount: Double,
    val note: String? = null,
    val direction: String? = null,
    val day: Int,
    val startMonth: String,
    val lastGeneratedMonth: String? = null,
    val active: Boolean = true
)

/** Same shape as the web app's JSON backup, so files exported from either
 * version can be imported into the other. */
@Serializable
data class BackupPayload(
    val app: String = "finance-tracker-android",
    val version: Int = 1,
    val exportedAt: String,
    val currency: String,
    val transactions: List<TransactionDto> = emptyList(),
    val budgets: List<BudgetDto> = emptyList(),
    val recurring: List<RecurringRuleDto> = emptyList()
)

// ---- Mappers between backup DTOs and domain models ----

fun Transaction.toDto() = TransactionDto(
    id = id, type = type.name, category = category, amount = amount,
    date = date.toString(), time = time?.toString(),
    note = note, direction = direction?.name, recurringId = recurringId
)

fun TransactionDto.toDomain() = Transaction(
    id = id,
    type = TransactionType.valueOf(type),
    category = category,
    amount = amount,
    date = LocalDate.parse(date),
    time = time?.let { LocalTime.parse(it) },
    note = note,
    direction = direction?.let { TransferDirection.valueOf(it) },
    recurringId = recurringId
)

fun Budget.toDto() = BudgetDto(category = category, limit = limit)
fun BudgetDto.toDomain() = Budget(category = category, limit = limit)

fun RecurringRule.toDto() = RecurringRuleDto(
    id = id, type = type.name, category = category, amount = amount, note = note,
    direction = direction?.name, day = day, startMonth = startMonth.toString(),
    lastGeneratedMonth = lastGeneratedMonth?.toString(), active = active
)

fun RecurringRuleDto.toDomain() = RecurringRule(
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
