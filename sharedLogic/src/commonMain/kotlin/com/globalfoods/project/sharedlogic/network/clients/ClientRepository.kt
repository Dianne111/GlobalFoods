package com.globalfoods.project.sharedlogic.network.clients

import com.globalfoods.project.sharedlogic.network.core.globalFoodsClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Cliente tal como lo devuelve GET /api/clientes (catálogo del administrador).
 *
 * La API también manda la contraseña en texto plano. Nunca se muestra: solo se conserva para
 * reenviarla al editar sin cambiarla, porque PUT /api/clientes/{id} la espera siempre.
 */
@Serializable
data class AdminClient(
    @SerialName("Id") val id: Int,
    @SerialName("Usuario") val user: String = "",
    @SerialName("Nombre") val name: String = "",
    @SerialName("Telefono") val phone: String? = null,
    @SerialName("Estatus") val status: Int = 1,
    @SerialName("Contrasena") internal val storedPassword: String? = null
) {
    val isActive: Boolean get() = status == 1

    override fun toString(): String = "AdminClient(id=$id, user=$user, name=$name, status=$status)"
}

/** Cuerpo de POST /api/clientes y PUT /api/clientes/{id} (mismo formato que la app web). */
@Serializable
data class ClientRequest(
    val usuario: String,
    val contrasena: String,
    val nombre: String,
    val telefono: String,
    val estatus: Int
)

@Serializable
private data class ApiError(val error: String? = null)

class ClientRepository {
    private val baseUrl = "https://api.globalfoodsmexico.com/api"

    suspend fun getClients(): Result<List<AdminClient>> = request {
        val response = globalFoodsClient.get("$baseUrl/clientes")
        ensureSuccess(response, "No se pudieron cargar los clientes")
        response.body<List<AdminClient>>()
    }

    suspend fun createClient(name: String, phone: String, user: String, password: String): Result<Unit> = request {
        require(password.isNotBlank()) { "Escribe una contraseña para el cliente." }
        val body = buildRequest(name, phone, user, password, status = 1)
        val response = globalFoodsClient.post("$baseUrl/clientes") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        ensureSuccess(response, "No se pudo registrar el cliente")
    }

    /** Si [newPassword] está vacío se reenvía la contraseña actual para no borrarla. */
    suspend fun updateClient(
        client: AdminClient,
        name: String,
        phone: String,
        user: String,
        newPassword: String,
        active: Boolean
    ): Result<Unit> = request {
        val password = newPassword.ifBlank { client.storedPassword.orEmpty() }
        require(password.isNotBlank()) {
            "La API no devolvió la contraseña actual del cliente; escribe una nueva para guardar los cambios."
        }
        val body = buildRequest(name, phone, user, password, status = if (active) 1 else 0)
        val response = globalFoodsClient.put("$baseUrl/clientes/${client.id}") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        ensureSuccess(response, "No se pudo actualizar el cliente")
    }

    /**
     * Baja lógica: el cliente se conserva con Estatus 0 (o vuelve a 1 al reactivarlo).
     * No se usa DELETE /api/clientes/{id} para no perder el historial de pedidos.
     */
    suspend fun setActive(client: AdminClient, active: Boolean): Result<Unit> =
        updateClient(
            client = client,
            name = client.name,
            phone = client.phone.orEmpty(),
            user = client.user,
            newPassword = "",
            active = active
        )

    private fun buildRequest(name: String, phone: String, user: String, password: String, status: Int): ClientRequest {
        require(name.isNotBlank()) { "Escribe el nombre del cliente." }
        require(user.isNotBlank()) { "Escribe el usuario con el que el cliente iniciará sesión." }
        return ClientRequest(
            usuario = user.trim(),
            contrasena = password,
            nombre = name.trim(),
            telefono = phone.trim(),
            estatus = status
        )
    }

    private suspend fun ensureSuccess(response: HttpResponse, fallback: String) {
        if (response.status.isSuccess()) return
        val message = try {
            response.body<ApiError>().error
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
        error(message?.takeIf { it.isNotBlank() } ?: "$fallback (HTTP ${response.status.value})")
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
