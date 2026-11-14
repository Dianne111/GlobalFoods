package com.globalfoods.project

import com.globalfoods.project.sharedlogic.network.auth.LoginResponse
import com.globalfoods.project.sharedlogic.network.auth.UserRole
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserRoleTest {
    @Test
    fun readsTheRoleIgnoringCase() {
        assertEquals(UserRole.Administrator, UserRole.fromApi("Administrador"))
        assertEquals(UserRole.Employee, UserRole.fromApi("EMPLEADO"))
        assertEquals(UserRole.Reports, UserRole.fromApi(" reporte "))
        assertEquals(UserRole.Client, UserRole.fromApi("Cliente"))
        assertEquals(UserRole.Administrator, UserRole.fromApi(null))
    }

    @Test
    fun onlySellersAndAdministratorsRegisterPayments() {
        assertEquals(
            listOf(UserRole.Administrator, UserRole.Employee),
            UserRole.entries.filter { it.canRegisterPayment }
        )
    }

    @Test
    fun aClientCreatesAndConsultsButDoesNotEditDeleteOrSeeStock() {
        val client = UserRole.Client
        assertTrue(client.canCreateOrders)
        assertTrue(client.canManageOwnBranches)
        assertFalse(client.canEditOrders)
        assertFalse(client.canCancelOrders)
        assertFalse(client.canSeeInventory)
        assertTrue(UserRole.Employee.canSeeInventory && UserRole.Employee.canEditOrders)
        assertFalse(UserRole.Reports.canCreateOrders)
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
