package com.globalfoods.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties

internal object TelegramAssistant {
    // Valor acordado para esta integración: Telegram ofrece Iniciar y envía /start app al pulsarlo.
    private const val startParameter = "app"
    const val webUrl = "https://t.me/GlobalFoodsMexicobot?start=$startParameter"
    private const val nativeUrl = "tg://resolve?domain=GlobalFoodsMexicobot&start=$startParameter"
    const val openError = "No se pudo abrir Telegram ni el navegador. Inténtalo de nuevo."

    // Abre el chat en Telegram; si no está instalado, usa el enlace web del mismo bot.
    fun open(openUri: (String) -> Boolean): Boolean = openUri(nativeUrl) || openUri(webUrl)
}

@Composable
internal expect fun rememberTelegramAssistantLauncher(): () -> Boolean

/** Pregunta obligatoria después de un POST exitoso; el pedido ya está guardado. */
@Composable
internal fun OrderCreatedAssistantDialog(onFinished: () -> Unit) {
    val openAssistant = rememberTelegramAssistantLauncher()
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        shape = RoundedCornerShape(28.dp),
        containerColor = Background,
        icon = { Text("🤖", fontSize = 48.sp) },
        title = {
            Text(
                "¡Pedido registrado!",
                modifier = Modifier.fillMaxWidth(),
                color = Navy,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "¿Te ayudo a conocer el estado de tu pedido?",
                    color = Navy,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Habla con nuestro asistente en Telegram.",
                    color = Navy,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onFinished) { Text("No", color = Navy) }
                Spacer(Modifier.width(14.dp))
                Button(
                    onClick = {
                        if (openAssistant()) onFinished() else error = TelegramAssistant.openError
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Navy, contentColor = Color.White)
                ) { Text("Sí") }
            }
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
