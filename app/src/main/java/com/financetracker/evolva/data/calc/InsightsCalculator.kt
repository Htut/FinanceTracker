package com.financetracker.evolva.data.calc

import com.financetracker.evolva.data.model.Budget
import com.financetracker.evolva.data.model.RecurringRule
import com.financetracker.evolva.data.model.Transaction
import com.financetracker.evolva.data.model.TransactionType
import java.time.YearMonth
import kotlin.math.abs

/** Ported 1:1 from renderInsights() in the web app. */
object InsightsCalculator {

    fun build(
        transactions: List<Transaction>,
        budgets: List<Budget>,
        recurringRules: List<RecurringRule>,
        fmt: (Double) -> String
    ): List<String> {
        if (transactions.isEmpty()) {
            return listOf("Add your first income, expense, or savings entry to see insights here.")
        }

        val items = mutableListOf<String>()
        val totals = FinanceCalculator.totals(transactions)
        val avg = FinanceCalculator.averages(totals, transactions)

        if (avg.months > 0) {
            items.add(
                "On average you earn ${fmt(avg.income)} and spend ${fmt(avg.expense)} per month " +
                    "(across ${avg.months} month${if (avg.months == 1) "" else "s"} of data)."
            )
        }

        if (totals.income > 0) {
            val rate = (totals.savings / totals.income) * 100
            items.add("You're saving ${"%.1f".format(rate)}% of your income.")
        }

        val byCategory = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }
        byCategory.entries.maxByOrNull { it.value }?.let { (cat, amount) ->
            items.add("Your biggest expense category is $cat at ${fmt(amount)}.")
        }

        val lastTwoMonths = FinanceCalculator.lastNMonths(2)
        val lastMonthSpend = FinanceCalculator.sumFor(transactions, TransactionType.EXPENSE, lastTwoMonths[0])
        val thisMonthSpend = FinanceCalculator.sumFor(transactions, TransactionType.EXPENSE, lastTwoMonths[1])
        if (lastMonthSpend > 0) {
            val change = ((thisMonthSpend - lastMonthSpend) / lastMonthSpend) * 100
            if (abs(change) >= 1) {
                items.add("Spending is ${if (change > 0) "up" else "down"} ${"%.1f".format(abs(change))}% vs. last month.")
            }
        }

        items.add(
            if (totals.net < 0) "You're spending more than you earn this period — net balance is ${fmt(totals.net)}."
            else "Net balance is positive at ${fmt(totals.net)}."
        )

        if (totals.transferIn > 0 || totals.transferOut > 0) {
            val netText = (if (totals.transferNet >= 0) "+" else "") + fmt(totals.transferNet)
            items.add(
                "You've sent ${fmt(totals.transferOut)} and received ${fmt(totals.transferIn)} in transfers " +
                    "with family/friends (net $netText)."
            )
        }

        val activeRecurring = recurringRules.count { it.active }
        if (activeRecurring > 0) {
            items.add("You have $activeRecurring recurring transaction${if (activeRecurring == 1) "" else "s"} set up to auto-post each month.")
        }

        val currentMonth = YearMonth.now()
        data class BudgetStatus(val category: String, val limit: Double, val spent: Double)
        val withBudget = budgets.filter { it.limit > 0 }.map {
            BudgetStatus(it.category, it.limit, FinanceCalculator.spendForCategoryMonth(transactions, it.category, currentMonth))
        }
        val over = withBudget.filter { it.spent >= it.limit }
        val near = withBudget.filter { it.spent < it.limit && it.spent / it.limit >= 0.8 }
        when {
            over.size == 1 -> items.add("You're over budget on ${over[0].category} this month — ${fmt(over[0].spent)} of ${fmt(over[0].limit)}.")
            over.size > 1 -> items.add("You're over budget on ${over.size} categories this month: ${over.joinToString(", ") { it.category }}.")
            near.isNotEmpty() -> items.add("You're close to your ${near[0].category} budget this month — ${fmt(near[0].spent)} of ${fmt(near[0].limit)}.")
        }

        return items
    }
}
