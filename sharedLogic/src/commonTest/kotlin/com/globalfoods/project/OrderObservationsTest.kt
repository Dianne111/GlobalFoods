package com.globalfoods.project

import com.globalfoods.project.sharedlogic.network.orders.OrderDeliveryInfo
import com.globalfoods.project.sharedlogic.network.orders.OrderObservations
import com.globalfoods.project.sharedlogic.network.orders.OrderRequest
import com.globalfoods.project.sharedlogic.network.orders.OrderResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OrderObservationsTest {
    @Test
    fun orderDetailResponseCanContainOnlyClientAndBranchIds() {
        val response = Json.decodeFromString<OrderResponse>(
            """{
                "Id": 25,
                "IdCliente": 12,
                "IdSucursal": 3,
                "Fecha": 20261109,
                "TotalKG": 5,
                "Total": 1250,
                "Estatus": 1,
                "Observaciones": null,
                "lineas": [{
                    "Id": 40,
                    "CodigoProducto": 101,
                    "Talla": "Mediano",
                    "Cantidad": 5,
                    "Precio": 250,
                    "Subtotal": 1250
                }]
            }"""
        )

        assertEquals(25, response.id)
        assertEquals(12, response.clientId)
        assertEquals(3, response.branchId)
        assertEquals(20261109, response.date)
        assertEquals(5.0, response.totalKg)
        assertEquals(1250.0, response.total)
        assertNull(response.client)
        assertNull(response.notes)
        assertEquals("Mediano", response.lines.single().size)
        assertEquals(5.0, response.lines.single().quantity)
        assertEquals(OrderObservations(""), OrderObservations.decode(response.notes))
    }

    @Test
    fun deliveryDetailsTravelThroughTheExistingApiField() {
        val delivery = OrderDeliveryInfo(
            clientName = "Lucía Martínez",
            clientType = "Cliente esporádico",
            method = "Dirección",
            address = "Calle Brisa 42, Colonia Miramar, C.P. 82000",
            phone = "669 000 0123",
            references = "Portón azul\nEntrada lateral",
            shippingCost = 80.0
        )
        val observations = OrderObservations("Llamar antes de llegar.\n", delivery)
        val request = OrderRequest(null, null, 20261114, 4.0, 1200.0, 1, observations.encode(), emptyList())
        val body = Json.encodeToJsonElement(OrderRequest.serializer(), request).jsonObject
        val response = Json.decodeFromString<OrderResponse>("""{"Id":184,"Observaciones":${body.getValue("Observaciones")}}""")

        assertEquals(observations, OrderObservations.decode(response.notes))
        assertEquals(OrderObservations("", delivery), OrderObservations.decode(OrderObservations("", delivery).encode()))
    }

    @Test
    fun oldNotesAndUnrecognizedPayloadsRemainReadable() {
        val oldNotes = "Entrega por la entrada lateral."
        val incomplete = "$oldNotes\n[GlobalFoods:Entrega:v1]\n{\"clientName\":\"Lucía"

        assertEquals(OrderObservations(oldNotes), OrderObservations.decode(oldNotes))
        assertEquals(OrderObservations(incomplete), OrderObservations.decode(incomplete))
        assertEquals(OrderObservations(""), OrderObservations.decode(null))
    }
}
