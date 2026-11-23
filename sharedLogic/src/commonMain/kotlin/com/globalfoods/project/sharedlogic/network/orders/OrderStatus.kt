package com.globalfoods.project.sharedlogic.network.orders

/**
 * Estados de un pedido tal como los guarda la API en la columna Estatus.
 * Almacén gestiona las etapas previas; confirmar el pago en el móvil finaliza la entrega.
 */
enum class OrderStatus(val code: Int, val label: String) {
    Created(1, "Creado"),
    Supplied(2, "Surtido"),
    InDelivery(3, "En reparto"),
    Delivered(4, "Entregado"),
    Cancelled(5, "Cancelado");

    /** Solo un pedido recién creado puede modificarse. */
    val isEditable: Boolean get() = this == Created

    /** Se puede cancelar mientras no se haya entregado (ni cancelado ya). */
    val isCancellable: Boolean get() = this == Created || this == Supplied || this == InDelivery

    /** Solo desde reparto se puede confirmar el pago y finalizar la entrega. */
    val acceptsPayment: Boolean get() = this == InDelivery

    companion object {
        /** Etapas normales del pedido, en orden (Cancelado queda fuera del flujo). */
        val flow: List<OrderStatus> = listOf(Created, Supplied, InDelivery, Delivered)

        fun fromCode(code: Int): OrderStatus? = entries.firstOrNull { it.code == code }
    }
}

val OrderResponse.orderStatus: OrderStatus?
    get() = OrderStatus.fromCode(status)
