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
        // Esta variable de estado controla en qué pantalla te encuentras
        var pantallaActual by remember { mutableStateOf("Login") }

        when (pantallaActual) {
            "Login" -> {
                LoginScreen(
                    onLoginSuccess = {
                        // Cuando el login es correcto, cambiamos el estado a Dashboard
                        pantallaActual = "Dashboard"
                    }
                )
            }
            "Dashboard" -> {
                DashboardScreen(
                    onOrderSelected = { orderNumber ->
                        // Aquí en el futuro puedes navegar al detalle del pedido
                    }
                )
            }
        }
    }
}