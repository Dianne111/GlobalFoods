package com.globalfoods.project

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun rememberAddressLauncher(): (String) -> Boolean {
    val context = LocalContext.current
    return remember(context) { { address -> openAddressInMaps(context, address) } }
}

private fun openAddressInMaps(context: Context, address: String): Boolean {
    val destination = address.trim()
    if (destination.isEmpty()) return false

    val query = Uri.encode(destination)
    val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$query"))
    if (mapIntent.resolveActivity(context.packageManager) != null) {
        val chooser = Intent.createChooser(mapIntent, "Abrir dirección con")
        if (tryOpenActivity(context, chooser)) return true
    }

    val browserIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://www.google.com/maps/search/?api=1&query=$query")
    )
    return tryOpenActivity(context, browserIntent)
}

private fun tryOpenActivity(context: Context, intent: Intent): Boolean =
    try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
