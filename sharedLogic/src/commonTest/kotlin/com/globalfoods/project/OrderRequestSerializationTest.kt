package com.globalfoods.project

import com.globalfoods.project.sharedlogic.network.orders.OrderLineRequest
import com.globalfoods.project.sharedlogic.network.orders.OrderRequest
import com.globalfoods.project.sharedlogic.network.orders.OrderResponse
import com.globalfoods.project.sharedlogic.network.orders.cancellationRequest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class OrderRequestSerializationTest {
    @Test
    fun oldOrdersWithoutAPaymentValueStillDecode() {
        assertNull(Json.decodeFromString<OrderResponse>("""{"Id":25}""").paymentMethod)
        assertNull(Json.decodeFromString<OrderResponse>("""{"Id":25,"MetodoPago":null}""").paymentMethod)
    }

    @Test
    fun newOrderSerializesThePendingDefaultAndExactApiFieldNames() {
        val request = OrderRequest(
            clientId = 12, branchId = 3, date = 20261120,
            totalKg = 5.0, total = 1250.0, status = 1, notes = "Llamar al llegar",
            lines = listOf(OrderLineRequest(101, "Mediano", 5.0, 250.0, 1250.0))
        )
        // Usa Json sin encodeDefaults, igual que la configuración de red de la app.
        val body = Json.encodeToJsonElement(OrderRequest.serializer(), request).jsonObject
        assertEquals("0", body.getValue("MetodoPago").jsonPrimitive.content)
        assertEquals("20261120", body.getValue("Fecha").jsonPrimitive.content)
        assertEquals("12", body.getValue("IdCliente").jsonPrimitive.content)
        assertEquals("3", body.getValue("IdSucursal").jsonPrimitive.content)
        val line = body.getValue("lineas").jsonArray.single().jsonObject
        assertEquals("101", line.getValue("CodigoProducto").jsonPrimitive.content)
        assertFalse(line.containsKey("codigoProducto"))
        assertEquals("Mediano", line.getValue("Talla").jsonPrimitive.content)
    }

    @Test
    fun existingPaymentsSurviveCancellationAndProductCodesReadBothApiSpellings() {
        for (key in listOf("CodigoProducto", "codigoProducto")) {
            val response = Json.decodeFromString<OrderResponse>("""{
                "Id":25,"IdCliente":12,"IdSucursal":3,"Fecha":20261120,"MetodoPago":2,
                "TotalKG":5,"Total":1250,"Estatus":3,
                "lineas":[{"Id":40,"$key":101,"Talla":"Mediano","Cantidad":5,"Precio":250,"Subtotal":1250}]
            }""")
            assertEquals(101, response.lines.single().productCode)
            val request = cancellationRequest(response)
            assertEquals(2, request.paymentMethod)
            assertEquals(20261120, request.date)
            assertEquals(250.0, request.lines.single().price)
        }
    }
}
