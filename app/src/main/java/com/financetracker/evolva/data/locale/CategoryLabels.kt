package com.financetracker.evolva.data.locale

import android.content.Context
import com.financetracker.evolva.R
import com.financetracker.evolva.data.model.TransactionType

/** Maps built-in English category storage keys to localized display labels. */
object CategoryLabels {
    private val map = mapOf(
        "Salary" to R.string.cat_salary,
        "Freelance" to R.string.cat_freelance,
        "Investment" to R.string.cat_investment,
        "Gift" to R.string.cat_gift,
        "Other" to R.string.cat_other,
        "Food" to R.string.cat_food,
        "Transport" to R.string.cat_transport,
        "Housing" to R.string.cat_housing,
        "Education" to R.string.cat_education,
        "Utilities" to R.string.cat_utilities,
        "Insurance" to R.string.cat_insurance,
        "Entertainment" to R.string.cat_entertainment,
        "Subscriptions" to R.string.cat_subscriptions,
        "Health" to R.string.cat_health,
        "Personal Care" to R.string.cat_personal_care,
        "Shopping" to R.string.cat_shopping,
        "Clothing" to R.string.cat_clothing,
        "Travel" to R.string.cat_travel,
        "Gifts & Donations" to R.string.cat_gifts_donations,
        "Emergency Fund" to R.string.cat_emergency_fund,
        "Retirement" to R.string.cat_retirement,
        "Goal" to R.string.cat_goal,
        "Parent" to R.string.cat_parent,
        "Spouse" to R.string.cat_spouse,
        "Sibling" to R.string.cat_sibling,
        "Child" to R.string.cat_child,
        "Relative" to R.string.cat_relative,
        "Friend" to R.string.cat_friend
    )

    fun display(context: Context, storedName: String): String {
        val res = map[storedName] ?: return storedName
        return context.getString(res)
    }

    fun typeLabel(context: Context, type: TransactionType): String = when (type) {
        TransactionType.INCOME -> context.getString(R.string.label_income)
        TransactionType.EXPENSE -> context.getString(R.string.label_expense)
        TransactionType.SAVINGS -> context.getString(R.string.label_savings)
        TransactionType.TRANSFER -> context.getString(R.string.label_transfer)
    }
}
