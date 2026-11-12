package com.globalfoods.project.sharedlogic.network.orders

import com.globalfoods.project.sharedlogic.network.core.globalFoodsClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException

class OrderRepository {
    private val baseUrl = "https://api.globalfoodsmexico.com/api"

    suspend fun getClients(): Result<List<Client>> = request { globalFoodsClient.get("$baseUrl/clientes/activos").body() }

    suspend fun getBranches(clientId: Int): Result<List<Branch>> = request {
        globalFoodsClient.get("$baseUrl/sucursales-clientes") {
            header("X-Id-Cliente", clientId.toString())
        }.body()
    }
    suspend fun getOrders(): Result<List<OrderResponse>> = request { globalFoodsClient.get("$baseUrl/pedidos").body() }
    suspend fun getOrder(id: Int): Result<OrderResponse> = request { globalFoodsClient.get("$baseUrl/pedidos/$id").body() }

    suspend fun createOrder(order: OrderRequest): Result<OrderResponse> = request {
        require(order.date > 0) { "La fecha del pedido no es válida" }
        require(order.totalKg > 0.0) { "El total de kilos debe ser mayor que cero" }
        require(order.total > 0.0) { "El total del pedido debe ser mayor que cero" }
        require(order.lines.isNotEmpty()) { "El pedido debe contener al menos una línea" }
        require(order.lines.all { it.productCode > 0 }) {
            "El inventario no proporcionó un CodigoProducto válido"
        }
        require(order.lines.all { it.quantity > 0.0 && it.price >= 0.0 && it.subtotal >= 0.0 }) {
            "El pedido contiene una línea inválida"
        }

        val response = globalFoodsClient.post("$baseUrl/pedidos") {
            contentType(ContentType.Application.Json)
            setBody(order)
        }
        if (!response.status.isSuccess()) {
            error("La API rechazó el pedido: HTTP ${response.status.value}")
        }
        response.body<OrderResponse>().also {
            require(it.id > 0) { "La API no devolvió el Id del pedido creado" }
        }
    }

    suspend fun deleteOrder(id: Int): Result<Unit> = request {
        val response = globalFoodsClient.delete("$baseUrl/pedidos/$id")
        if (!response.status.isSuccess()) error("Error del servidor: ${response.status.value}")
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
