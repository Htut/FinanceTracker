package com.financetracker.evolva.data.calc

import com.financetracker.evolva.data.model.Transaction
import com.financetracker.evolva.data.model.TransactionType
import com.financetracker.evolva.data.model.TransferDirection
import java.time.YearMonth

data class ForecastPoint(val month: YearMonth, val balance: Double)

data class ForecastResult(
    val pastMonths: List<YearMonth>,
    val pastCumulative: List<Double>,
    val futurePoints: List<ForecastPoint>,
    val netPerMonth: Double,
    val averages: Averages,
    val firstNegativeMonth: YearMonth?
)

object ForecastCalculator {

    /**
     * Ported from renderForecast() in the web app: replays the last 6
     * months of actual net cash flow, then projects [horizonMonths] more
     * months forward using the current monthly averages (income, expense,
     * savings, and net transfers).
     */
    fun compute(transactions: List<Transaction>, horizonMonths: Int): ForecastResult {
        val totals = FinanceCalculator.totals(transactions)
        val avg = FinanceCalculator.averages(totals, transactions)
        val netPerMonth = avg.income - avg.expense - avg.savings + avg.transferNet

        val pastMonths = FinanceCalculator.lastNMonths(6)
        val priorPast = transactions.filter { YearMonth.from(it.date) < pastMonths.first() }
        var cum = priorPast.sumOf { t ->
            when (t.type) {
                TransactionType.INCOME -> t.amount
                TransactionType.TRANSFER -> if (t.direction == TransferDirection.IN) t.amount else -t.amount
                else -> -t.amount // expense, savings both reduce spendable cash
            }
        }
        val pastCumulative = pastMonths.map { m ->
            cum += FinanceCalculator.sumFor(transactions, TransactionType.INCOME, m) -
                FinanceCalculator.sumFor(transactions, TransactionType.EXPENSE, m) -
                FinanceCalculator.sumFor(transactions, TransactionType.SAVINGS, m) +
                FinanceCalculator.transferNetFor(transactions, m)
            cum
        }

        var running = cum
        var firstNegative: YearMonth? = null
        val now = YearMonth.now()
        val futurePoints = (1..horizonMonths).map { i ->
            val m = now.plusMonths(i.toLong())
            running += netPerMonth
            if (running < 0 && firstNegative == null) firstNegative = m
            ForecastPoint(m, running)
        }

        return ForecastResult(pastMonths, pastCumulative, futurePoints, netPerMonth, avg, firstNegative)
    }
}
