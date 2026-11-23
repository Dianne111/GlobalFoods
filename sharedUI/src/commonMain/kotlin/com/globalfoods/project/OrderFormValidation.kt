package com.globalfoods.project

/*
 * Validación del formulario de pedido del vendedor/administrador (RF-PED-01). Cada campo devuelve
 * el mensaje que se muestra debajo de él, o null si es válido. La vista del cliente no la usa.
 */

internal const val MAX_ORDER_NOTES = 250
internal const val MAX_SPORADIC_CLIENT_NAME = 150

internal const val SELECT_CLIENT_TYPE_MESSAGE = "Seleccione el tipo de cliente."
internal const val SELECT_CLIENT_MESSAGE = "Seleccione un cliente."
internal const val ADD_PRODUCT_MESSAGE = "Agregue al menos un producto al pedido."
internal const val SELECT_BRANCH_MESSAGE = "Seleccione la sucursal de entrega."
internal const val POSTAL_CODE_MESSAGE = "Capture un código postal de 5 dígitos."
internal const val SELECT_DATE_MESSAGE = "Seleccione la fecha de creación."
internal const val PAST_DATE_MESSAGE = "La fecha de creación no puede ser anterior a hoy."

internal data class OrderFormErrors(
    val clientType: String? = null,
    val client: String? = null,
    val phone: String? = null,
    val products: String? = null,
    val branch: String? = null,
    val street: String? = null,
    val neighborhood: String? = null,
    val postalCode: String? = null,
    val date: String? = null,
    val notes: String? = null
) {
    val isValid: Boolean
        get() = listOf(clientType, client, phone, products, branch, street, neighborhood, postalCode, date, notes)
            .all { it == null }
}

/**
 * [todayMillis] es el día de hoy; al editar ([isEditing]) se permite conservar una fecha anterior,
 * igual que el selector de fecha.
 */
internal fun validateOrderForm(form: NewOrderFormState, isEditing: Boolean, todayMillis: Long): OrderFormErrors {
    val registered = form.clientType == "Cliente registrado"
    val sporadic = form.clientType == "Cliente esporádico"
    val toAddress = form.deliveryMode != "Recolección"
    val dateMillis = dateToMillis(form.deliveryDate)
    return OrderFormErrors(
        clientType = SELECT_CLIENT_TYPE_MESSAGE.takeIf { form.clientType.isBlank() },
        client = when {
            registered && form.clientId == null -> SELECT_CLIENT_MESSAGE
            sporadic -> requiredError(form.client, MAX_SPORADIC_CLIENT_NAME)
            else -> null
        },
        // El celular del cliente registrado viene de su ficha y no se edita aquí.
        phone = if (sporadic) phoneError(form.phone) else null,
        products = ADD_PRODUCT_MESSAGE.takeIf {
            form.quantities.values.none { (it.toDoubleOrNull() ?: 0.0) > 0.0 }
        },
        branch = SELECT_BRANCH_MESSAGE.takeIf { toAddress && registered && form.branchId == null },
        street = if (toAddress && sporadic) requiredError(form.street, Int.MAX_VALUE) else null,
        neighborhood = if (toAddress && sporadic) requiredError(form.neighborhood, Int.MAX_VALUE) else null,
        postalCode = when {
            !toAddress || !sporadic -> null
            form.postalCode.isBlank() -> REQUIRED_MESSAGE
            form.postalCode.length != 5 || !form.postalCode.all(Char::isDigit) -> POSTAL_CODE_MESSAGE
            else -> null
        },
        date = when {
            dateMillis == null -> SELECT_DATE_MESSAGE
            !isEditing && !isSelectableCreationDate(dateMillis, todayMillis) -> PAST_DATE_MESSAGE
            else -> null
        },
        notes = "Máximo $MAX_ORDER_NOTES caracteres.".takeIf { form.notes.length > MAX_ORDER_NOTES }
    )
}
