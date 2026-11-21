package com.globalfoods.project

import com.globalfoods.project.sharedlogic.network.orders.OrderLineResponse
import com.globalfoods.project.sharedlogic.network.orders.OrderPaymentMethod
import com.globalfoods.project.sharedlogic.network.orders.OrderResponse
import com.globalfoods.project.sharedlogic.network.orders.paymentRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OrderPaymentTest {
    private val original = OrderResponse(
        id = 7, clientId = 12, branchId = 3, date = 20261120, paymentMethod = 0, status = 3,
        totalKg = 5.0, total = 1250.0, notes = "Conservar las observaciones originales",
        lines = listOf(OrderLineResponse(id = 40, productCode = 101, size = "Mediano", quantity = 5.0, price = 250.0, subtotal = 1250.0))
    )

    @Test
    fun bothMethodsUpdateOnlyTheDateAndPayment() {
        for ((method, code) in listOf(OrderPaymentMethod.Cash to 1, OrderPaymentMethod.CardOrTransfer to 2)) {
            val request = paymentRequest(original, method, 20261121)
            assertEquals(code, request.paymentMethod)
            assertEquals(20261121, request.date)
            assertEquals(3, request.status)
            assertEquals(12, request.clientId)
            assertEquals(3, request.branchId)
            assertEquals(5.0, request.totalKg)
            assertEquals(1250.0, request.total)
            assertEquals(original.notes, request.notes)
            assertEquals(101, request.lines.single().productCode)
            assertEquals("Mediano", request.lines.single().size)
            assertEquals(5.0, request.lines.single().quantity)
            assertEquals(250.0, request.lines.single().price)
            assertEquals(1250.0, request.lines.single().subtotal)
        }
    }

    @Test
    fun permitsAnotherSelectionAndDateWhileTheOrderRemainsInDelivery() {
        val request = paymentRequest(original.copy(paymentMethod = 1), OrderPaymentMethod.CardOrTransfer, 20261122)
        assertEquals(2, request.paymentMethod)
        assertEquals(20261122, request.date)
        assertEquals(3, request.status)
    }

    @Test
    fun refusesOtherStatesAndImpossibleDates() {
        for (status in listOf(1, 2, 4, 5)) {
            assertFailsWith<IllegalArgumentException> {
                paymentRequest(original.copy(status = status), OrderPaymentMethod.Cash, 20261121)
            }
        }
        assertFailsWith<IllegalArgumentException> { paymentRequest(original, OrderPaymentMethod.Cash, 20260230) }
        assertEquals(20240229, paymentRequest(original, OrderPaymentMethod.Cash, 20240229).date)
    }
}
