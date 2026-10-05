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
                    },
                    onNewOrder = { pantallaActual = "NuevoPedido" },
                    onLogout = {
                        idPedidoSeleccionado = ""
                        pantallaActual = "Login"
                    }
                )
            }
            "NuevoPedido" -> {
                NewOrderScreen(
                    onBack = { pantallaActual = "Dashboard" },
                    onLogout = {
                        idPedidoSeleccionado = ""
                        pantallaActual = "Login"
                    }
                )
            }
            "Detalles" -> {
                val order = orders.firstOrNull { it.number == idPedidoSeleccionado }

                if (order != null) {
                    OrderDetailScreen(
                        order = order,
                        onNavigateBack = {
                            pantallaActual = "Dashboard"
                        }
                    )
                } else {
                    pantallaActual = "Dashboard"
                }
            }
        }
    }
}