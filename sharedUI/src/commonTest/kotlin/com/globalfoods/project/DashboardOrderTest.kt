package com.globalfoods.project

import com.globalfoods.project.sharedlogic.network.orders.Client
import com.globalfoods.project.sharedlogic.network.orders.OrderDeliveryInfo
import com.globalfoods.project.sharedlogic.network.orders.OrderObservations
import com.globalfoods.project.sharedlogic.network.orders.OrderResponse
import kotlin.test.Test
import kotlin.test.assertEquals

class DashboardOrderTest {
    @Test
    fun webCompatibleNotesKeepTheRegisteredNameAndBothDestinations() {
        val client = Client(id = 42, name = "María López")
        val response = OrderResponse(
            id = 27, clientId = 42, branchId = 7,
            notes = "A domicilio · Entrega 25/11/2026 · Dir: Av. Juárez 120"
        )
        val order = toOrder(response, mapOf(42 to client), mapOf(7 to "Sucursal Centro"))
        assertEquals("María López", order.client)
        assertEquals("Sucursal Centro", order.destination)
        val pickup = toOrder(response.copy(notes = "Recolección en congeladora · Entrega 25/11/2026"), mapOf(42 to client))
        assertEquals("Recolección", pickup.destination)
        assertEquals("María López", pickup.client)
    }

    @Test
    fun looksUpTheClientByIdAndPreservesItForTheDetailScreen() {
        val client = Client(id = 42, name = "María López")
        val clients = mapOf(8 to Client(id = 8, name = "Otro cliente"), 42 to client)
        val response = OrderResponse(id = 27, clientId = 42)

        val order = toOrder(response, clients)

        assertEquals("María López", order.client)
        assertEquals(client, order.response?.client)
        assertEquals(27, order.id)
    }

    @Test
    fun keepsTheNameCapturedForASporadicClient() {
        val observations = OrderObservations(
            notes = "Llamar al llegar",
            delivery = OrderDeliveryInfo(
                clientName = "Ana Pérez",
                clientType = "Cliente esporádico",
                method = "Dirección",
                address = "Calle del Mar 15"
            )
        )
        val response = OrderResponse(id = 28, notes = observations.encode())

        assertEquals("Ana Pérez", toOrder(response).client)
    }

    @Test
    fun missingClientNamesDoNotBecomeNumericLabels() {
        val response = OrderResponse(id = 29, clientId = 42)
        val clients = mapOf(8 to Client(id = 8, name = "Otro cliente"))

        assertEquals("Nombre no disponible", toOrder(response, clients).client)
        assertEquals("Nombre no disponible", toOrder(OrderResponse(id = 30)).client)
    }
}
