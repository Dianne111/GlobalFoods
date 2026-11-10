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
        var pantallaActual by remember { mutableStateOf("Splash") }
        // Guarda el ID del pedido cuando haces clic en uno
        var pedidoSeleccionado by remember { mutableStateOf<Order?>(null) }
        var refreshDashboard by remember { mutableStateOf(0) }

        when (pantallaActual) {
            "Splash" -> {
                SplashScreen(
                    onFinished = { pantallaActual = "Login" }
                )
            }
            "Login" -> {
                LoginScreen(
                    onLoginSuccess = {
                        pantallaActual = "Dashboard"
                    }
                )
            }
            "Dashboard" -> {
                DashboardScreen(
                    refreshKey = refreshDashboard,
                    onOrderSelected = { order ->
                        pedidoSeleccionado = order
                        pantallaActual = "Detalles"
                    },
                    onNewOrder = { pantallaActual = "NuevoPedido" },
                    onLogout = {
                        pedidoSeleccionado = null
                        pantallaActual = "Login"
                    }
                )
            }
            "NuevoPedido" -> {
                NewOrderScreen(
                    onBack = { pantallaActual = "Dashboard" },
                    onOrderCreated = {
                        refreshDashboard++
                        pantallaActual = "Dashboard"
                    },
                    onLogout = {
                        pedidoSeleccionado = null
                        pantallaActual = "Login"
                    }
                )
            }
            "Detalles" -> {
                val order = pedidoSeleccionado

                if (order != null) {
                    OrderDetailScreen(
                        order = order,
                        onNavigateBack = {
                            pantallaActual = "Dashboard"
                        },
                        onDeleted = {
                            refreshDashboard++
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