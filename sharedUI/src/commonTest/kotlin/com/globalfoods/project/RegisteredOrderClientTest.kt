package com.globalfoods.project

import com.globalfoods.project.sharedlogic.network.orders.Client
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RegisteredOrderClientTest {
    private val old = NewOrderFormState(
        clientId = 7, client = "Anterior", phone = "6670000001", branchId = 3, branch = "Anterior",
        address = "Dirección anterior", quantities = mapOf("Mediano" to "10.5"), notes = "Llamar"
    )

    @Test
    fun selectingAClientUsesItsIdAndPhoneAndClearsThePreviousBranch() {
        val client = Client(id = 8, name = "Mariscos José", phonePascal = "6671234567")
        val selected = selectRegisteredClient(old, client)
        assertEquals(8, selected.clientId)
        assertEquals("Mariscos José", selected.client)
        assertEquals("6671234567", selected.phone)
        assertEquals("Cliente registrado", selected.clientType)
        assertNull(selected.branchId)
        assertEquals("", selected.branch)
        assertEquals("", selected.address)
        assertEquals(old.quantities, selected.quantities)
        assertEquals(old.notes, selected.notes)
    }

    @Test
    fun pickupKeepsTheFreezerWhenSelectingAnotherClient() {
        val selected = selectRegisteredClient(old.copy(deliveryMode = "Recolección"), Client(8, "Mariscos José"))
        assertNull(selected.branchId)
        assertEquals(PickupLocation.description, selected.branch)
        assertEquals(PickupLocation.description, selected.address)
    }

    @Test
    fun equalNamesDoNotReplaceTheIdOfTheSelectedClient() {
        val first = Client(8, "Mariscos José", phonePascal = "6670000001")
        val second = Client(9, "Mariscos José", phonePascal = "6670000002")
        assertEquals(8, selectRegisteredClient(old, first).clientId)
        val selected = selectRegisteredClient(old, second)
        assertEquals(9, selected.clientId)
        assertEquals("6670000002", selected.phone)
    }

    @Test
    fun oldUnregisteredContactsMustBeReplacedWithARegisteredSelection() {
        val normalized = registeredOrderForm(old.copy(clientType = "Cliente esporádico", clientId = null))
        assertEquals("Cliente registrado", normalized.clientType)
        assertNull(normalized.clientId)
        assertEquals("", normalized.client)
        assertEquals("", normalized.phone)
        assertNull(normalized.branchId)
        assertEquals("", normalized.address)
        assertEquals(old.quantities, normalized.quantities)
    }

    @Test
    fun aNewStaffOrderStartsWithTheRegisteredClientFlow() {
        val form = initialNewOrderForm(false, null, null, null)
        assertEquals("Cliente registrado", form.clientType)
        assertNull(form.clientId)
        assertEquals("", form.client)
    }
}
