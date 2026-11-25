package com.globalfoods.project.sharedlogic.network.orders

import com.globalfoods.project.sharedlogic.network.core.ApiConflictException
import com.globalfoods.project.sharedlogic.network.core.ensureSuccess
import com.globalfoods.project.sharedlogic.network.core.ensureSuccessWithApiMessage
import com.globalfoods.project.sharedlogic.network.core.ensureWriteSuccess
import com.globalfoods.project.sharedlogic.network.core.globalFoodsClient
import com.globalfoods.project.sharedlogic.network.core.networkErrorMessage
import io.ktor.client.call.body
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import com.globalfoods.project.sharedlogic.network.core.sessionRol
import com.globalfoods.project.sharedlogic.network.auth.UserRole
import com.globalfoods.project.sharedlogic.network.core.sessionToken
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders

class OrderRepository internal constructor(private val client: HttpClient) {
    constructor() : this(globalFoodsClient)
    private val baseUrl = "https://api.globalfoodsmexico.com/api"

    suspend fun getClients(): Result<List<Client>> = request {
        val response = client.get("$baseUrl/clientes/activos")
        ensureSuccess(response, "los clientes")
        response.body()
    }

    suspend fun getBranches(clientId: Int): Result<List<Branch>> = request {
        val response = client.get("$baseUrl/sucursales-clientes") {
            if (sessionRol != UserRole.Client && clientId > 0) {
                header("X-Id-Cliente", clientId.toString())
            }
        }
        ensureSuccess(response, "las sucursales")
        response.body()
    }

    suspend fun createBranch(branch: BranchRequest): Result<Unit> = request {
        validate(branch)
        val response = client.post("$baseUrl/sucursales-clientes") {
            contentType(ContentType.Application.Json)
            setBody(branch)
        }
        if (!response.status.isSuccess()) error("No se pudo registrar la sucursal (HTTP ${response.status.value})")
    }

    suspend fun updateBranch(id: Int, branch: BranchRequest): Result<Unit> = request {
        validate(branch)
        val response = client.put("$baseUrl/sucursales-clientes/$id") {
            contentType(ContentType.Application.Json)
            setBody(branch)
        }
        if (!response.status.isSuccess()) error("No se pudo actualizar la sucursal (HTTP ${response.status.value})")
    }

    /**
     * Baja lógica: la sucursal se conserva con Estatus 0 (o vuelve a 1 al reactivarla).
     * No se usa DELETE para no perder la referencia de los pedidos que ya se entregaron ahí.
     * No revalida nombre ni dirección para poder dar de baja sucursales antiguas incompletas.
     * Si tiene pedidos pendientes, la API responde 409 y falla con [ApiConflictException].
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
        val response = client.put("$baseUrl/sucursales-clientes/${branch.id}") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        val action = if (active) "reactivar" else "dar de baja"
        ensureSuccessWithApiMessage(response, "No se pudo $action la sucursal")
    }

    private fun validate(branch: BranchRequest) {
        require(branch.clientId > 0) { "Tu usuario no está vinculado a un cliente." }
        require(branch.name.isNotBlank()) { "Escribe el nombre de la sucursal." }
        require(branch.address.isNotBlank()) { "Escribe la dirección de la sucursal." }
    }

    suspend fun getOrders(): Result<List<OrderResponse>> = request {
        // Determinamos si el usuario es cliente para usar su ruta exclusiva o la general
        val esCliente = sessionRol == UserRole.Client
        val endpoint = if (esCliente) "$baseUrl/pedidos/mios" else "$baseUrl/pedidos"

        val response = client.get(endpoint)
        ensureSuccess(response, "los pedidos")
        response.body()
    }

    suspend fun getOrder(id: Int): Result<OrderResponse> = request {
        val response = client.get("$baseUrl/pedidos/$id")
        ensureSuccess(response, "el pedido")
        response.body()
    }

    suspend fun createOrder(order: OrderRequest): Result<OrderResponse> = request {
        val initial = order.copy(paymentMethod = 0, deliveryDate = null)
        validate(initial)
        val response = client.post("$baseUrl/pedidos") {
            contentType(ContentType.Application.Json)
            setBody(initial)
        }
        ensureWriteSuccess(response, "el pedido")
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
        val response = client.put("$baseUrl/pedidos/$id") {
            contentType(ContentType.Application.Json)
            setBody(order)
        }
        ensureWriteSuccess(response, "los cambios del pedido")
        try {
            response.body<OrderResponse>().takeIf { it.id == id }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    suspend fun registerPayment(id: Int, method: OrderPaymentMethod, date: Int): Result<OrderResponse> = request {
        // Se consulta la versión más reciente antes de reenviar el encabezado y las líneas.
        val current = getOrder(id).getOrThrow()
        if (current.orderStatus == OrderStatus.Delivered) {
            // Reintento de una entrega ya guardada: no modifica el método ni ninguna fecha.
            require(current.paymentMethod == method.code && current.deliveryDate != null) {
                "El pedido ya está Entregado y su método de pago no se puede modificar. Actualiza el pedido."
            }
            current
        } else {
            val body = paymentRequest(current, method, date)
            updateOrder(id, body).getOrThrow()
            val confirmed = getOrder(id).getOrThrow()
            requireNotNull(confirmed.paymentMethod) {
                "La API aceptó la actualización, pero la consulta del pedido no devolvió MetodoPago para confirmarla."
            }
            requireNotNull(confirmed.deliveryDate) {
                "La API aceptó la actualización, pero la consulta del pedido no devolvió FechaEntrega para confirmarla."
            }
            require(confirmed.paymentMethod == method.code && confirmed.deliveryDate == date &&
                confirmed.date == current.date && confirmed.orderStatus == OrderStatus.Delivered) {
                "La API no confirmó el pago, FechaEntrega o el estado Entregado conservando la fecha de creación. Actualiza el pedido."
            }
            confirmed
        }
    }

    /**
     * Registra la entrega completa con lo que se cobró. Efectivo o tarjeta se guardan como hoy
     * (MetodoPago del pedido); el pago mixto espera la tabla PagoPedidosApp de la API.
     */
    suspend fun registerDelivery(id: Int, payment: DeliveryPayment, date: Int): Result<OrderResponse> =
        when (payment.type) {
            PaymentType.Cash -> registerPayment(id, OrderPaymentMethod.Cash, date)
            PaymentType.Card -> registerPayment(id, OrderPaymentMethod.CardOrTransfer, date)
            PaymentType.Mixed -> Result.failure(IllegalStateException(MIXED_PAYMENT_PENDING_MESSAGE))
            PaymentType.Pending -> Result.failure(IllegalArgumentException("Escribe cuánto se cobró."))
        }

    private fun validate(order: OrderRequest) {
        require(order.paymentMethod in 0..2) { "El método de pago no es válido." }
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
            return Result.failure(
                Exception(status?.cancelBlockedMessage ?: "El pedido no puede cancelarse (estado ${order.status}).")
            )
        }
        return updateOrder(order.id, cancellationRequest(order))
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
