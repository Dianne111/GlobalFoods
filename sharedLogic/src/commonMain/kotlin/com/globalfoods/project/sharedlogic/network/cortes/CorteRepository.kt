package com.globalfoods.project.sharedlogic.network.cortes

import com.globalfoods.project.sharedlogic.network.core.globalFoodsClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable

@Serializable
private data class ApiError(val error: String? = null)

class CorteRepository {
    private val baseUrl = "https://api.globalfoodsmexico.com/api/cortes"

    suspend fun getCortePendiente(): Result<OrdenCorte?> = request {
        val response = globalFoodsClient.get("$baseUrl/pendiente")
        if (!response.status.isSuccess()) {
            error("No se pudo consultar el corte pendiente (HTTP ${response.status.value})")
        }
        val data: CortePendienteResponse = response.body()
        data.orden
    }

    suspend fun confirmarCorte(idCorte: Int, codigo: String): Result<Unit> = request {
        val response = globalFoodsClient.put("$baseUrl/$idCorte/confirmar") {
            contentType(ContentType.Application.Json)
            setBody(ConfirmarCorteRequest(codigo = codigo))
        }
        if (!response.status.isSuccess()) {
            val errorMsg = try {
                response.body<ApiError>().error
            } catch (_: Exception) {
                null
            }
            error(errorMsg ?: "El código de 4 dígitos es incorrecto")
        }
    }

    private suspend fun <T> request(block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.failure(Exception(e.message ?: "Error de comunicación con el servidor", e))
        }
}