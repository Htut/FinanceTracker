package com.financetracker.evolva.data.locale

import android.content.Context
import com.financetracker.evolva.R
import com.financetracker.evolva.data.model.Account
import com.financetracker.evolva.data.model.AccountKind

/** Localized display for built-in wallet names; custom wallets keep their stored name. */
object AccountLabels {
    fun display(context: Context, account: Account): String = when (account.id) {
        Account.ID_CASH -> context.getString(R.string.kind_cash)
        Account.ID_BANK -> context.getString(R.string.kind_bank)
        Account.ID_EWALLET -> context.getString(R.string.kind_ewallet)
        else -> account.name
    }

    fun kindLabel(context: Context, kind: AccountKind): String = when (kind) {
        AccountKind.CASH -> context.getString(R.string.kind_cash)
        AccountKind.BANK -> context.getString(R.string.kind_bank)
        AccountKind.EWALLET -> context.getString(R.string.kind_ewallet)
    }
}
