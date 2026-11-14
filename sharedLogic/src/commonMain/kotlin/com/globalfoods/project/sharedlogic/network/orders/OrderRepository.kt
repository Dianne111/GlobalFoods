package com.globalfoods.project.sharedlogic.network.orders

import com.globalfoods.project.sharedlogic.network.core.globalFoodsClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
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
    suspend fun createBranch(branch: BranchRequest): Result<Unit> = request {
        validate(branch)
        val response = globalFoodsClient.post("$baseUrl/sucursales-clientes") {
            contentType(ContentType.Application.Json)
            setBody(branch)
        }
        if (!response.status.isSuccess()) error("No se pudo registrar la sucursal (HTTP ${response.status.value})")
    }

    suspend fun updateBranch(id: Int, branch: BranchRequest): Result<Unit> = request {
        validate(branch)
        val response = globalFoodsClient.put("$baseUrl/sucursales-clientes/$id") {
            contentType(ContentType.Application.Json)
            setBody(branch)
        }
        if (!response.status.isSuccess()) error("No se pudo actualizar la sucursal (HTTP ${response.status.value})")
    }

    /**
     * Baja lógica: la sucursal se conserva con Estatus 0 (o vuelve a 1 al reactivarla).
     * No se usa DELETE para no perder la referencia de los pedidos que ya se entregaron ahí.
     * No revalida nombre ni dirección para poder dar de baja sucursales antiguas incompletas.
     */
    suspend fun setBranchActive(branch: Branch, active: Boolean): Result<Unit> = request {
        require(branch.clientId > 0) { "La sucursal no tiene un cliente válido." }
        val body = BranchRequest(
            clientId = branch.clientId,
            name = branch.name,
            address = branch.address,
            phone = branch.phone,
            status = if (active) 1 else 0
        )
        val response = globalFoodsClient.put("$baseUrl/sucursales-clientes/${branch.id}") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        if (!response.status.isSuccess()) {
            val action = if (active) "reactivar" else "dar de baja"
            error("No se pudo $action la sucursal (HTTP ${response.status.value})")
        }
    }

    private fun validate(branch: BranchRequest) {
        require(branch.clientId > 0) { "Tu usuario no está vinculado a un cliente." }
        require(branch.name.isNotBlank()) { "Escribe el nombre de la sucursal." }
        require(branch.address.isNotBlank()) { "Escribe la dirección de la sucursal." }
    }

    suspend fun getOrders(): Result<List<OrderResponse>> = request { globalFoodsClient.get("$baseUrl/pedidos").body() }
    suspend fun getOrder(id: Int): Result<OrderResponse> = request { globalFoodsClient.get("$baseUrl/pedidos/$id").body() }

    suspend fun createOrder(order: OrderRequest): Result<OrderResponse> = request {
        validate(order)
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

    /**
     * Reemplaza el pedido [id] (encabezado y líneas) con [order].
     * Devuelve el pedido actualizado si la API lo incluye en la respuesta; si no, null.
     */
    suspend fun updateOrder(id: Int, order: OrderRequest): Result<OrderResponse?> = request {
        require(id > 0) { "El pedido a editar no tiene un Id válido" }
        validate(order)
        val response = globalFoodsClient.put("$baseUrl/pedidos/$id") {
            contentType(ContentType.Application.Json)
            setBody(order)
        }
        if (!response.status.isSuccess()) {
            error("La API rechazó los cambios del pedido: HTTP ${response.status.value}")
        }
        try {
            response.body<OrderResponse>().takeIf { it.id == id }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    private fun validate(order: OrderRequest) {
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
    }

    /**
     * Cancelación (baja lógica): reenvía el pedido tal como está con Estatus 5 (Cancelado).
     * No se usa DELETE para conservar el pedido en el historial.
     */
    suspend fun cancelOrder(order: OrderResponse): Result<OrderResponse?> {
        val status = OrderStatus.fromCode(order.status)
        if (status?.isCancellable != true) {
            return Result.failure(Exception("Este pedido ya no se puede cancelar (estado: ${status?.label ?: order.status})."))
        }
        return updateOrder(order.id, cancellationRequest(order))
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
