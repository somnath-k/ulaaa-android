package com.dotkios.ulaaa.domain

import com.dotkios.ulaaa.data.model.Expense
import com.dotkios.ulaaa.data.model.Settlement
import com.dotkios.ulaaa.data.model.SplitResult
import kotlin.math.abs
import kotlin.math.min

/** Splits trip expenses equally among members and computes a minimal set of settle-up transfers. */
object SplitCalculator {

    private const val EPSILON = 0.01

    fun calculate(members: List<String>, expenses: List<Expense>): SplitResult {
        if (members.isEmpty()) {
            return SplitResult(total = 0.0, perPerson = 0.0, balances = emptyMap(), settlements = emptyList())
        }

        val total = expenses.sumOf { it.amount }
        val perPerson = total / members.size

        // Net balance per member: what they paid minus their equal share.
        val balances = members.associateWith { name ->
            val paid = expenses.filter { it.paidBy == name }.sumOf { it.amount }
            round(paid - perPerson)
        }

        return SplitResult(
            total = round(total),
            perPerson = round(perPerson),
            balances = balances,
            settlements = settle(balances),
        )
    }

    /** Greedy min-transfers: repeatedly match the biggest debtor with the biggest creditor. */
    private fun settle(balances: Map<String, Double>): List<Settlement> {
        val creditors = balances.filter { it.value > EPSILON }
            .map { it.key to it.value }.toMutableList()
        val debtors = balances.filter { it.value < -EPSILON }
            .map { it.key to -it.value }.toMutableList()

        val settlements = mutableListOf<Settlement>()
        creditors.sortByDescending { it.second }
        debtors.sortByDescending { it.second }

        var ci = 0
        var di = 0
        while (ci < creditors.size && di < debtors.size) {
            val (creditor, credit) = creditors[ci]
            val (debtor, debt) = debtors[di]
            val transfer = round(min(credit, debt))
            if (transfer > EPSILON) {
                settlements += Settlement(from = debtor, to = creditor, amount = transfer)
            }
            val remainingCredit = credit - transfer
            val remainingDebt = debt - transfer
            creditors[ci] = creditor to remainingCredit
            debtors[di] = debtor to remainingDebt
            if (remainingCredit <= EPSILON) ci++
            if (remainingDebt <= EPSILON) di++
        }
        return settlements
    }

    private fun round(value: Double): Double = Math.round(value * 100.0) / 100.0

    fun isBalanced(a: Double, b: Double): Boolean = abs(a - b) < EPSILON
}
