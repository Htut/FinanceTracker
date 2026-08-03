package com.financetracker.evolva.data.model

import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.UUID

enum class TransactionType { INCOME, EXPENSE, SAVINGS, TRANSFER }

enum class TransferDirection { OUT, IN }

enum class AppCurrency(val code: String, val symbol: String, val decimals: Int) {
    // Regional defaults first
    MYR("MYR", "RM", 2),
    USD("USD", "$", 2),
    MMK("MMK", "Ks", 0),
    SGD("SGD", "S$", 2),
    IDR("IDR", "Rp", 0),
    THB("THB", "฿", 2),
    VND("VND", "₫", 0),
    PHP("PHP", "₱", 2),
    // Major world
    EUR("EUR", "€", 2),
    GBP("GBP", "£", 2),
    JPY("JPY", "¥", 0),
    CNY("CNY", "¥", 2),
    KRW("KRW", "₩", 0),
    INR("INR", "₹", 2),
    AUD("AUD", "A$", 2),
    CAD("CAD", "C$", 2),
    CHF("CHF", "CHF", 2),
    HKD("HKD", "HK$", 2),
    TWD("TWD", "NT$", 0),
    NZD("NZD", "NZ$", 2),
    SAR("SAR", "﷼", 2),
    AED("AED", "د.إ", 2),
    TRY("TRY", "₺", 2),
    RUB("RUB", "₽", 2),
    BRL("BRL", "R$", 2),
    MXN("MXN", "MX$", 2),
    ZAR("ZAR", "R", 2),
    PKR("PKR", "₨", 2),
    BDT("BDT", "৳", 2),
    EGP("EGP", "E£", 2);

    companion object {
        fun fromCode(code: String?): AppCurrency = entries.find { it.code == code } ?: MYR
    }
}

/**
 * Built-in category lists (aligned with the web app), plus helpers that merge
 * user-defined expense types.
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

    fun forType(type: TransactionType, customExpense: List<String> = emptyList()): List<String> =
        when (type) {
            TransactionType.INCOME -> income
            TransactionType.EXPENSE -> expenseCategories(customExpense)
            TransactionType.SAVINGS -> savings
            TransactionType.TRANSFER -> transfer
        }

    fun expenseCategories(customExpense: List<String>): List<String> {
        val builtInLower = expense.map { it.lowercase() }.toSet()
        val customs = normalizeCustom(customExpense)
            .filter { it.lowercase() !in builtInLower }
        // Keep built-ins first; put "Other" last after customs.
        val core = expense.filter { it != "Other" }
        return core + customs + listOf("Other")
    }

    fun budgetable(customExpense: List<String> = emptyList()): List<String> =
        expenseCategories(customExpense).filter { it != "Other" }

    /** Trim, drop blanks, de-dupe case-insensitively (first spelling wins). */
    fun normalizeCustom(names: List<String>): List<String> {
        val seen = linkedSetOf<String>()
        val result = mutableListOf<String>()
        names.forEach { raw ->
            val name = raw.trim().replace(Regex("\\s+"), " ")
            if (name.isEmpty()) return@forEach
            val key = name.lowercase()
            if (key in seen) return@forEach
            seen += key
            result += name
        }
        return result
    }

    fun isBuiltInExpense(name: String): Boolean =
        expense.any { it.equals(name.trim(), ignoreCase = true) }
}

enum class AccountKind { CASH, BANK, EWALLET }

data class Account(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val kind: AccountKind,
    val openingBalance: Double = 0.0,
    val archived: Boolean = false
) {
    companion object {
        const val ID_CASH = "cash"
        const val ID_BANK = "bank"
        const val ID_EWALLET = "ewallet"

        fun defaults(): List<Account> = listOf(
            Account(id = ID_CASH, name = "Cash", kind = AccountKind.CASH),
            Account(id = ID_BANK, name = "Bank", kind = AccountKind.BANK),
            Account(id = ID_EWALLET, name = "E-Wallet", kind = AccountKind.EWALLET)
        )
    }
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
    val recurringId: String? = null,
    /** Wallet this transaction affects; null = legacy / unspecified. */
    val accountId: String? = null,
    /** Local file path or content URI for an optional receipt image. */
    val receiptUri: String? = null,
    /** Currency of [amount]; null means app home currency. */
    val currencyCode: String? = null,
    /** Units of home currency per 1 unit of [currencyCode]. Null/blank = 1.0. */
    val exchangeRate: Double? = null,
    /** When true, edit/delete are blocked until unlocked. */
    val locked: Boolean = false
)

/** Convert transaction amount into the app's home currency. */
fun Transaction.homeAmount(): Double {
    val rate = exchangeRate?.takeIf { it > 0 } ?: 1.0
    return amount * rate
}

private val TIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** Date always; time only when present. */
fun Transaction.formatRecordedAt(): String =
    if (time != null) "$date ${time.format(TIME_FMT)}" else date.toString()

data class Budget(
    val category: String,
    val limit: Double,
    val locked: Boolean = false
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

/** Amount digits only (with grouping), no currency symbol. */
fun formatAmountNumber(amount: Double, currency: AppCurrency): String {
    val abs = kotlin.math.abs(amount)
    val pattern = if (currency.decimals > 0) "%,.${currency.decimals}f" else "%,.0f"
    val formatted = String.format(pattern, abs)
    return (if (amount < 0) "-" else "") + formatted
}

/** Compact FX rate for linked forward/inverse fields (no grouping). */
fun formatExchangeRate(rate: Double): String {
    if (rate == rate.toLong().toDouble()) return rate.toLong().toString()
    return String.format(java.util.Locale.US, "%.8f", rate)
        .trimEnd('0')
        .trimEnd('.')
}
