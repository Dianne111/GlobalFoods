package com.globalfoods.project

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OrderFormValidationTest {
    private val today = dateToMillis("22/11/2026")!!

    private val validRegistered = NewOrderFormState(
        quantities = mapOf("U10" to "2.5"),
        clientType = "Cliente registrado",
        clientId = 7,
        client = "El Faro",
        branchId = 3,
        branch = "Centro",
        deliveryDate = "22/11/2026"
    )

    @Test
    fun anEmptyFormReportsTheMissingData() {
        val errors = validateOrderForm(NewOrderFormState(deliveryDate = ""), isEditing = false, todayMillis = today)
        assertEquals(SELECT_CLIENT_MESSAGE, errors.client)
        assertEquals(ADD_PRODUCT_MESSAGE, errors.products)
        assertEquals(SELECT_DATE_MESSAGE, errors.date)
    }

    @Test
    fun aRegisteredClientNeedsTheClientAndTheBranch() {
        assertTrue(validateOrderForm(validRegistered, isEditing = false, todayMillis = today).isValid)

        val missing = validateOrderForm(validRegistered.copy(clientId = null, branchId = null), false, today)
        assertEquals(SELECT_CLIENT_MESSAGE, missing.client)
        assertEquals(SELECT_BRANCH_MESSAGE, missing.branch)

        // En recolección no se pide sucursal.
        assertNull(validateOrderForm(validRegistered.copy(branchId = null, deliveryMode = "Recolección"), false, today).branch)
    }

    @Test
    fun aTypedContactDoesNotReplaceSelectingARegisteredClient() {
        val form = NewOrderFormState(
            quantities = mapOf("U10" to "1"),
            clientType = "Cliente esporádico",
            client = "Ana",
            phone = "66712",
            street = "",
            neighborhood = "Centro",
            postalCode = "800",
            deliveryDate = "22/11/2026"
        )
        val errors = validateOrderForm(form, isEditing = false, todayMillis = today)
        assertEquals(SELECT_CLIENT_MESSAGE, errors.client)
        assertEquals(SELECT_BRANCH_MESSAGE, errors.branch)

        val fixed = form.copy(clientType = "Cliente registrado", clientId = 7, branchId = 3)
        assertTrue(validateOrderForm(fixed, isEditing = false, todayMillis = today).isValid)
    }

    @Test
    fun aZeroClientIdCannotBeSubmittedEvenForPickup() {
        val errors = validateOrderForm(validRegistered.copy(clientId = 0, deliveryMode = "Recolección"), false, today)
        assertEquals(SELECT_CLIENT_MESSAGE, errors.client)
        assertNull(errors.branch)
    }

    @Test
    fun aProductInZeroIsMarkedOnItsOwnLine() {
        val errors = validateOrderForm(validRegistered.copy(quantities = mapOf("U10" to "0")), false, today)
        assertNull(errors.products)
        assertEquals(mapOf("U10" to QUANTITY_MESSAGE), errors.lines)
        assertEquals(ADD_PRODUCT_MESSAGE, validateOrderForm(validRegistered.copy(quantities = emptyMap()), false, today).products)
    }

    @Test
    fun aPastDateIsOnlyAllowedWhenEditing() {
        val past = validRegistered.copy(deliveryDate = "20/11/2026")
        assertEquals(PAST_DATE_MESSAGE, validateOrderForm(past, isEditing = false, todayMillis = today).date)
        assertNull(validateOrderForm(past, isEditing = true, todayMillis = today).date)
    }

    @Test
    fun notesAreLimitedTo250Characters() {
        val errors = validateOrderForm(validRegistered.copy(notes = "a".repeat(251)), false, today)
        assertEquals("Máximo 250 caracteres.", errors.notes)
        assertNull(validateOrderForm(validRegistered.copy(notes = "a".repeat(250)), false, today).notes)
    }
}
