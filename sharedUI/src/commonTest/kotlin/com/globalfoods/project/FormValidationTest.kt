package com.globalfoods.project

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FormValidationTest {
    @Test
    fun phoneMustHaveTenDigits() {
        assertEquals(REQUIRED_MESSAGE, phoneError(""))
        assertEquals(PHONE_MESSAGE, phoneError("667123456"))
        assertNull(phoneError("6671234567"))
        assertEquals("3312345678", phoneInput("(33) 1234-5678 99"))
    }

    @Test
    fun userNeedsSixToTwentyCharactersWithoutSpaces() {
        assertEquals(REQUIRED_MESSAGE, userError(""))
        assertEquals(USER_MESSAGE, userError("luisf"))
        assertEquals(USER_MESSAGE, userError("a".repeat(21)))
        assertNull(userError("palo_seco"))
    }

    @Test
    fun passwordIsRequiredOnlyForNewClients() {
        assertEquals(REQUIRED_MESSAGE, passwordError("", required = true))
        assertNull(passwordError("", required = false))
        assertEquals(PASSWORD_MESSAGE, passwordError("1234567", required = false))
        assertNull(passwordError("12345678", required = true))
    }

    @Test
    fun newClientReportsEveryInvalidField() {
        val errors = validateClientForm(name = " ", phone = "123", user = "ab", password = "123", originalUser = null)
        assertEquals(ClientFormErrors(REQUIRED_MESSAGE, PHONE_MESSAGE, USER_MESSAGE, PASSWORD_MESSAGE), errors)

        val valid = validateClientForm("El Faro", "6671234567", "elfaro01", "secreto123", originalUser = null)
        assertTrue(valid.isValid)
    }

    @Test
    fun editingKeepsAnOldShortUserUnlessItChanges() {
        assertNull(validateClientForm("Luis", "6671234567", "luisf", "", originalUser = "luisf").user)
        assertEquals(USER_MESSAGE, validateClientForm("Luis", "6671234567", "luis", "", originalUser = "luisf").user)
    }

    @Test
    fun branchRequiresNameAddressPhoneAndAUniqueName() {
        val empty = validateBranchForm("", "", "", otherActiveNames = emptyList())
        assertEquals(BranchFormErrors(REQUIRED_MESSAGE, REQUIRED_MESSAGE, REQUIRED_MESSAGE), empty)

        val duplicated = validateBranchForm(" centro ", "Av. Obregón 10, Centro, Culiacán", "6671234567", listOf("Centro"))
        assertEquals(DUPLICATE_BRANCH_MESSAGE, duplicated.name)

        assertTrue(validateBranchForm("Norte", "Blvd. Zapata 5, Culiacán", "6671234567", listOf("Centro")).isValid)
    }
}
