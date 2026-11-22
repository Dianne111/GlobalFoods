package com.globalfoods.project

import com.globalfoods.project.sharedlogic.network.orders.OrderDeliveryInfo
import com.globalfoods.project.sharedlogic.network.orders.OrderObservations
import com.globalfoods.project.sharedlogic.network.orders.OrderPaymentMethod
import com.globalfoods.project.sharedlogic.network.orders.OrderResponse
import com.globalfoods.project.sharedlogic.network.orders.paymentRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class OrderWebObservationsTest {
    private val delivery = OrderDeliveryInfo(
        clientName = "María López", clientType = "Cliente registrado", method = "Dirección",
        address = "Av. Juárez 120", phone = "3312345678", branchName = "Sucursal Centro"
    )

    @Test
    fun writesTheExactHomeDeliveryFormatReadByTheExistingWeb() {
        val encoded = OrderObservations("Llamar antes de llegar", delivery).encodeForWeb(20261125)
        assertEquals("A domicilio · Entrega 25/11/2026 · Dir: Av. Juárez 120 · Llamar antes de llegar", encoded)
        assertFalse(encoded.contains("[GlobalFoods:Entrega:v1]"))
        assertFalse(encoded.contains("clientName"))
        val decoded = OrderObservations.decode(encoded)
        assertEquals("Dirección", decoded.delivery?.method)
        assertEquals("Av. Juárez 120", decoded.delivery?.address)
        assertEquals("Llamar antes de llegar", decoded.notes)
    }

    @Test
    fun pickupDoesNotIncludeADestinationAddressOrBlankNotes() {
        val encoded = OrderObservations("", delivery.copy(method = "Recolección", references = "Anterior"))
            .encodeForWeb(20260105)
        assertEquals("Recolección en congeladora · Entrega 05/01/2026", encoded)
        assertEquals("Recolección", OrderObservations.decode(encoded).delivery?.method)
        assertEquals("", OrderObservations.decode(encoded).notes)
    }

    @Test
    fun readsAddressesAndReferencesInEitherOrderAndKeepsNoteSeparatorsAndNewlines() {
        val decoded = OrderObservations.decode(
            "A domicilio · Entrega 25/11/2026 · Ref: Portón azul · Dir: Calle 1 · Llamar\nantes · No tocar"
        )
        assertEquals("Calle 1", decoded.delivery?.address)
        assertEquals("Portón azul", decoded.delivery?.references)
        assertEquals("Llamar\nantes · No tocar", decoded.notes)
    }

    @Test
    fun aMissingOrUnrecognizedWebHeaderIsKeptAsPlainNotes() {
        for (text in listOf("A domicilio", "A domicilio · Entrega 2026/11/25", "Otra modalidad · Entrega 25/11/2026")) {
            assertEquals(OrderObservations(text), OrderObservations.decode(text))
        }
    }

    @Test
    fun replacingTheOldMobileTextDoesNotNestMetadataOrDuplicateHeaders() {
        val old = OrderObservations("Tocar el timbre", delivery).encode()
        val encoded = OrderObservations.decode(old).encodeForWeb(20261125)
        assertEquals("A domicilio · Entrega 25/11/2026 · Dir: Av. Juárez 120 · Tocar el timbre", encoded)
        assertEquals(encoded, OrderObservations.decode(encoded).encodeForWeb(20261125))
    }

    @Test
    fun paymentPreservesCreationAndWebNotesWhileSavingTheRealDeliveryDate() {
        val notes = OrderObservations("Llamar", delivery).encodeForWeb(20261125)
        val request = paymentRequest(
            OrderResponse(id = 7, clientId = 12, branchId = 3, status = 3, date = 20261125, notes = notes),
            OrderPaymentMethod.CardOrTransfer, 20261126
        )
        assertEquals(20261125, request.date)
        assertEquals(20261126, request.deliveryDate)
        assertEquals(2, request.paymentMethod)
        assertEquals(4, request.status)
        assertEquals(notes, request.notes)
    }

}
