package com.financetracker.evolva.data.model

import java.time.LocalTime

/** Shared Activity / detail-dialog sort options. */
enum class TransactionSort {
    DATE_DESC,
    DATE_ASC,
    AMOUNT_DESC,
    AMOUNT_ASC
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
