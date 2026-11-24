package com.globalfoods.project

import com.globalfoods.project.sharedlogic.network.dashboard.ArticuloInventario
import com.globalfoods.project.sharedlogic.network.orders.OrderLineResponse
import com.globalfoods.project.sharedlogic.network.orders.OrderResponse
import kotlin.test.Test
import kotlin.test.assertEquals

class OrderTotalsTest {
    private val inventory = listOf(
        ArticuloInventario(codigoProducto = 101, talla = "Mediano", precio = 250.0, master = 0.0, cantidadKg = 100.0),
        ArticuloInventario(codigoProducto = 102, talla = "Grande", precio = 100.0, master = 0.0, cantidadKg = 100.0)
    )

    @Test
    fun totalIsOnlyTheSelectedProductsIncludingFractionalKilograms() {
        assertEquals(2650.0, orderProductsTotal(mapOf("Mediano" to "10.5", "Grande" to "0.25"), inventory))
        assertEquals(2625.0, orderProductsTotal(mapOf("Mediano" to "10.5"), inventory))
    }

    @Test
    fun emptyOrRemovedProductsDoNotAddAnyAmount() {
        assertEquals(0.0, orderProductsTotal(emptyMap(), inventory))
        assertEquals(0.0, orderProductsTotal(mapOf("Mediano" to "0", "Grande" to ""), inventory))
    }

    @Test
    fun editingAnOldOrderRecalculatesFromProductsWithoutRestoringObsoleteCharges() {
        val old = OrderResponse(
            id = 7, clientId = 12, branchId = 3, date = 20261120, totalKg = 5.0, total = 1330.0,
            notes = "Llamar\n[GlobalFoods:Entrega:v1]\n" +
                """{"clientName":"María","clientType":"Cliente registrado","method":"Dirección","address":"Calle 1","shippingCost":80}""",
            lines = listOf(OrderLineResponse(productCode = 101, size = "Mediano", quantity = 5.0, price = 250.0, subtotal = 1250.0))
        )
        val form = formFromOrder(old)
        assertEquals("Llamar", form.notes)
        assertEquals("María", form.client)
        assertEquals("Calle 1", form.address)
        assertEquals(1250.0, orderProductsTotal(form.quantities, editableInventory(inventory, old)))
    }
}
