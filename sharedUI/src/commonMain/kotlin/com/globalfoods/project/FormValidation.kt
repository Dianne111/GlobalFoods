package com.globalfoods.project

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation

/*
 * Reglas de los formularios del vendedor/administrador, tomadas de los requerimientos
 * RF-CLI-02/03 (clientes) y RF-SUC-01/03 (sucursales) del documento del proyecto.
 * Cada función devuelve el mensaje que se muestra debajo del campo, o null si el dato es válido.
 */

internal const val MAX_CLIENT_NAME = 150
internal const val MAX_BRANCH_NAME = 100
internal const val PHONE_DIGITS = 10
internal const val MIN_USER = 6
internal const val MAX_USER = 20
internal const val MIN_PASSWORD = 8

internal const val REQUIRED_MESSAGE = "Este dato es obligatorio."
internal const val PHONE_MESSAGE = "Capture un teléfono de 10 dígitos."
internal const val USER_MESSAGE = "El usuario debe tener de 6 a 20 caracteres, sin espacios."
internal const val PASSWORD_MESSAGE = "La contraseña debe tener al menos 8 caracteres."
internal const val DUPLICATE_BRANCH_MESSAGE = "Ya existe una sucursal activa con ese nombre."

internal fun requiredError(value: String, maxLength: Int): String? = when {
    value.isBlank() -> REQUIRED_MESSAGE
    value.trim().length > maxLength -> "Máximo $maxLength caracteres."
    else -> null
}

internal fun phoneError(value: String): String? = when {
    value.isBlank() -> REQUIRED_MESSAGE
    value.length != PHONE_DIGITS || !value.all(Char::isDigit) -> PHONE_MESSAGE
    else -> null
}

internal fun userError(value: String): String? = when {
    value.isBlank() -> REQUIRED_MESSAGE
    value.any(Char::isWhitespace) || value.length !in MIN_USER..MAX_USER -> USER_MESSAGE
    else -> null
}

/** Al editar, la contraseña vacía es válida porque se conserva la actual. */
internal fun passwordError(value: String, required: Boolean): String? = when {
    value.isEmpty() -> if (required) REQUIRED_MESSAGE else null
    value.length < MIN_PASSWORD -> PASSWORD_MESSAGE
    else -> null
}

/** Al escribir un teléfono solo se aceptan dígitos, hasta 10. */
internal fun phoneInput(value: String): String = value.filter(Char::isDigit).take(PHONE_DIGITS)

data class ClientFormErrors(
    val name: String? = null,
    val phone: String? = null,
    val user: String? = null,
    val password: String? = null
) {
    val isValid: Boolean get() = listOf(name, phone, user, password).all { it == null }
}

/**
 * [originalUser] es el usuario actual al editar (null al crear). El usuario solo se valida si es
 * nuevo o si cambió, para no impedir editar otros datos de clientes con usuarios anteriores a la regla.
 */
internal fun validateClientForm(
    name: String,
    phone: String,
    user: String,
    password: String,
    originalUser: String?
): ClientFormErrors {
    val isNew = originalUser == null
    return ClientFormErrors(
        name = requiredError(name, MAX_CLIENT_NAME),
        phone = phoneError(phone),
        user = if (isNew || user != originalUser) userError(user) else null,
        password = passwordError(password, required = isNew)
    )
}

data class BranchFormErrors(
    val name: String? = null,
    val address: String? = null,
    val phone: String? = null
) {
    val isValid: Boolean get() = listOf(name, address, phone).all { it == null }
}

/** [otherActiveNames]: nombres de las demás sucursales activas del cliente (sin la que se edita). */
internal fun validateBranchForm(
    name: String,
    address: String,
    phone: String,
    otherActiveNames: List<String>
): BranchFormErrors {
    val duplicated = otherActiveNames.any { it.trim().equals(name.trim(), ignoreCase = true) }
    return BranchFormErrors(
        name = requiredError(name, MAX_BRANCH_NAME) ?: DUPLICATE_BRANCH_MESSAGE.takeIf { duplicated },
        address = requiredError(address, Int.MAX_VALUE),
        phone = phoneError(phone)
    )
}

/**
 * Campo de texto que, si [error] no es null, se marca en rojo (borde y etiqueta) y muestra
 * el mensaje debajo. Si no hay error y se da [hint], muestra esa ayuda en gris.
 */
@Composable
internal fun ValidatedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    modifier: Modifier = Modifier,
    hint: String? = null,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val supporting = error ?: hint
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = supporting?.let { { Text(it) } },
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        modifier = modifier
    )
}
