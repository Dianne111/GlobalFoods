package com.globalfoods.project.sharedlogic.network.orders

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Client(
    @SerialName("Id") val id: Int,
    @SerialName("Nombre") val name: String,
    @SerialName("Telefono") private val phonePascal: String? = null,
    @SerialName("telefono") private val phoneLower: String? = null,
    @SerialName("Sucursales") private val branchesPascal: List<Branch> = emptyList(),
    @SerialName("sucursales") private val branchesLower: List<Branch> = emptyList(),
    @SerialName("SucursalesAppVentas") private val branchesTableName: List<Branch> = emptyList(),
    @SerialName("sucursalesAppVentas") private val branchesTableNameLower: List<Branch> = emptyList(),
    @SerialName("Sucursal") private val branchSingular: List<Branch> = emptyList()
) {
    val phone: String
        get() = phonePascal ?: phoneLower.orEmpty()

    val branches: List<Branch>
        get() = sequenceOf(
            branchesPascal,
            branchesLower,
            branchesTableName,
            branchesTableNameLower,
            branchSingular
        ).firstOrNull { it.isNotEmpty() }.orEmpty()
}

@Serializable
data class Branch(
    @SerialName("Id") val id: Int,
    @SerialName("IdCliente") val clientId: Int,
    @SerialName("NombreSucursal") val name: String,
    @SerialName("Direccion") val address: String = "",
    @SerialName("Telefono") val phone: String = "",
    @SerialName("Estatus") val status: Int = 1
)

/** Cuerpo de POST /api/sucursales-clientes y PUT /api/sucursales-clientes/{id} (mismo formato que la web). */
@Serializable
data class BranchRequest(
    @SerialName("idCliente") val clientId: Int,
    @SerialName("NombreSucursal") val name: String,
    @SerialName("Direccion") val address: String,
    @SerialName("Telefono") val phone: String,
    @SerialName("Estatus") val status: Int = 1
)

@Serializable
data class OrderRequest(
    @SerialName("IdCliente") val clientId: Int?,
    @SerialName("IdSucursal") val branchId: Int?,
    @SerialName("Fecha") val date: Int,
    @SerialName("TotalKG") val totalKg: Double,
    @SerialName("Total") val total: Double,
    @SerialName("Estatus") val status: Int,
    @SerialName("Observaciones") val notes: String? = null,
    @SerialName("lineas") val lines: List<OrderLineRequest>
)

@Serializable
data class OrderLineRequest(
    @SerialName("CodigoProducto") val productCode: Int,
    @SerialName("Talla") val size: String,
    @SerialName("Cantidad") val quantity: Double,
    @SerialName("Precio") val price: Double,
    @SerialName("Subtotal") val subtotal: Double
)

@Serializable
data class OrderResponse(
    @SerialName("Id") val id: Int,
    @SerialName("IdCliente") val clientId: Int? = null,
    @SerialName("IdSucursal") val branchId: Int? = null,
    @SerialName("Fecha") val date: Int = 0,
    @SerialName("TotalKG") val totalKg: Double = 0.0,
    @SerialName("Total") val total: Double = 0.0,
    @SerialName("Estatus") val status: Int = 1,
    @SerialName("Observaciones") val notes: String? = null,
    @SerialName("lineas") val lines: List<OrderLineResponse> = emptyList(),
    @SerialName("Cliente") val client: Client? = null
)

/** El mismo pedido, sin cambios, pero con Estatus Cancelado (para PUT /api/pedidos/{id}). */
fun cancellationRequest(order: OrderResponse): OrderRequest = OrderRequest(
    clientId = order.clientId ?: order.client?.id,
    branchId = order.branchId,
    date = order.date,
    totalKg = order.totalKg,
    total = order.total,
    status = OrderStatus.Cancelled.code,
    notes = order.notes,
    lines = order.lines.map { OrderLineRequest(it.productCode, it.size, it.quantity, it.price, it.subtotal) }
)

@Serializable
data class OrderLineResponse(
    @SerialName("Id") val id: Int = 0,
    @SerialName("CodigoProducto") val productCode: Int = 0,
    @SerialName("Talla") val size: String = "",
    @SerialName("Cantidad") val quantity: Double = 0.0,
    @SerialName("Precio") val price: Double = 0.0,
    @SerialName("Subtotal") val subtotal: Double = 0.0
)
