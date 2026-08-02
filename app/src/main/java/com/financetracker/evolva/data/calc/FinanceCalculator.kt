package com.financetracker.evolva.data.calc

import com.financetracker.evolva.data.model.Transaction
import com.financetracker.evolva.data.model.TransactionType
import com.financetracker.evolva.data.model.TransferDirection
import java.time.YearMonth

data class Totals(
    val income: Double,
    val expense: Double,
    val savings: Double,
    val transferIn: Double,
    val transferOut: Double,
    val transferNet: Double,
    val net: Double
)

data class Averages(
    val months: Int,
    val income: Double,
    val expense: Double,
    val savings: Double,
    val transferNet: Double
)

enum class BudgetState { NONE, OK, WARN, OVER }

/**
 * Pure functions over the full transaction list — deliberately ported 1:1
 * from the web app's totals()/averages()/sumFor()/etc. so the numbers this
 * app shows always match what the browser version would show for the same
 * data.
 */
object FinanceCalculator {

    fun totals(transactions: List<Transaction>): Totals {
        var income = 0.0
        var expense = 0.0
        var savings = 0.0
        var transferIn = 0.0
        var transferOut = 0.0
        transactions.forEach { t ->
            when (t.type) {
                TransactionType.INCOME -> income += t.amount
                TransactionType.EXPENSE -> expense += t.amount
                TransactionType.SAVINGS -> savings += t.amount
                TransactionType.TRANSFER -> {
                    if (t.direction == TransferDirection.IN) transferIn += t.amount else transferOut += t.amount
                }
            }
        }
        val transferNet = transferIn - transferOut
        val net = income - expense - savings + transferNet
        return Totals(income, expense, savings, transferIn, transferOut, transferNet, net)
    }

    fun activeMonths(transactions: List<Transaction>): List<YearMonth> =
        transactions.map { YearMonth.from(it.date) }.distinct()

    fun averages(totals: Totals, transactions: List<Transaction>): Averages {
        val n = activeMonths(transactions).size
        return Averages(
            months = n,
            income = if (n > 0) totals.income / n else 0.0,
            expense = if (n > 0) totals.expense / n else 0.0,
            savings = if (n > 0) totals.savings / n else 0.0,
            transferNet = if (n > 0) totals.transferNet / n else 0.0
        )
    }

    fun sumFor(transactions: List<Transaction>, type: TransactionType, month: YearMonth): Double =
        transactions.filter { it.type == type && YearMonth.from(it.date) == month }.sumOf { it.amount }

    fun spendForCategoryMonth(transactions: List<Transaction>, category: String, month: YearMonth): Double =
        transactions.filter {
            it.type == TransactionType.EXPENSE && it.category == category && YearMonth.from(it.date) == month
        }.sumOf { it.amount }

    fun transferNetFor(transactions: List<Transaction>, month: YearMonth): Double =
        transactions.filter { it.type == TransactionType.TRANSFER && YearMonth.from(it.date) == month }
            .sumOf { if (it.direction == TransferDirection.IN) it.amount else -it.amount }

    /** Last [n] months ending at [end] (inclusive), oldest first. */
    fun lastNMonths(n: Int, end: YearMonth = YearMonth.now()): List<YearMonth> =
        (n - 1 downTo 0).map { end.minusMonths(it.toLong()) }

    fun budgetState(spent: Double, limit: Double): BudgetState {
        if (limit <= 0) return BudgetState.NONE
        if (spent >= limit) return BudgetState.OVER
        if (spent / limit >= 0.8) return BudgetState.WARN
        return BudgetState.OK
    }
}
