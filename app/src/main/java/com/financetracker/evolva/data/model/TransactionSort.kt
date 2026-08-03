package com.financetracker.evolva.data.model

import java.time.LocalTime

/** Shared Activity / detail-dialog sort options. */
enum class TransactionSort {
    DATE_DESC,
    DATE_ASC,
    AMOUNT_DESC,
    AMOUNT_ASC;

    val isDate: Boolean get() = this == DATE_DESC || this == DATE_ASC
    val isAmount: Boolean get() = this == AMOUNT_DESC || this == AMOUNT_ASC
    val arrow: String get() = if (this == DATE_DESC || this == AMOUNT_DESC) "↓" else "↑"

    /** Tap Date: select date (newest first) or flip direction if already on date. */
    fun onDateClick(): TransactionSort = when (this) {
        DATE_DESC -> DATE_ASC
        DATE_ASC -> DATE_DESC
        else -> DATE_DESC
    }

    /** Tap Amount: select amount (high first) or flip direction if already on amount. */
    fun onAmountClick(): TransactionSort = when (this) {
        AMOUNT_DESC -> AMOUNT_ASC
        AMOUNT_ASC -> AMOUNT_DESC
        else -> AMOUNT_DESC
    }
}

fun List<Transaction>.applySort(sort: TransactionSort): List<Transaction> = when (sort) {
    TransactionSort.DATE_DESC -> sortedWith(
        compareByDescending<Transaction> { it.date }
            .thenByDescending { it.time ?: LocalTime.MIN }
    )
    TransactionSort.DATE_ASC -> sortedWith(
        compareBy<Transaction> { it.date }
            .thenBy { it.time ?: LocalTime.MIN }
    )
    TransactionSort.AMOUNT_DESC -> sortedWith(
        compareByDescending<Transaction> { it.homeAmount() }
            .thenByDescending { it.date }
    )
    TransactionSort.AMOUNT_ASC -> sortedWith(
        compareBy<Transaction> { it.homeAmount() }
            .thenByDescending { it.date }
    )
}
