package com.globalfoods.project

import com.globalfoods.project.sharedlogic.network.orders.Client
import com.globalfoods.project.sharedlogic.network.orders.OrderDeliveryInfo
import com.globalfoods.project.sharedlogic.network.orders.OrderLineRequest
import com.globalfoods.project.sharedlogic.network.orders.OrderLineResponse
import com.globalfoods.project.sharedlogic.network.orders.OrderObservations
import com.globalfoods.project.sharedlogic.network.orders.OrderRequest
import com.globalfoods.project.sharedlogic.network.orders.OrderResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RegisteredOrderNotesTest {
    @Test
    fun registeredOrderSendsOnlyTheUsersNotesAndTheRelatedIds() {
        val form = NewOrderFormState(
            clientType = "Cliente registrado", clientId = 12, client = "Marisquería",
            branchId = 3, branch = "Las Quintas", address = "Lago de Chapala 1298",
            phone = "6670000001", notes = "Dejar con recepción.\nLlamar antes de llegar."
        )
        val request = OrderRequest(
            clientId = form.clientId, branchId = form.branchId, date = 20261121,
            totalKg = 5.0, total = 1250.0, status = 1,
            notes = form.observationsForApi(),
            lines = listOf(OrderLineRequest(101, "Mediano", 5.0, 250.0, 1250.0))
        )
        val json = Json.encodeToJsonElement(OrderRequest.serializer(), request).jsonObject

        assertEquals(form.notes, json.getValue("Observaciones").jsonPrimitive.content)
        assertEquals("12", json.getValue("IdCliente").jsonPrimitive.content)
        assertEquals("3", json.getValue("IdSucursal").jsonPrimitive.content)
        assertEquals("", form.copy(notes = "").observationsForApi())
    }

    @Test
    fun anOrderWithoutMetadataCanBeEditedUsingItsLinkedClientAndBranch() {
        val client = Json.decodeFromString<Client>("""{
            "Id":12,"Nombre":"Marisquería","Telefono":"6670000001",
            "Sucursales":[{"Id":3,"IdCliente":12,"NombreSucursal":"Las Quintas","Direccion":"Lago de Chapala 1298"}]
        }""")
        val order = OrderResponse(
            id = 15, clientId = 12, branchId = 3, date = 20261121, client = client,
            notes = "Llamar al llegar", total = 1300.0,
            lines = listOf(OrderLineResponse(productCode = 101, size = "Mediano", quantity = 5.0, price = 250.0, subtotal = 1250.0))
        )

        val form = formFromOrder(order)
        assertEquals("Marisquería", form.client)
        assertEquals("6670000001", form.phone)
        assertEquals("Las Quintas", form.branch)
        assertEquals("Lago de Chapala 1298", form.address)
        assertEquals(3, form.branchId)
        assertEquals("Dirección", form.deliveryMode)
        assertEquals("50", form.shipping)
        assertEquals("Llamar al llegar", form.observationsForApi())
        assertEquals("Marisquería", toOrder(order).client)
        assertEquals("Las Quintas", toOrder(order).destination)
        assertFalse(order.isPickupDelivery())
    }

    @Test
    fun editingALegacyRegisteredOrderRemovesTheMetadataFromTheNextSubmission() {
        val notes = OrderObservations("Tocar el timbre", OrderDeliveryInfo(
            clientName = "Marisquería", clientType = "Cliente registrado", method = "Dirección",
            address = "Lago de Chapala 1298", branchName = "Las Quintas"
        )).encode()
        val form = formFromOrder(OrderResponse(id = 15, clientId = 12, branchId = 3, notes = notes))

        assertEquals("Marisquería", form.client)
        assertEquals("Lago de Chapala 1298", form.address)
        assertEquals("Tocar el timbre", form.observationsForApi())
    }

    @Test
    fun registeredPickupRemainsRecognizableWithoutAnObservationsBlock() {
        for (branchId in listOf(null, 0)) {
            val response = OrderResponse(id = 15, clientId = 12, branchId = branchId, notes = "Recoge Luis")
            val form = formFromOrder(response)
            assertTrue(response.isPickupDelivery())
            assertEquals("Recolección", form.deliveryMode)
            assertEquals("Congeladora Global Foods", form.branch)
            assertEquals("Recolección", toOrder(response).destination)
            assertEquals("Recoge Luis", form.observationsForApi())
        }
        assertFalse(OrderResponse(id = 16).isPickupDelivery())
    }
}
