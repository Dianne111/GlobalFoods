package com.globalfoods.project

import com.globalfoods.project.sharedlogic.network.auth.UserRole
import com.globalfoods.project.sharedlogic.network.dashboard.ArticuloInventario
import com.globalfoods.project.sharedlogic.network.orders.Branch
import com.globalfoods.project.sharedlogic.network.orders.OrderLineResponse
import com.globalfoods.project.sharedlogic.network.orders.OrderResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClientViewTest {
    private val responses = listOf(
        OrderResponse(id = 1, clientId = 7, status = 4, date = 20261105, totalKg = 10.0),
        OrderResponse(id = 2, clientId = 7, status = 1, date = 20261120, totalKg = 5.5),
        OrderResponse(id = 3, clientId = 8, status = 1, date = 20261120, totalKg = 3.0),
        OrderResponse(id = 4, clientId = 7, status = 5, date = 20261112, totalKg = 99.0),
        OrderResponse(id = 5, clientId = 7, status = 4, date = 20261030, totalKg = 20.0)
    )

    @Test
    fun aClientOnlySeesTheirOwnOrders() {
        assertEquals(listOf(1, 2, 4, 5), visibleToCurrentUser(responses, UserRole.Client, 7).map { it.id })
        assertEquals(emptyList(), visibleToCurrentUser(responses, UserRole.Client, null))
        assertEquals(responses, visibleToCurrentUser(responses, UserRole.Employee, null))
    }

    @Test
    fun summarizesTheCurrentMonth() {
        val orders = visibleToCurrentUser(responses, UserRole.Client, 7).map { toOrder(it) }

        val summary = clientMonthSummary(orders, yearMonth = 202611)

        assertEquals(1, summary.active)
        assertEquals(1, summary.deliveredThisMonth)
        // Cuenta noviembre sin cancelados: 10 + 5.5 (el de octubre y el cancelado no).
        assertEquals(15.5, summary.kgThisMonth)
    }

    @Test
    fun detectsWhenARequestedQuantityExceedsStock() {
        val products = listOf(
            ArticuloInventario(codigoProducto = 1, talla = "21/25", precio = 300.0, master = 0.0, cantidadKg = 10.0),
            ArticuloInventario(codigoProducto = 2, talla = "31/35", precio = 200.0, master = 0.0, cantidadKg = 4.0)
        )
        assertFalse(exceedsStock(mapOf("21/25" to "10", "31/35" to "4"), products))
        assertTrue(exceedsStock(mapOf("21/25" to "2", "31/35" to "4.5"), products))
    }

    @Test
    fun aClientsNewOrderIsAlwaysForThemselves() {
        val form = initialNewOrderForm(isClient = true, clientId = 7, userName = "Marisquería Palo Seco", template = null)

        assertEquals("Cliente registrado", form.clientType)
        assertEquals(7, form.clientId)
        assertEquals("Marisquería Palo Seco", form.client)
    }

    @Test
    fun repeatingAnOrderCopiesProductsButResetsTheDate() {
        val previous = OrderResponse(
            id = 9,
            clientId = 7,
            branchId = 3,
            date = 20250101,
            lines = listOf(OrderLineResponse(productCode = 1, size = "21/25", quantity = 2.0, price = 300.0))
        )

        val form = initialNewOrderForm(isClient = true, clientId = 7, userName = "Cliente", template = previous)

        assertEquals(mapOf("21/25" to "2"), form.quantities)
        assertEquals(3, form.branchId)
        assertEquals(NewOrderFormState().deliveryDate, form.deliveryDate)
    }

    @Test
    fun listsOnlyTheClientsBranchesWithActiveOnesFirst() {
        val branches = listOf(
            Branch(id = 1, clientId = 7, name = "Norte", status = 0),
            Branch(id = 2, clientId = 7, name = "centro"),
            Branch(id = 3, clientId = 8, name = "Ajena"),
            Branch(id = 4, clientId = 7, name = "Bodega")
        )
        assertEquals(listOf(4, 2, 1), sortBranches(branches, 7).map { it.id })
        assertEquals(emptyList(), sortBranches(branches, null))
    }

    @Test
    fun outOfStockMessageIsSpelledCorrectly() {
        assertEquals(
            "No se cuenta con disponibilidad de la cantidad solicitada. Favor de comunicarse con su asesor de ventas.",
            OUT_OF_STOCK_MESSAGE
        )
    }
}
