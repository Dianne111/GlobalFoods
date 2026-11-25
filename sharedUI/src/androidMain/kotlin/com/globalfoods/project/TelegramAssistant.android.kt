package com.globalfoods.project

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun rememberTelegramAssistantLauncher(orderId: Int?): () -> Boolean {
    val context = LocalContext.current
    return remember(context, orderId) {
        { TelegramAssistant.open(orderId) { uri -> openAssistantUri(context, uri) } }
    }
}

private fun openAssistantUri(context: Context, uri: String): Boolean =
    try {
        // Iniciar directamente evita depender de la visibilidad de paquetes de Android.
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
