package com.globalfoods.project.sharedlogic.network.clients

import com.globalfoods.project.sharedlogic.network.core.ApiConflictException
import com.globalfoods.project.sharedlogic.network.core.ensureSuccessWithApiMessage
import com.globalfoods.project.sharedlogic.network.core.globalFoodsClient
import com.globalfoods.project.sharedlogic.network.core.networkErrorMessage
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Cliente tal como lo devuelve GET /api/clientes (catálogo del administrador). La API no manda la contraseña. */
@Serializable
data class AdminClient(
    @SerialName("Id") val id: Int,
    @SerialName("Usuario") val user: String = "",
    @SerialName("Nombre") val name: String = "",
    @SerialName("Telefono") val phone: String? = null,
    @SerialName("Estatus") val status: Int = 1
) {
    val isActive: Boolean get() = status == 1
}

/**
 * Cuerpo de POST /api/clientes y PUT /api/clientes/{id} (mismo formato que la app web).
 * En el PUT, si [contrasena] es null no se envía y la API conserva la contraseña actual.
 */
@Serializable
data class ClientRequest(
    val usuario: String,
    val contrasena: String? = null,
    val nombre: String,
    val telefono: String,
    val estatus: Int
)

class ClientRepository {
    private val baseUrl = "https://api.globalfoodsmexico.com/api"

    suspend fun getClients(): Result<List<AdminClient>> = request {
        val response = globalFoodsClient.get("$baseUrl/clientes")
        ensureSuccessWithApiMessage(response, "No se pudieron cargar los clientes")
        response.body<List<AdminClient>>()
    }

    suspend fun createClient(name: String, phone: String, user: String, password: String): Result<Unit> = request {
        require(password.isNotBlank()) { "Escribe una contraseña para el cliente." }
        val body = buildRequest(name, phone, user, password, status = 1)
        val response = globalFoodsClient.post("$baseUrl/clientes") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        ensureSuccessWithApiMessage(response, "No se pudo registrar el cliente")
    }

    /** Si [newPassword] está vacío no se envía contraseña y la API conserva la actual. */
    suspend fun updateClient(
        client: AdminClient,
        name: String,
        phone: String,
        user: String,
        newPassword: String,
        active: Boolean
    ): Result<Unit> = request {
        val body = buildRequest(name, phone, user, newPassword.ifBlank { null }, status = if (active) 1 else 0)
        val response = globalFoodsClient.put("$baseUrl/clientes/${client.id}") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        ensureSuccessWithApiMessage(response, "No se pudo actualizar el cliente")
    }

    /**
     * Baja lógica: el cliente se conserva con Estatus 0 (o vuelve a 1 al reactivarlo).
     * No se usa DELETE /api/clientes/{id} porque borra el registro y su historial.
     * Si tiene pedidos pendientes, la API responde 409 y falla con [ApiConflictException].
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

    private fun buildRequest(name: String, phone: String, user: String, password: String?, status: Int): ClientRequest {
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

    private suspend fun <T> request(block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiConflictException) {
            Result.failure(e)
        } catch (e: Throwable) {
            Result.failure(Exception(networkErrorMessage(e) ?: e.message ?: "Error de comunicación con el servidor", e))
        }
}
