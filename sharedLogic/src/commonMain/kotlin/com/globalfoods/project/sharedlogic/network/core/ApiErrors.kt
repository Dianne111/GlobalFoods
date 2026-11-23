package com.globalfoods.project.sharedlogic.network.core

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

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

/** Muestra el motivo de validación de POST/PUT en lugar de perderlo y mostrar solo HTTP 400. */
suspend fun ensureWriteSuccess(response: HttpResponse, what: String) {
    if (response.status.isSuccess()) return
    val body = try {
        response.bodyAsText()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        ""
    }
    error(writeErrorMessageFor(response.status.value, what, body))
}

internal fun writeErrorMessageFor(status: Int, what: String, body: String): String {
    val prefix = "La API rechazó $what (HTTP $status)"
    fun messages(value: JsonElement): List<String> = when (value) {
        JsonNull -> emptyList()
        is JsonPrimitive -> if (value.isString) listOf(value.content) else emptyList()
        is JsonArray -> value.flatMap(::messages)
        is JsonObject -> value.flatMap { (field, detail) -> messages(detail).map { "$field: $it" } }
    }
    val text = body.trim()
    val parsed = runCatching { Json.parseToJsonElement(text) }.getOrNull()
    val details = when (parsed) {
        is JsonObject -> {
            val validation = (parsed["errors"] ?: parsed["Errors"])?.let(::messages).orEmpty().filter(String::isNotBlank)
            if (validation.isNotEmpty()) validation.joinToString("; ")
            else sequenceOf("error", "message", "Message", "detail", "title")
                .mapNotNull { key -> parsed[key]?.let(::messages)?.filter(String::isNotBlank)?.joinToString("; ") }
                .firstOrNull { it.isNotBlank() }.orEmpty()
        }
        is JsonPrimitive -> messages(parsed).joinToString("; ")
        else -> if (parsed == null && !text.startsWith("<")) text else ""
    }.trim().take(800)
    return if (details.isEmpty()) prefix else "$prefix: $details"
}

/**
 * La API rechazó la operación por un conflicto (HTTP 409), por ejemplo al dar de baja un cliente
 * o una sucursal con pedidos pendientes. [message] es el texto que manda la API.
 */
class ApiConflictException(message: String) : Exception(message)

@Serializable
private data class ApiErrorBody(val error: String? = null)

/** Texto del campo "error" que manda la API al rechazar una petición, o null si no viene. */
suspend fun apiErrorMessage(response: HttpResponse): String? =
    try {
        response.body<ApiErrorBody>().error?.takeIf { it.isNotBlank() }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

/**
 * Si la respuesta no fue exitosa, lanza un error con el mensaje de la API (o [fallback] con el
 * código HTTP si no manda uno). Un 409 se lanza como [ApiConflictException].
 */
suspend fun ensureSuccessWithApiMessage(response: HttpResponse, fallback: String) {
    if (response.status.isSuccess()) return
    val message = apiErrorMessage(response)
    if (response.status == HttpStatusCode.Conflict) {
        throw ApiConflictException(message ?: fallback)
    }
    error(message ?: "$fallback (HTTP ${response.status.value})")
}
