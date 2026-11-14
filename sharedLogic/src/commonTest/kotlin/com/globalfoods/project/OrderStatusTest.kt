package com.globalfoods.project

import com.globalfoods.project.sharedlogic.network.orders.OrderResponse
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
    fun onlyCreatedOrdersAreEditableAndOnlyDeliveredOnesAcceptPayment() {
        assertEquals(listOf(OrderStatus.Created), OrderStatus.entries.filter { it.isEditable })
        assertEquals(listOf(OrderStatus.Delivered), OrderStatus.entries.filter { it.acceptsPayment })
        assertFalse(OrderStatus.Cancelled in OrderStatus.flow)
        assertTrue(OrderStatus.flow.zipWithNext().all { (a, b) -> a.code < b.code })
    }
}
