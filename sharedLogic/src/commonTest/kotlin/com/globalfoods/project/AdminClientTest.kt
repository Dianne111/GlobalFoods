package com.globalfoods.project

import com.globalfoods.project.sharedlogic.network.clients.AdminClient
import com.globalfoods.project.sharedlogic.network.clients.ClientRequest
import com.globalfoods.project.sharedlogic.network.orders.BranchRequest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class AdminClientTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun readsTheClientButNeverPrintsThePassword() {
        val parsed = json.decodeFromString(
            AdminClient.serializer(),
            """{"Id":5,"Usuario":"ana","Contrasena":"secreta123","Nombre":"Ana","Telefono":"33","Estatus":0}"""
        )
        assertEquals("Ana", parsed.name)
        assertEquals("ana", parsed.user)
        assertFalse(parsed.isActive)
        assertFalse(parsed.toString().contains("secreta123"))
    }

    @Test
    fun aDeactivatedBranchIsSentWithEstatusZero() {
        val body = json.encodeToString(
            BranchRequest.serializer(),
            BranchRequest(clientId = 7, name = "Centro", address = "Av. Juárez 120", phone = "", status = 0)
        )
        assertEquals(
            """{"idCliente":7,"NombreSucursal":"Centro","Direccion":"Av. Juárez 120","Telefono":"","Estatus":0}""",
            body
        )
    }

    @Test
    fun sendsTheSameFieldsAsTheWebApp() {
        val body = json.encodeToString(
            ClientRequest.serializer(),
            ClientRequest(usuario = "ana", contrasena = "x", nombre = "Ana", telefono = "33", estatus = 1)
        )
        assertEquals("""{"usuario":"ana","contrasena":"x","nombre":"Ana","telefono":"33","estatus":1}""", body)
    }
}
