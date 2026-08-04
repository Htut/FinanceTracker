package com.financetracker.evolva.data.backup

import com.financetracker.evolva.data.model.Account
import com.financetracker.evolva.data.model.AppCurrency
import com.financetracker.evolva.data.model.Budget
import com.financetracker.evolva.data.model.RecurringRule
import com.financetracker.evolva.data.model.Transaction
import com.financetracker.evolva.data.model.TransactionType
import com.financetracker.evolva.data.model.TransferDirection
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

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

    /**
     * Parses a CSV produced by [toCsv] (or compatible). Returns null if the
     * header/rows are unusable. Generates new ids for each row.
     */
    fun parseCsv(text: String, homeCurrencyCode: String): List<Transaction>? {
        val lines = text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()
        if (lines.isEmpty()) return null
        val headerCells = splitCsvLine(lines.first()).map { it.trim().lowercase() }
        val idx = { name: String -> headerCells.indexOf(name).takeIf { it >= 0 } }
        val dateIdx = idx("date") ?: return null
        val typeIdx = idx("type") ?: return null
        val categoryIdx = idx("category") ?: return null
        val amountIdx = idx("amount") ?: return null
        val timeIdx = idx("time")
        val directionIdx = idx("direction")
        val noteIdx = idx("note")
        val currencyIdx = idx("currency")
        val rateIdx = idx("exchangerate")
        val accountIdx = idx("accountid")

        val out = ArrayList<Transaction>()
        for (line in lines.drop(1)) {
            val cells = splitCsvLine(line)
            fun cell(i: Int?): String = i?.let { cells.getOrNull(it)?.trim().orEmpty() }.orEmpty()
            val dateRaw = cell(dateIdx)
            val typeRaw = cell(typeIdx)
            val category = cell(categoryIdx)
            val amountRaw = cell(amountIdx)
            if (dateRaw.isEmpty() || typeRaw.isEmpty() || category.isEmpty() || amountRaw.isEmpty()) {
                continue
            }
            val date = runCatching { LocalDate.parse(dateRaw) }.getOrNull() ?: continue
            val type = runCatching {
                TransactionType.valueOf(typeRaw.uppercase())
            }.getOrNull() ?: continue
            val amount = amountRaw.toDoubleOrNull() ?: continue
            val time = cell(timeIdx).takeIf { it.isNotEmpty() }
                ?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
            val direction = cell(directionIdx).takeIf { it.isNotEmpty() }
                ?.let {
                    runCatching {
                        TransferDirection.valueOf(it.uppercase())
                    }.getOrNull()
                }
            val note = cell(noteIdx).takeIf { it.isNotEmpty() }
            val currencyCode = cell(currencyIdx).takeIf { it.isNotEmpty() && !it.equals(homeCurrencyCode, ignoreCase = true) }
            val exchangeRate = cell(rateIdx).toDoubleOrNull()
            val accountId = cell(accountIdx).takeIf { it.isNotEmpty() }
            out.add(
                Transaction(
                    id = UUID.randomUUID().toString(),
                    type = type,
                    category = category,
                    amount = amount,
                    date = date,
                    time = time,
                    note = note,
                    direction = direction,
                    accountId = accountId,
                    currencyCode = currencyCode,
                    exchangeRate = exchangeRate,
                    locked = false
                )
            )
        }
        return out
    }

    /** Minimal CSV split that respects double-quoted fields. */
    private fun splitCsvLine(line: String): List<String> {
        val result = ArrayList<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(c)
            }
            i++
        }
        result.add(current.toString())
        return result
    }

    fun backupFileName(): String = "finance-tracker-backup-${todayIso()}.json"
    fun csvFileName(): String = "finance-transactions-${todayIso()}.csv"

    private fun todayIso(): String = java.time.LocalDate.now().toString()
}
