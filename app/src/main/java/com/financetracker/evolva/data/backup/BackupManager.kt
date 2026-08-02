package com.financetracker.evolva.data.backup

import com.financetracker.evolva.data.model.Account
import com.financetracker.evolva.data.model.AppCurrency
import com.financetracker.evolva.data.model.Budget
import com.financetracker.evolva.data.model.RecurringRule
import com.financetracker.evolva.data.model.Transaction
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant

/**
 * Pure (no Android framework dependency) JSON/CSV building and parsing.
 * The actual file picking + stream I/O lives in SettingsScreen, using the
 * Storage Access Framework — that's what lets an exported file be shared
 * straight to another app (Drive, WhatsApp, email...) to hand to a family
 * member, and lets an imported file come from anywhere the user picks.
 */
object BackupManager {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun buildPayload(
        currency: AppCurrency,
        transactions: List<Transaction>,
        budgets: List<Budget>,
        recurring: List<RecurringRule>,
        customExpenseCategories: List<String> = emptyList(),
        accounts: List<Account> = emptyList(),
        exchangeRates: Map<String, Double> = emptyMap()
    ): BackupPayload = BackupPayload(
        exportedAt = Instant.now().toString(),
        currency = currency.code,
        transactions = transactions.map { it.toDto() },
        budgets = budgets.map { it.toDto() },
        recurring = recurring.map { it.toDto() },
        customExpenseCategories = customExpenseCategories,
        accounts = accounts.map { it.toDto() },
        exchangeRates = exchangeRates
    )

    fun toJson(payload: BackupPayload): String = json.encodeToString(payload)

    /** Returns null if the text isn't a recognizable backup file. */
    fun parseJson(text: String): BackupPayload? = try {
        json.decodeFromString<BackupPayload>(text)
    } catch (_: Exception) {
        null
    }

    fun toCsv(transactions: List<Transaction>, homeCurrencyCode: String): String {
        val header = listOf(
            "Date", "Time", "Type", "Direction", "Category", "Note",
            "Amount", "Currency", "ExchangeRate", "AccountId"
        )
        val rows = transactions.sortedWith(compareBy({ it.date }, { it.time })).map { t ->
            listOf(
                t.date.toString(),
                t.time?.toString().orEmpty(),
                t.type.name,
                t.direction?.name ?: "",
                t.category,
                (t.note ?: "").replace(",", " "),
                t.amount.toString(),
                t.currencyCode ?: homeCurrencyCode,
                t.exchangeRate?.toString().orEmpty(),
                t.accountId.orEmpty()
            )
        }
        return (listOf(header) + rows).joinToString("\n") { row -> row.joinToString(",") }
    }

    fun backupFileName(): String = "finance-tracker-backup-${todayIso()}.json"
    fun csvFileName(): String = "finance-transactions-${todayIso()}.csv"

    private fun todayIso(): String = java.time.LocalDate.now().toString()
}
