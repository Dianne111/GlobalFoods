package com.globalfoods.project.sharedlogic.network.orders

import kotlin.math.abs
import kotlin.math.roundToLong

/** TipoPago de la tabla PagoPedidosApp que está preparando la API. */
enum class PaymentType(val code: Int, val label: String) {
    Pending(0, "Pendiente"),
    Cash(1, "Efectivo"),
    Card(2, "Tarjeta"),
    Mixed(3, "Mixto");

    companion object {
        fun fromCode(code: Int?): PaymentType? = entries.firstOrNull { it.code == code }
    }
}

/** Lo que se cobró al entregar: efectivo, tarjeta o ambos (pago mixto). */
data class DeliveryPayment(val cash: Double, val card: Double) {
    val type: PaymentType
        get() = when {
            cash > 0.0 && card > 0.0 -> PaymentType.Mixed
            cash > 0.0 -> PaymentType.Cash
            card > 0.0 -> PaymentType.Card
            else -> PaymentType.Pending
        }
}

/** Resultado de revisar los montos capturados: el pago listo para guardar o el error a mostrar. */
data class DeliveryPaymentCheck(val payment: DeliveryPayment?, val error: String?)

/** Mientras la API no tenga la tabla de pagos, el pago mixto no se puede guardar. */
const val MIXED_PAYMENT_PENDING_MESSAGE =
    "El pago mixto se podrá guardar cuando la API tenga la tabla de pagos. Por ahora cobra todo en efectivo o todo con tarjeta."

/** Deja solo dígitos y un punto decimal con hasta dos decimales, para los campos de monto. */
fun sanitizeAmountInput(text: String): String {
    val builder = StringBuilder()
    var hasDot = false
    var decimals = 0
    for (char in text) {
        when {
            char.isDigit() && (!hasDot || decimals < 2) -> {
                builder.append(char)
                if (hasDot) decimals++
            }
            char == '.' && !hasDot -> {
                hasDot = true
                builder.append(if (builder.isEmpty()) "0." else ".")
            }
        }
    }
    return builder.toString()
}

/** Monto escrito por el vendedor; vacío cuenta como 0. Devuelve null si no es un número. */
fun parseAmount(text: String): Double? = text.trim().ifEmpty { "0" }.toDoubleOrNull()

/** Monto para un campo de captura: sin signo de pesos ni comas, con dos decimales. */
fun amountInput(value: Double): String {
    val cents = (value * 100).roundToLong().coerceAtLeast(0)
    return "${cents / 100}.${(cents % 100).toString().padStart(2, '0')}"
}

/** Al escribir el efectivo, lo que falta para el total se llena solo en tarjeta. */
fun remainingForCard(total: Double, cashText: String): String {
    val cash = parseAmount(cashText) ?: return ""
    return amountInput((total - cash).coerceAtLeast(0.0))
}

private fun toCents(value: Double): Long = (value * 100).roundToLong()

private fun money(cents: Long): String {
    val whole = (cents / 100).toString().reversed().chunked(3).joinToString(",").reversed()
    return "$$whole.${(cents % 100).toString().padStart(2, '0')}"
}

/** Revisa que los montos sean números y que juntos sumen exactamente el total del pedido. */
fun checkDeliveryPayment(total: Double, cashText: String, cardText: String): DeliveryPaymentCheck {
    val cash = parseAmount(cashText)
        ?: return DeliveryPaymentCheck(null, "Escribe un monto válido en Efectivo.")
    val card = parseAmount(cardText)
        ?: return DeliveryPaymentCheck(null, "Escribe un monto válido en Tarjeta.")
    if (cash < 0.0 || card < 0.0) {
        return DeliveryPaymentCheck(null, "Los montos no pueden ser negativos.")
    }
    val difference = toCents(total) - (toCents(cash) + toCents(card))
    return when {
        toCents(cash) + toCents(card) == 0L ->
            DeliveryPaymentCheck(null, "Escribe cuánto se cobró en efectivo o con tarjeta.")
        difference > 0 ->
            DeliveryPaymentCheck(null, "Faltan ${money(difference)} por asignar a un método de pago.")
        difference < 0 ->
            DeliveryPaymentCheck(null, "Los montos suman ${money(abs(difference))} más que el total.")
        else -> DeliveryPaymentCheck(DeliveryPayment(cash, card), null)
    }
}
