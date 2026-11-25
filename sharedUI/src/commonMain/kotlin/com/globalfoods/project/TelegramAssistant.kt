package com.globalfoods.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties

internal object TelegramAssistant {
    const val webUrl = "https://t.me/GlobalFoodsMexicobot"
    private const val nativeUrl = "tg://resolve?domain=GlobalFoodsMexicobot"
    const val openError = "No se pudo abrir Telegram ni el navegador. Inténtalo de nuevo."

    // Contrato propuesto al bot: /start pedido_<ID>. El bot valida el acceso y consulta la API.
    fun open(orderId: Int? = null, openUri: (String) -> Boolean): Boolean {
        if (orderId != null && orderId <= 0) return false
        val parameter = orderId?.let { "pedido_$it" }
        val native = if (parameter == null) nativeUrl else "$nativeUrl&start=$parameter"
        val web = if (parameter == null) webUrl else "$webUrl?start=$parameter"
        return openUri(native) || openUri(web)
    }
}

@Composable
internal expect fun rememberTelegramAssistantLauncher(orderId: Int? = null): () -> Boolean

/** Pregunta obligatoria después de un POST exitoso; el pedido ya está guardado. */
@Composable
internal fun OrderCreatedAssistantDialog(orderId: Int, onFinished: () -> Unit) {
    val openAssistant = rememberTelegramAssistantLauncher(orderId)
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        shape = RoundedCornerShape(24.dp),
        containerColor = Background,
        icon = { Icon(Icons.Outlined.Chat, contentDescription = null, tint = Navy) },
        title = { Text("Pedido registrado", color = Navy, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "¿Quieres comunicarte con nuestro asistente para recibir información de tu pedido?",
                    color = Navy,
                    fontSize = 16.sp
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (openAssistant()) onFinished() else error = TelegramAssistant.openError
                },
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Navy, contentColor = Color.White)
            ) { Text("Sí") }
        },
        dismissButton = {
            TextButton(onClick = onFinished) { Text("No", color = Navy) }
        }
    )
}

/** Menú de configuración compartido por los encabezados de la app. */
@Composable
internal fun AppSettingsDialog(onDismissRequest: () -> Unit, onLogout: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(24.dp),
        containerColor = Background,
        title = { Text("Configuración", color = Navy, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Logout, contentDescription = null, tint = Navy)
                    Spacer(Modifier.width(8.dp))
                    Text("Cerrar sesión", color = Navy)
                    Spacer(Modifier.weight(1f))
                }
            }
        },
        confirmButton = {}
    )
}
