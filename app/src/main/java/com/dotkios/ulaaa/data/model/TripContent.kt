package com.dotkios.ulaaa.data.model

data class ChecklistItem(
    val id: String,
    val text: String,
    val isDone: Boolean,
)

data class TripMember(
    val id: String,
    val name: String,
)

data class Expense(
    val id: String,
    val title: String,
    val amount: Double,
    val paidBy: String,
)

/** One suggested transfer to settle up. */
data class Settlement(
    val from: String,
    val to: String,
    val amount: Double,
)

data class SplitResult(
    val total: Double,
    val perPerson: Double,
    val balances: Map<String, Double>,
    val settlements: List<Settlement>,
)
