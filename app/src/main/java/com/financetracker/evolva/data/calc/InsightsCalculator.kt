package com.financetracker.evolva.data.calc

import android.content.Context
import com.financetracker.evolva.R
import com.financetracker.evolva.data.locale.CategoryLabels
import com.financetracker.evolva.data.model.Budget
import com.financetracker.evolva.data.model.RecurringRule
import com.financetracker.evolva.data.model.Transaction
import com.financetracker.evolva.data.model.TransactionType
import com.financetracker.evolva.data.model.homeAmount
import java.time.YearMonth
import kotlin.math.abs

/** Ported 1:1 from renderInsights() in the web app. */
object InsightsCalculator {

    fun build(
        context: Context,
        transactions: List<Transaction>,
        budgets: List<Budget>,
        recurringRules: List<RecurringRule>,
        fmt: (Double) -> String
    ): List<String> {
        if (transactions.isEmpty()) {
            return listOf(context.getString(R.string.insight_empty))
        }

        val items = mutableListOf<String>()
        val totals = FinanceCalculator.totals(transactions)
        val avg = FinanceCalculator.averages(totals, transactions)

        if (avg.months > 0) {
            items.add(
                context.getString(
                    R.string.insight_avg,
                    fmt(avg.income),
                    fmt(avg.expense),
                    avg.months
                )
            )
        }

        if (totals.income > 0) {
            val rate = (totals.savings / totals.income) * 100
            items.add(
                context.getString(R.string.insight_saving_rate, "%.1f".format(rate))
            )
        }

        val byCategory = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { (_, txs) -> txs.sumOf { it.homeAmount() } }
        byCategory.entries.maxByOrNull { it.value }?.let { (cat, amount) ->
            items.add(
                context.getString(
                    R.string.insight_biggest_expense,
                    CategoryLabels.display(context, cat),
                    fmt(amount)
                )
            )
        }

        val lastTwoMonths = FinanceCalculator.lastNMonths(2)
        val lastMonthSpend = FinanceCalculator.sumFor(transactions, TransactionType.EXPENSE, lastTwoMonths[0])
        val thisMonthSpend = FinanceCalculator.sumFor(transactions, TransactionType.EXPENSE, lastTwoMonths[1])
        if (lastMonthSpend > 0) {
            val change = ((thisMonthSpend - lastMonthSpend) / lastMonthSpend) * 100
            if (abs(change) >= 1) {
                val pct = "%.1f".format(abs(change))
                items.add(
                    context.getString(
                        if (change > 0) R.string.insight_spend_up else R.string.insight_spend_down,
                        pct
                    )
                )
            }
        }

        items.add(
            if (totals.net < 0) {
                context.getString(R.string.insight_net_negative, fmt(totals.net))
            } else {
                context.getString(R.string.insight_net_positive, fmt(totals.net))
            }
        )

        if (totals.transferIn > 0 || totals.transferOut > 0) {
            val netText = (if (totals.transferNet >= 0) "+" else "") + fmt(totals.transferNet)
            items.add(
                context.getString(
                    R.string.insight_transfers,
                    fmt(totals.transferOut),
                    fmt(totals.transferIn),
                    netText
                )
            )
        }

        val activeRecurring = recurringRules.count { it.active }
        if (activeRecurring > 0) {
            items.add(context.getString(R.string.insight_recurring, activeRecurring))
        }

        val currentMonth = YearMonth.now()
        data class BudgetStatus(val category: String, val limit: Double, val spent: Double)
        val withBudget = budgets.filter { it.limit > 0 }.map {
            BudgetStatus(
                it.category,
                it.limit,
                FinanceCalculator.spendForCategoryMonth(transactions, it.category, currentMonth)
            )
        }
        val over = withBudget.filter { it.spent >= it.limit }
        val near = withBudget.filter { it.spent < it.limit && it.spent / it.limit >= 0.8 }
        when {
            over.size == 1 -> items.add(
                context.getString(
                    R.string.insight_over_one,
                    CategoryLabels.display(context, over[0].category),
                    fmt(over[0].spent),
                    fmt(over[0].limit)
                )
            )
            over.size > 1 -> items.add(
                context.getString(
                    R.string.insight_over_many,
                    over.size,
                    over.joinToString(", ") { CategoryLabels.display(context, it.category) }
                )
            )
            near.isNotEmpty() -> items.add(
                context.getString(
                    R.string.insight_near,
                    CategoryLabels.display(context, near[0].category),
                    fmt(near[0].spent),
                    fmt(near[0].limit)
                )
            )
        }

        return items
    }
}
