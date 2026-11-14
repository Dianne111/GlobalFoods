package com.globalfoods.project

import com.globalfoods.project.sharedlogic.network.core.errorMessageFor
import kotlin.test.Test
import kotlin.test.assertEquals

class ErrorMessageTest {
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
