package com.globalfoods.project

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun App() {
    MaterialTheme {
        // Controla en qué pantalla te encuentras
        var pantallaActual by remember { mutableStateOf("Login") }
        // Guarda el ID del pedido cuando haces clic en uno
        var idPedidoSeleccionado by remember { mutableStateOf("") }

        when (pantallaActual) {
            "Login" -> {
                LoginScreen(
                    onLoginSuccess = {
                        pantallaActual = "Dashboard"
                    }
                )
            }
            "Dashboard" -> {
                DashboardScreen(
                    onOrderSelected = { orderNumber ->
                        // Guardamos el número de pedido y cambiamos de pantalla
                        idPedidoSeleccionado = orderNumber
                        pantallaActual = "Detalles"
                    }
                )
            }
            "Detalles" -> {
                // CAMBIO AQUÍ: Usamos OrderDetailScreen y el parámetro onBack
                OrderDetailScreen(
                    orderId = idPedidoSeleccionado,
                    onBack = {
                        pantallaActual = "Dashboard"
                    }
                )
            }
        }
    }
}