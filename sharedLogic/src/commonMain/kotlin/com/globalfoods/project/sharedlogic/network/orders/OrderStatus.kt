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

    /** Solo se cancela antes de salir a reparto: Creado o Surtido (CU-07, RF-PED-03). */
    val isCancellable: Boolean get() = this == Created || this == Supplied

    /**
     * Pedido en proceso: todavía no se entrega ni se cancela (Creado, Surtido o En reparto).
     * Mientras un cliente o una sucursal tenga alguno, no se puede dar de baja (CU-18, CU-22).
     */
    val isInProcess: Boolean get() = this == Created || this == Supplied || this == InDelivery

    /** Mensaje de RF-PED-03 cuando el estado no permite cancelar. */
    val cancelBlockedMessage: String get() = "El pedido no puede cancelarse porque se encuentra $label."

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
