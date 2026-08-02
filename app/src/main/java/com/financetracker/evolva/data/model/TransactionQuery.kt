package com.financetracker.evolva.data.model

/**
 * Local list filters applied on top of the shared date filter.
 */
data class TransactionQuery(
    val search: String = "",
    val type: TransactionType? = null,
    val category: String? = null,
    val minAmount: Double? = null,
    val maxAmount: Double? = null
) {
    val isDefault: Boolean
        get() = search.isBlank() && type == null && category == null &&
            minAmount == null && maxAmount == null
}

fun List<Transaction>.filteredByQuery(query: TransactionQuery): List<Transaction> {
    if (query.isDefault) return this
    val needle = query.search.trim().lowercase()
    return filter { tx ->
        if (query.type != null && tx.type != query.type) return@filter false
        if (query.category != null && !tx.category.equals(query.category, ignoreCase = true)) {
            return@filter false
        }
        if (query.minAmount != null && tx.amount < query.minAmount) return@filter false
        if (query.maxAmount != null && tx.amount > query.maxAmount) return@filter false
        if (needle.isNotEmpty()) {
            val haystack = listOfNotNull(tx.category, tx.note, tx.type.name, tx.direction?.name)
                .joinToString(" ")
                .lowercase()
            if (!haystack.contains(needle)) return@filter false
        }
        true
    }
}
