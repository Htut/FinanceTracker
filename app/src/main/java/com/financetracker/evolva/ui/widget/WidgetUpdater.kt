package com.financetracker.evolva.ui.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Push a refresh to every pinned [BudgetGlanceWidget]. */
suspend fun refreshBudgetWidgets(context: Context) {
    withContext(Dispatchers.Default) {
        BudgetGlanceWidget().updateAll(context.applicationContext)
    }
}
