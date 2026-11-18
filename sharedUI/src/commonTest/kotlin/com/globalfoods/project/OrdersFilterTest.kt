package com.globalfoods.project

import com.globalfoods.project.sharedlogic.network.auth.UserRole
import com.globalfoods.project.sharedlogic.network.orders.Client
import com.globalfoods.project.sharedlogic.network.orders.OrderResponse
import kotlin.test.Test
import kotlin.test.assertEquals

class OrdersFilterTest {
    private fun order(id: Int, client: String, status: Int, date: Int) =
        toOrder(OrderResponse(id = id, clientId = id, status = status, date = date, client = Client(id, client)))

    private val orders = listOf(
        order(1, "Marisquería Palo Seco", status = 4, date = 20261011),
        order(3, "Marisquería Palo Seco", status = 1, date = 20262011),
        order(4, "Luis Fernando Olivarria", status = 3, date = 20262011),
        order(7, "Cocina Doña Luz", status = 2, date = 20261811),
        order(14, "Restaurante El Faro", status = 5, date = 20261511)
    )

    @Test
    fun sortsFromTheNewestToTheOldest() {
        assertEquals(listOf(4, 3, 7, 14, 1), sortOrders(orders).map { it.id })
    }

    @Test
    fun sortsByCalendarDateAcrossMonthsInsteadOfTheNumericApiValue() {
        val acrossMonths = listOf(
            order(1, "Cliente", status = 1, date = 20263101),
            order(2, "Cliente", status = 1, date = 20260102),
            order(3, "Cliente", status = 1, date = 0)
        )
        assertEquals(listOf(2, 1, 3), sortOrders(acrossMonths).map { it.id })
    }

    @Test
    fun groupsCreatedSuppliedAndInDeliveryAsActive() {
        assertEquals(listOf(3, 4, 7), orders.filter(OrdersView.Active::includes).map { it.id })
        assertEquals(listOf(1), orders.filter(OrdersView.Delivered::includes).map { it.id })
        assertEquals(listOf(14), orders.filter(OrdersView.Cancelled::includes).map { it.id })
    }

    @Test
    fun keepsUnknownStatusesVisibleAsActive() {
        assertEquals(true, OrdersView.Active.includes(order(20, "Cliente", status = 9, date = 0)))
    }

    @Test
    fun searchesByClientNameOrExactOrderNumber() {
        assertEquals(listOf(1, 3), searchOrders(orders, "  palo ").map { it.id })
        assertEquals(listOf(4), searchOrders(orders, "#4").map { it.id })
        assertEquals(listOf(14), searchOrders(orders, "14").map { it.id })
        assertEquals(orders, searchOrders(orders, " "))
    }

    @Test
    fun showsTheTabsAllowedForEachRole() {
        assertEquals(listOf(MainTab.Inicio, MainTab.Pedidos, MainTab.Clientes), tabsFor(UserRole.Administrator))
        assertEquals(listOf(MainTab.Inicio, MainTab.Pedidos, MainTab.Clientes), tabsFor(UserRole.Employee))
        assertEquals(listOf(MainTab.Inicio, MainTab.Pedidos, MainTab.Sucursales), tabsFor(UserRole.Client))
        assertEquals(listOf(MainTab.Inicio), tabsFor(UserRole.Reports))
    }

    @Test
    fun formatsTheApiDateForTheCard() {
        assertEquals("Entrega 2026/20/11", orders[1].elapsed)
        assertEquals("Sin fecha", order(30, "Cliente", status = 1, date = 0).elapsed)
    }
}
