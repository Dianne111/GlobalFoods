package com.globalfoods.project

import com.globalfoods.project.sharedlogic.network.dashboard.ArticuloInventario
import com.globalfoods.project.sharedlogic.network.orders.Client
import com.globalfoods.project.sharedlogic.network.orders.OrderDeliveryInfo
import com.globalfoods.project.sharedlogic.network.orders.OrderLineResponse
import com.globalfoods.project.sharedlogic.network.orders.OrderObservations
import com.globalfoods.project.sharedlogic.network.orders.OrderResponse
import kotlin.test.Test
import kotlin.test.assertEquals

class EditOrderFormTest {
    @Test
    fun rebuildsTheFormOfARegisteredClientOrder() {
        val order = OrderResponse(
            id = 10,
            clientId = 42,
            branchId = 7,
            date = 20261120,
            status = 1,
            notes = OrderObservations(
                notes = "Tocar el timbre",
                delivery = OrderDeliveryInfo(
                    clientName = "María López",
                    clientType = "Cliente registrado",
                    method = "Dirección",
                    address = "Av. Juárez 120",
                    phone = "3312345678",
                    branchName = "Sucursal Centro",
                    references = "Portón negro",
                    shippingCost = 50.0
                )
            ).encode(),
            lines = listOf(
                OrderLineResponse(productCode = 1, size = "21/25", quantity = 2.5, price = 300.0, subtotal = 750.0),
                OrderLineResponse(productCode = 2, size = "U-15", quantity = 1.0, price = 450.0, subtotal = 450.0)
            ),
            client = Client(id = 42, name = "María López")
        )

        val form = formFromOrder(order)

        assertEquals(mapOf("21/25" to "2.5", "U-15" to "1"), form.quantities)
        assertEquals(42, form.clientId)
        assertEquals(7, form.branchId)
        assertEquals("María López", form.client)
        assertEquals("Cliente registrado", form.clientType)
        assertEquals("3312345678", form.phone)
        assertEquals("Sucursal Centro", form.branch)
        assertEquals("Av. Juárez 120", form.address)
        assertEquals("20/11/2026", form.deliveryDate)
        assertEquals("Tocar el timbre", form.notes)
        assertEquals("50", form.shipping)
        assertEquals("Dirección", form.deliveryMode)
        assertEquals("Portón negro", form.references)
    }

    @Test
    fun splitsTheAddressOfAWalkInClientIntoItsFields() {
        val order = OrderResponse(
            id = 11,
            date = 20261121,
            notes = OrderObservations(
                notes = "",
                delivery = OrderDeliveryInfo(
                    clientName = "Juan Pérez",
                    clientType = "Cliente esporádico",
                    method = "Dirección",
                    address = "Calle 5 #12, Col. Americana, C.P. 44160"
                )
            ).encode()
        )

        val form = formFromOrder(order)

        assertEquals("Calle 5 #12", form.street)
        assertEquals("Col. Americana", form.neighborhood)
        assertEquals("44160", form.postalCode)
        assertEquals(null, form.clientId)
    }

    @Test
    fun keepsTheAgreedPriceAndAllowsTheOrderedQuantity() {
        val inventory = listOf(
            ArticuloInventario(codigoProducto = 1, talla = "21/25", precio = 320.0, master = 0.0, cantidadKg = 1.0),
            ArticuloInventario(codigoProducto = 3, talla = "31/35", precio = 200.0, master = 0.0, cantidadKg = 10.0)
        )
        val order = OrderResponse(
            id = 12,
            lines = listOf(
                OrderLineResponse(productCode = 1, size = "21/25", quantity = 2.5, price = 300.0),
                OrderLineResponse(productCode = 9, size = "Agotada", quantity = 4.0, price = 150.0)
            )
        )

        val result = editableInventory(inventory, order).associateBy { it.talla }

        assertEquals(300.0, result.getValue("21/25").precio)
        assertEquals(2.5, result.getValue("21/25").cantidadKg)
        assertEquals(200.0, result.getValue("31/35").precio)
        assertEquals(9, result.getValue("Agotada").codigoProducto)
        assertEquals(4.0, result.getValue("Agotada").cantidadKg)
    }
}
