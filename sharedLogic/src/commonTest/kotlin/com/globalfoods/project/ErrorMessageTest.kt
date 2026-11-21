package com.globalfoods.project

import com.globalfoods.project.sharedlogic.network.core.errorMessageFor
import com.globalfoods.project.sharedlogic.network.core.writeErrorMessageFor
import kotlin.test.Test
import kotlin.test.assertEquals

class ErrorMessageTest {
    @Test
    fun preservesTheServerValidationMessageAndFieldNames() {
        assertEquals("La API rechazó el pedido (HTTP 400): Motivo devuelto por el servidor",
            writeErrorMessageFor(400, "el pedido", """{"error":"Motivo devuelto por el servidor"}"""))
        assertEquals("La API rechazó el pedido (HTTP 400): IdSucursal: Sucursal inválida; Fecha: Fecha inválida",
            writeErrorMessageFor(400, "el pedido", """{"title":"Validation failed","errors":{"IdSucursal":["Sucursal inválida"],"Fecha":["Fecha inválida"]}}"""))
        assertEquals("La API rechazó los cambios del pedido (HTTP 400): Datos inválidos",
            writeErrorMessageFor(400, "los cambios del pedido", """{"message":"Datos inválidos"}"""))
    }

    @Test
    fun emptyOrHtmlResponsesKeepTheHttpCodeWithoutDisplayingThePage() {
        for (body in listOf("", "<html>Error</html>")) {
            assertEquals("La API rechazó el pedido (HTTP 400)", writeErrorMessageFor(400, "el pedido", body))
        }
    }

    @Test
    fun explainsPermissionAndSessionErrorsInPlainSpanish() {
        assertEquals(
            "Tu usuario no tiene permiso para consultar las sucursales. Comunícate con tu asesor de ventas.",
            errorMessageFor(403, "las sucursales")
        )
        assertEquals("Tu sesión expiró. Inicia sesión nuevamente.", errorMessageFor(401, "los pedidos"))
        assertEquals("No se pudo consultar los pedidos (error del servidor 500).", errorMessageFor(500, "los pedidos"))
    }
}
