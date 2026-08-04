package com.financetracker.evolva.ui.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.financetracker.evolva.FinanceApp
import com.financetracker.evolva.R
import com.financetracker.evolva.data.calc.FinanceCalculator
import com.financetracker.evolva.data.model.formatAmount
import kotlinx.coroutines.flow.first
import java.time.YearMonth

class BudgetGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as FinanceApp
        val transactions = app.repository.transactions.first()
        val budgets = app.repository.budgets.first()
        val currency = app.profileSession.currency.first()
        val month = YearMonth.now()
        val expense = FinanceCalculator.sumFor(
            transactions,
            com.financetracker.evolva.data.model.TransactionType.EXPENSE,
            month
        )
        val remaining = FinanceCalculator.totalBudgetRemaining(transactions, budgets, month)
        val appName = context.getString(R.string.app_name)
        val spentLabel = context.getString(R.string.widget_month_spend)
        val remainingLabel = context.getString(R.string.widget_budget_remaining)

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(
                        ColorProvider(
                            day = Color(0xFFF8FAFC),
                            night = Color(0xFF0F172A)
                        )
                    )
                    .padding(16.dp)
            ) {
                Text(
                    text = appName,
                    style = TextStyle(
                        color = ColorProvider(
                            day = Color(0xFF0F172A),
                            night = Color(0xFFF8FAFC)
                        ),
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(GlanceModifier.height(12.dp))
                Text(
                    text = spentLabel,
                    style = TextStyle(
                        color = ColorProvider(
                            day = Color(0xFF64748B),
                            night = Color(0xFF94A3B8)
                        )
                    )
                )
                Text(
                    text = formatAmount(expense, currency),
                    style = TextStyle(
                        color = ColorProvider(
                            day = Color(0xFFDC2626),
                            night = Color(0xFFF87171)
                        ),
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(GlanceModifier.height(8.dp))
                Text(
                    text = remainingLabel,
                    style = TextStyle(
                        color = ColorProvider(
                            day = Color(0xFF64748B),
                            night = Color(0xFF94A3B8)
                        )
                    )
                )
                Text(
                    text = formatAmount(remaining, currency),
                    style = TextStyle(
                        color = ColorProvider(
                            day = Color(0xFF16A34A),
                            night = Color(0xFF4ADE80)
                        ),
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

class BudgetWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BudgetGlanceWidget()
}
