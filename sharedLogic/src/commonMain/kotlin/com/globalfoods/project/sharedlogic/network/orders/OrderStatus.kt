package com.globalfoods.project.sharedlogic.network.orders

/**
 * Estados de un pedido tal como los guarda la API en la columna Estatus.
 * La app solo los lee: el cambio de estado lo hacen almacén y reparto en el sistema.
 */
enum class OrderStatus(val code: Int, val label: String) {
    Created(1, "Creado"),
    Supplied(2, "Surtido"),
    InDelivery(3, "En reparto"),
    Delivered(4, "Entregado"),
    Cancelled(5, "Cancelado");

    /** Solo un pedido recién creado puede modificarse. */
    val isEditable: Boolean get() = this == Created

    /** Al entregarse, el vendedor o administrador registra cómo pagó el cliente. */
    val acceptsPayment: Boolean get() = this == Delivered

    companion object {
        /** Etapas normales del pedido, en orden (Cancelado queda fuera del flujo). */
        val flow: List<OrderStatus> = listOf(Created, Supplied, InDelivery, Delivered)

        fun fromCode(code: Int): OrderStatus? = entries.firstOrNull { it.code == code }
    }
}

val OrderResponse.orderStatus: OrderStatus?
    get() = OrderStatus.fromCode(status)
