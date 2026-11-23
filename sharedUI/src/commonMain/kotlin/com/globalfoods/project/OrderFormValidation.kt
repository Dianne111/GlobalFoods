package com.globalfoods.project

import com.globalfoods.project.sharedlogic.network.dashboard.ArticuloInventario

/*
 * Validación del formulario de pedido del vendedor/administrador (RF-PED-01). Cada campo devuelve
 * el mensaje que se muestra debajo de él, o null si es válido. La vista del cliente no la usa.
 */

internal const val MAX_ORDER_NOTES = 250

internal const val SELECT_CLIENT_MESSAGE = "Seleccione un cliente."
internal const val ADD_PRODUCT_MESSAGE = "Agregue al menos un producto al pedido."
internal const val SELECT_BRANCH_MESSAGE = "Seleccione la sucursal de entrega."
internal const val SELECT_DATE_MESSAGE = "Seleccione la fecha de creación."
internal const val PAST_DATE_MESSAGE = "La fecha de creación no puede ser anterior a hoy."

internal data class OrderFormErrors(
    val client: String? = null,
    val products: String? = null,
    val branch: String? = null,
    val date: String? = null,
    val notes: String? = null,
    /** Cantidad en cero o mayor que la disponible, por talla (se muestra debajo de cada producto). */
    val lines: Map<String, String> = emptyMap()
) {
    val isValid: Boolean
        get() = listOf(client, products, branch, date, notes)
            .all { it == null } && lines.isEmpty()
}

/**
 * [todayMillis] es el día de hoy; al editar ([isEditing]) se permite conservar una fecha anterior,
 * igual que el selector de fecha.
 */
internal fun validateOrderForm(
    form: NewOrderFormState,
    isEditing: Boolean,
    todayMillis: Long,
    inventory: List<ArticuloInventario> = emptyList()
): OrderFormErrors {
    val toAddress = form.deliveryMode != "Recolección"
    val dateMillis = dateToMillis(form.deliveryDate)
    return OrderFormErrors(
        client = SELECT_CLIENT_MESSAGE.takeIf { (form.clientId ?: 0) <= 0 },
        // Los productos se agregan al pedido y luego se ajusta su cantidad: basta con que haya alguno.
        products = ADD_PRODUCT_MESSAGE.takeIf { form.quantities.isEmpty() },
        branch = SELECT_BRANCH_MESSAGE.takeIf { toAddress && (form.branchId ?: 0) <= 0 },
        date = when {
            dateMillis == null -> SELECT_DATE_MESSAGE
            !isEditing && !isSelectableCreationDate(dateMillis, todayMillis) -> PAST_DATE_MESSAGE
            else -> null
        },
        notes = "Máximo $MAX_ORDER_NOTES caracteres.".takeIf { form.notes.length > MAX_ORDER_NOTES },
        lines = zeroQuantityErrors(form.quantities) + stockErrors(form.quantities, inventory)
    )
}
