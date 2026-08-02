package com.financetracker.evolva.data.backup

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
        recurring: List<RecurringRule>
    ): BackupPayload = BackupPayload(
        exportedAt = Instant.now().toString(),
        currency = currency.code,
        transactions = transactions.map { it.toDto() },
        budgets = budgets.map { it.toDto() },
        recurring = recurring.map { it.toDto() }
    )

    fun toJson(payload: BackupPayload): String = json.encodeToString(payload)

    /** Returns null if the text isn't a recognizable backup file. */
    fun parseJson(text: String): BackupPayload? = try {
        json.decodeFromString<BackupPayload>(text)
    } catch (e: Exception) {
        null
    }

    fun toCsv(transactions: List<Transaction>, currencyCode: String): String {
        val header = listOf("Date", "Type", "Direction", "Category", "Note", "Amount", "Currency")
        val rows = transactions.sortedBy { it.date }.map { t ->
            listOf(
                t.date.toString(),
                t.type.name,
                t.direction?.name ?: "",
                t.category,
                (t.note ?: "").replace(",", " "),
                t.amount.toString(),
                currencyCode
            )
        }
        return (listOf(header) + rows).joinToString("\n") { row -> row.joinToString(",") }
    }

    fun backupFileName(): String = "finance-tracker-backup-${todayIso()}.json"
    fun csvFileName(): String = "finance-transactions-${todayIso()}.csv"

    private fun todayIso(): String = java.time.LocalDate.now().toString()
}
