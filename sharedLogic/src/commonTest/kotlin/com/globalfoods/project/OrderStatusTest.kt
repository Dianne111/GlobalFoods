package com.globalfoods.project

import com.globalfoods.project.sharedlogic.network.orders.OrderLineRequest
import com.globalfoods.project.sharedlogic.network.orders.OrderLineResponse
import com.globalfoods.project.sharedlogic.network.orders.OrderResponse
import com.globalfoods.project.sharedlogic.network.orders.cancellationRequest
import com.globalfoods.project.sharedlogic.network.orders.OrderStatus
import com.globalfoods.project.sharedlogic.network.orders.orderStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OrderStatusTest {
    @Test
    fun mapsTheApiCodesToTheirStatus() {
        assertEquals(OrderStatus.Created, OrderStatus.fromCode(1))
        assertEquals(OrderStatus.Supplied, OrderStatus.fromCode(2))
        assertEquals(OrderStatus.InDelivery, OrderStatus.fromCode(3))
        assertEquals(OrderStatus.Delivered, OrderStatus.fromCode(4))
        assertEquals(OrderStatus.Cancelled, OrderStatus.fromCode(5))
        assertNull(OrderStatus.fromCode(9))
        assertEquals(OrderStatus.Delivered, OrderResponse(id = 1, status = 4).orderStatus)
    }

    @Test
    fun ordersCanBeCancelledUntilTheyAreDelivered() {
        assertEquals(
            listOf(OrderStatus.Created, OrderStatus.Supplied, OrderStatus.InDelivery),
            OrderStatus.entries.filter { it.isCancellable }
        )
    }

    @Test
    fun cancellingKeepsTheOrderAndOnlyChangesTheStatus() {
        val order = OrderResponse(
            id = 12,
            clientId = 7,
            branchId = 3,
            date = 20261120,
            totalKg = 2.5,
            total = 750.0,
            status = 2,
            notes = "Tocar el timbre",
            lines = listOf(OrderLineResponse(id = 1, productCode = 4, size = "21/25", quantity = 2.5, price = 300.0, subtotal = 750.0))
        )

        val request = cancellationRequest(order)

        assertEquals(5, request.status)
        assertEquals(7, request.clientId)
        assertEquals(3, request.branchId)
        assertEquals(20261120, request.date)
        assertEquals(750.0, request.total)
        assertEquals("Tocar el timbre", request.notes)
        assertEquals(listOf(OrderLineRequest(4, "21/25", 2.5, 300.0, 750.0)), request.lines)
    }

    @Test
    fun onlyCreatedOrdersAreEditableAndOnlyDeliveredOnesAcceptPayment() {
        assertEquals(listOf(OrderStatus.Created), OrderStatus.entries.filter { it.isEditable })
        assertEquals(listOf(OrderStatus.Delivered), OrderStatus.entries.filter { it.acceptsPayment })
        assertFalse(OrderStatus.Cancelled in OrderStatus.flow)
        assertTrue(OrderStatus.flow.zipWithNext().all { (a, b) -> a.code < b.code })
    }
}
