package com.globalfoods.project

import com.globalfoods.project.sharedlogic.network.auth.LoginResponse
import com.globalfoods.project.sharedlogic.network.auth.UserRole
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UserRoleTest {
    @Test
    fun readsTheRoleIgnoringCase() {
        assertEquals(UserRole.Administrator, UserRole.fromApi("Administrador"))
        assertEquals(UserRole.Administrator, UserRole.fromApi(" admin "))
        assertEquals(UserRole.Seller, UserRole.fromApi("Vendedor"))
        assertEquals(UserRole.Seller, UserRole.fromApi("EMPLEADO"))
        assertEquals(UserRole.Client, UserRole.fromApi("Cliente"))
    }

    @Test
    fun aMissingOrUnknownRoleCannotLogIn() {
        assertNull(UserRole.fromApi(null))
        assertNull(UserRole.fromApi(""))
        assertNull(UserRole.fromApi("Reporte"))
        assertNull(UserRole.fromApi("Supervisor"))
    }

    @Test
    fun theAppOnlyHasThreeRoles() {
        assertEquals(listOf(UserRole.Administrator, UserRole.Seller, UserRole.Client), UserRole.entries)
    }

    @Test
    fun sellersAndAdministratorsHandleOrdersTheSameWay() {
        for (role in listOf(UserRole.Administrator, UserRole.Seller)) {
            assertTrue(role.canRegisterPayment)
            assertTrue(role.canSeeInventory)
            assertTrue(role.canEditOrders)
            assertTrue(role.canCancelOrders)
            assertFalse(role.canManageOwnBranches)
        }
    }

    @Test
    fun onlyAdministratorsManageClientsAndTheirBranches() {
        // RN-18 y CU-16 a CU-22: la asesora (vendedor) solo consulta clientes (CU-03).
        assertEquals(listOf(UserRole.Administrator), UserRole.entries.filter { it.canManageClients })
    }

    @Test
    fun aClientCreatesAndConsultsButDoesNotEditDeleteOrSeeStock() {
        val client = UserRole.Client
        assertTrue(client.canManageOwnBranches)
        assertFalse(client.canEditOrders)
        assertFalse(client.canCancelOrders)
        assertFalse(client.canSeeInventory)
        assertFalse(client.canRegisterPayment)
    }

    @Test
    fun parsesTipoUsuarioFromTheLoginResponse() {
        val json = Json { ignoreUnknownKeys = true }
        val withRole = json.decodeFromString(
            LoginResponse.serializer(),
            """{"accessToken":"t","nombre":"Ana","tipoUsuario":"Empleado"}"""
        )
        val withoutRole = json.decodeFromString(LoginResponse.serializer(), """{"accessToken":"t","nombre":"Ana"}""")
        assertTrue(withRole.tipoUsuario == "Empleado")
        assertFalse(withoutRole.tipoUsuario != null)
    }
}
