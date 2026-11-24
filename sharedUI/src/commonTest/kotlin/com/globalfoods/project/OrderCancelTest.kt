package com.globalfoods.project

import com.globalfoods.project.sharedlogic.network.orders.OrderResponse
import kotlin.test.Test
import kotlin.test.assertEquals

class OrderCancelTest {
    @Test
    fun cancelTextsFollowRfPed03() {
        assertEquals(
            "¿Está seguro de que desea cancelar el pedido #22? Esta acción no se puede deshacer.",
            cancelConfirmationText("#22")
        )
        assertEquals("Pedido #22 cancelado.", cancelledMessage("#22"))
        assertEquals("Almacén ya surtió este pedido.", SUPPLIED_CANCEL_WARNING)
    }

    @Test
    fun ordersInDeliveryStillBlockDeactivation() {
        val orders = listOf(OrderResponse(id = 30, clientId = 4, branchId = 9, status = 3))
        assertEquals(listOf(30), pendingDeliveryOrders(orders, clientId = 4).map { it.id })
        assertEquals(listOf(30), pendingBranchOrders(orders, branchId = 9).map { it.id })
    }
}
