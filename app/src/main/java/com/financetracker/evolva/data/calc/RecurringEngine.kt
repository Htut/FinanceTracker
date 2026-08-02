package com.financetracker.evolva.data.calc

import com.financetracker.evolva.data.model.RecurringRule
import com.financetracker.evolva.data.model.Transaction
import java.time.LocalDate
import java.time.YearMonth

data class RecurringRunResult(
    val newTransactions: List<Transaction>,
    val updatedRules: List<RecurringRule>
)

object RecurringEngine {

    private fun monthsBetweenInclusive(from: YearMonth, to: YearMonth): List<YearMonth> {
        val result = mutableListOf<YearMonth>()
        var m = from
        while (!m.isAfter(to)) {
            result.add(m)
            m = m.plusMonths(1)
        }
        return result
    }

    /**
     * Backfills any missed months for every active rule, without ever
     * creating a future-dated transaction. Running this again after
     * persisting [RecurringRunResult.updatedRules] will not duplicate
     * anything — same behavior as the web app's runRecurringEngine().
     */
    fun runOnce(rules: List<RecurringRule>, today: LocalDate = LocalDate.now()): RecurringRunResult {
        val nowMonth = YearMonth.from(today)
        val newTransactions = mutableListOf<Transaction>()
        val updatedRules = mutableListOf<RecurringRule>()

        rules.forEach { rule ->
            if (!rule.active) return@forEach
            val from = rule.lastGeneratedMonth?.plusMonths(1) ?: rule.startMonth
            if (from.isAfter(nowMonth)) return@forEach

            var lastGenerated = rule.lastGeneratedMonth
            for (m in monthsBetweenInclusive(from, nowMonth)) {
                val dim = m.lengthOfMonth()
                val day = minOf(rule.day, dim)
                val isCurrentMonth = m == nowMonth
                if (isCurrentMonth && day > today.dayOfMonth) break // not due yet this month

                newTransactions.add(
                    Transaction(
                        type = rule.type,
                        category = rule.category,
                        amount = rule.amount,
                        date = m.atDay(day),
                        note = rule.note,
                        direction = rule.direction,
                        recurringId = rule.id
                    )
                )
                lastGenerated = m
            }
            if (lastGenerated != rule.lastGeneratedMonth) {
                updatedRules.add(rule.copy(lastGeneratedMonth = lastGenerated))
            }
        }

        return RecurringRunResult(newTransactions, updatedRules)
    }
}
