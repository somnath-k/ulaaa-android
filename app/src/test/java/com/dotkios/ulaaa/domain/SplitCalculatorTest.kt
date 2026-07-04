package com.dotkios.ulaaa.domain

import com.dotkios.ulaaa.data.model.Expense
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SplitCalculatorTest {

    private fun expense(title: String, amount: Double, paidBy: String) =
        Expense(id = title, title = title, amount = amount, paidBy = paidBy)

    @Test
    fun `single payer is owed by everyone else equally`() {
        val result = SplitCalculator.calculate(
            members = listOf("Asha", "Bala"),
            expenses = listOf(expense("Hotel", 100.0, "Asha")),
        )

        assertEquals(100.0, result.total, 0.001)
        assertEquals(50.0, result.perPerson, 0.001)
        assertEquals(50.0, result.balances["Asha"]!!, 0.001)
        assertEquals(-50.0, result.balances["Bala"]!!, 0.001)

        assertEquals(1, result.settlements.size)
        val transfer = result.settlements.first()
        assertEquals("Bala", transfer.from)
        assertEquals("Asha", transfer.to)
        assertEquals(50.0, transfer.amount, 0.001)
    }

    @Test
    fun `equal payments settle to nothing`() {
        val result = SplitCalculator.calculate(
            members = listOf("A", "B"),
            expenses = listOf(
                expense("Lunch", 40.0, "A"),
                expense("Cab", 40.0, "B"),
            ),
        )

        assertTrue(result.settlements.isEmpty())
        assertEquals(0.0, result.balances["A"]!!, 0.001)
        assertEquals(0.0, result.balances["B"]!!, 0.001)
    }

    @Test
    fun `three members with one payer produces two transfers`() {
        val result = SplitCalculator.calculate(
            members = listOf("A", "B", "C"),
            expenses = listOf(expense("Villa", 300.0, "A")),
        )

        assertEquals(100.0, result.perPerson, 0.001)
        assertEquals(2, result.settlements.size)
        assertTrue(result.settlements.all { it.to == "A" })
        assertEquals(200.0, result.settlements.sumOf { it.amount }, 0.001)
    }

    @Test
    fun `no members yields an empty result`() {
        val result = SplitCalculator.calculate(emptyList(), listOf(expense("x", 10.0, "ghost")))
        assertTrue(result.balances.isEmpty())
        assertTrue(result.settlements.isEmpty())
    }
}
