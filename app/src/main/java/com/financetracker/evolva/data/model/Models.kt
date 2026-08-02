package com.financetracker.evolva.data.model

import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.UUID

enum class TransactionType { INCOME, EXPENSE, SAVINGS, TRANSFER }

enum class TransferDirection { OUT, IN }

enum class AppCurrency(val code: String, val symbol: String, val decimals: Int) {
    MYR("MYR", "RM", 2),
    USD("USD", "$", 2),
    MMK("MMK", "K", 0);

    companion object {
        fun fromCode(code: String?): AppCurrency = entries.find { it.code == code } ?: MYR
    }
}

/**
 * Mirrors the category lists from the web app exactly, including which
 * expense categories are eligible for a monthly budget limit (everything
 * except the catch-all "Other" bucket).
 */
object Categories {
    val income = listOf("Salary", "Freelance", "Investment", "Gift", "Other")

    val expense = listOf(
        "Food", "Transport", "Housing", "Education", "Utilities", "Insurance",
        "Entertainment", "Subscriptions", "Health", "Personal Care", "Shopping",
        "Clothing", "Travel", "Gifts & Donations", "Other"
    )

    val savings = listOf("Emergency Fund", "Retirement", "Goal", "Other")

    val transfer = listOf("Parent", "Spouse", "Sibling", "Child", "Relative", "Friend", "Other")

    fun forType(type: TransactionType): List<String> = when (type) {
        TransactionType.INCOME -> income
        TransactionType.EXPENSE -> expense
        TransactionType.SAVINGS -> savings
        TransactionType.TRANSFER -> transfer
    }

    val budgetable: List<String> get() = expense.filter { it != "Other" }
}

data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val type: TransactionType,
    val category: String,
    val amount: Double,
    val date: LocalDate,
    /** Optional wall-clock time; null means "date only" (usual case). */
    val time: LocalTime? = null,
    val note: String? = null,
    // Only meaningful when type == TRANSFER: OUT = money sent, IN = received.
    val direction: TransferDirection? = null,
    // Set when this transaction was auto-posted by a RecurringRule.
    val recurringId: String? = null
)

private val TIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** Date always; time only when present. */
fun Transaction.formatRecordedAt(): String =
    if (time != null) "$date ${time.format(TIME_FMT)}" else date.toString()

data class Budget(
    val category: String,
    val limit: Double
)

data class RecurringRule(
    val id: String = UUID.randomUUID().toString(),
    val type: TransactionType,
    val category: String,
    val amount: Double,
    val note: String? = null,
    val direction: TransferDirection? = null,
    val day: Int,
    val startMonth: YearMonth,
    val lastGeneratedMonth: YearMonth? = null,
    val active: Boolean = true
)

/** Mirrors the web app's fmt(): relabels the amount in the chosen currency
 * without any exchange-rate conversion. */
fun formatAmount(amount: Double, currency: AppCurrency): String {
    val abs = kotlin.math.abs(amount)
    val pattern = if (currency.decimals > 0) "%,.${currency.decimals}f" else "%,.0f"
    val formatted = String.format(pattern, abs)
    return (if (amount < 0) "-" else "") + currency.symbol + " " + formatted
}
