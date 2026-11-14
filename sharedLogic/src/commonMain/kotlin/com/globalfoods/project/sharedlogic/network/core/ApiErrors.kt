package com.globalfoods.project.sharedlogic.network.core

import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess

/**
 * Lanza un error con un mensaje para el usuario si la respuesta no fue exitosa.
 * [what] completa la frase: "consultar [what]" (p. ej. "tus sucursales").
 */
fun ensureSuccess(response: HttpResponse, what: String) {
    if (response.status.isSuccess()) return
    error(errorMessageFor(response.status.value, what))
}

fun errorMessageFor(status: Int, what: String): String = when (status) {
    401 -> "Tu sesión expiró. Inicia sesión nuevamente."
    403 -> "Tu usuario no tiene permiso para consultar $what. Comunícate con tu asesor de ventas."
    404 -> "No se encontró la información de $what."
    else -> "No se pudo consultar $what (error del servidor $status)."
}
