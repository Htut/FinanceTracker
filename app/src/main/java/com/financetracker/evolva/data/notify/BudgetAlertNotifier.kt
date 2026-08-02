package com.financetracker.evolva.data.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.financetracker.evolva.MainActivity
import com.financetracker.evolva.R
import com.financetracker.evolva.data.calc.BudgetState
import com.financetracker.evolva.data.calc.FinanceCalculator
import com.financetracker.evolva.data.locale.CategoryLabels
import com.financetracker.evolva.data.model.AppCurrency
import com.financetracker.evolva.data.model.Budget
import com.financetracker.evolva.data.model.Transaction
import com.financetracker.evolva.data.model.formatAmount
import java.time.YearMonth

data class BudgetAlert(
    val category: String,
    val spent: Double,
    val limit: Double,
    val state: BudgetState
)

object BudgetAlertNotifier {
    const val CHANNEL_ID = "budget_alerts"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notif_channel_budget),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notif_channel_budget_desc)
        }
        manager.createNotificationChannel(channel)
    }

    fun collectAlerts(
        transactions: List<Transaction>,
        budgets: List<Budget>,
        month: YearMonth = YearMonth.now()
    ): List<BudgetAlert> =
        budgets.mapNotNull { budget ->
            val spent = FinanceCalculator.spendForCategoryMonth(transactions, budget.category, month)
            val state = FinanceCalculator.budgetState(spent, budget.limit)
            if (state == BudgetState.WARN || state == BudgetState.OVER) {
                BudgetAlert(budget.category, spent, budget.limit, state)
            } else null
        }

    fun notifyAlerts(
        context: Context,
        alerts: List<BudgetAlert>,
        currency: AppCurrency
    ) {
        if (alerts.isEmpty()) return
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        ensureChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alerts.forEach { alert ->
            val categoryLabel = CategoryLabels.display(context, alert.category)
            val title = when (alert.state) {
                BudgetState.OVER -> context.getString(R.string.notif_over_budget, categoryLabel)
                else -> context.getString(R.string.notif_near_limit, categoryLabel)
            }
            val text = context.getString(
                R.string.notif_budget_spent,
                formatAmount(alert.spent, currency),
                formatAmount(alert.limit, currency)
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(pending)
                .setAutoCancel(true)
                .setPriority(
                    if (alert.state == BudgetState.OVER) NotificationCompat.PRIORITY_HIGH
                    else NotificationCompat.PRIORITY_DEFAULT
                )
                .build()

            try {
                NotificationManagerCompat.from(context).notify(
                    alert.category.hashCode(),
                    notification
                )
            } catch (_: SecurityException) {
                // POST_NOTIFICATIONS denied on Android 13+
            }
        }
    }
}
