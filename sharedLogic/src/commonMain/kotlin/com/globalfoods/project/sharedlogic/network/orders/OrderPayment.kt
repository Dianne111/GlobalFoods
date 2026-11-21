package com.globalfoods.project.sharedlogic.network.orders

enum class OrderPaymentMethod(val code: Int, val label: String) {
    Cash(1, "Efectivo"),
    CardOrTransfer(2, "Tarjeta / transferencia");

    companion object {
        fun fromCode(code: Int?): OrderPaymentMethod? = entries.firstOrNull { it.code == code }
    }
}

fun paymentMethodLabel(code: Int?): String = when (code) {
    0 -> "Pendiente"
    null -> "Sin información"
    else -> OrderPaymentMethod.fromCode(code)?.label ?: "Método $code"
}

/** Para las pruebas actuales, Fecha se reemplaza con el día de captura del pago. */
fun paymentRequest(order: OrderResponse, method: OrderPaymentMethod, date: Int): OrderRequest {
    require(order.orderStatus?.acceptsPayment == true) { "Solo se puede registrar el pago de un pedido En reparto." }
    val year = date / 10000
    val month = date / 100 % 100
    val day = date % 100
    val maximumDay = when (month) {
        2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }
    require(year in 1900..2100 && month in 1..12 && day in 1..maximumDay) { "La fecha de captura no es válida." }
    return order.toRequest().copy(date = date, paymentMethod = method.code)
}
