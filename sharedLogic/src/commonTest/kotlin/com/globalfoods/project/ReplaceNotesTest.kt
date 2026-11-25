package com.globalfoods.project

import com.globalfoods.project.sharedlogic.network.orders.OrderDeliveryInfo
import com.globalfoods.project.sharedlogic.network.orders.OrderObservations
import kotlin.test.Test
import kotlin.test.assertEquals

class ReplaceNotesTest {
    private val home = "A domicilio · Entrega 12/05/2026 · Dir: Calle 1 · Ref: Portón azul"

    @Test
    fun keepsTheWebDeliveryDataAndChangesOnlyTheNotes() {
        assertEquals(
            "$home · Dejar en recepción",
            OrderObservations.replaceNotes("$home · Llamar antes", " Dejar en recepción ")
        )
        assertEquals("$home · Nueva nota", OrderObservations.replaceNotes(home, "Nueva nota"))
        assertEquals(home, OrderObservations.replaceNotes("$home · Llamar antes", "  "))
        val decoded = OrderObservations.decode(OrderObservations.replaceNotes("$home · Vieja", "Nueva"))
        assertEquals("Nueva", decoded.notes)
        assertEquals("Calle 1", decoded.delivery?.address)
    }

    @Test
    fun keepsThePickupFormat() {
        val pickup = "Recolección en congeladora · Entrega 03/06/2026"
        assertEquals("$pickup · Pasa a las 9", OrderObservations.replaceNotes(pickup, "Pasa a las 9"))
    }

    @Test
    fun keepsTheOlderMobileFormat() {
        val info = OrderDeliveryInfo("Ana", "Registrado", "Dirección", "Calle 2")
        val original = OrderObservations("Vieja", info).encode()
        val updated = OrderObservations.replaceNotes(original, "Nueva")
        assertEquals(OrderObservations("Nueva", info), OrderObservations.decode(updated))
        assertEquals(OrderObservations("", info), OrderObservations.decode(OrderObservations.replaceNotes(original, "")))
    }

    @Test
    fun plainNotesAreReplacedCompletely() {
        assertEquals("Nueva", OrderObservations.replaceNotes("Texto sin formato", "Nueva"))
        assertEquals("Nueva", OrderObservations.replaceNotes(null, "Nueva"))
        assertEquals("", OrderObservations.replaceNotes(null, ""))
    }
}
