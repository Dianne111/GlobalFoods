package com.globalfoods.project

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.globalfoods.project.sharedlogic.network.auth.AuthRepository
import com.globalfoods.project.sharedlogic.network.orders.OrderResponse

@Composable
fun App() {
    MaterialTheme {
        // Controla en qué pantalla te encuentras
        var pantallaActual by remember { mutableStateOf("Splash") }
        // Guarda el ID del pedido cuando haces clic en uno
        var pedidoSeleccionado by remember { mutableStateOf<Order?>(null) }
        var refreshDashboard by remember { mutableStateOf(0) }
        // Pedido que se está editando (tomado del detalle más reciente)
        var pedidoEnEdicion by remember { mutableStateOf<OrderResponse?>(null) }
        // Pantalla de la barra inferior a la que se regresa al salir del detalle ("Dashboard" o "Pedidos")
        var pantallaLista by remember { mutableStateOf("Dashboard") }

        val abrirPestana: (MainTab) -> Unit = { tab ->
            when (tab) {
                MainTab.Inicio -> pantallaActual = "Dashboard"
                MainTab.Pedidos -> pantallaActual = "Pedidos"
                MainTab.Clientes -> Unit // Pendiente: se desarrolla en su propia rama.
            }
        }
        val cerrarSesion: () -> Unit = {
            // Limpia token, nombre y rol para que el siguiente usuario no herede la sesión.
            AuthRepository().cerrarSesion()
            pedidoSeleccionado = null
            pedidoEnEdicion = null
            pantallaLista = "Dashboard"
            pantallaActual = "Login"
        }
        val abrirDetalle: (Order, String) -> Unit = { order, origen ->
            pedidoSeleccionado = order
            pantallaLista = origen
            pantallaActual = "Detalles"
        }

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
                    onOrderSelected = { order -> abrirDetalle(order, "Dashboard") },
                    onNewOrder = { pantallaActual = "NuevoPedido" },
                    onLogout = cerrarSesion,
                    onTabSelected = abrirPestana
                )
            }
            "Pedidos" -> {
                OrdersScreen(
                    refreshKey = refreshDashboard,
                    onOrderSelected = { order -> abrirDetalle(order, "Pedidos") },
                    onTabSelected = abrirPestana
                )
            }
            "NuevoPedido" -> {
                NewOrderScreen(
                    onBack = { pantallaActual = "Dashboard" },
                    onOrderCreated = {
                        refreshDashboard++
                        pantallaActual = "Dashboard"
                    },
                    onLogout = cerrarSesion
                )
            }
            "Detalles" -> {
                val order = pedidoSeleccionado

                if (order != null) {
                    OrderDetailScreen(
                        order = order,
                        onNavigateBack = {
                            pantallaActual = pantallaLista
                        },
                        onDeleted = {
                            refreshDashboard++
                            pantallaActual = pantallaLista
                        },
                        onEdit = { response ->
                            pedidoEnEdicion = response
                            pantallaActual = "EditarPedido"
                        }
                    )
                } else {
                    pantallaActual = pantallaLista
                }
            }
            "EditarPedido" -> {
                val response = pedidoEnEdicion

                if (response != null) {
                    NewOrderScreen(
                        orderToEdit = response,
                        onBack = {
                            pedidoEnEdicion = null
                            pantallaActual = "Detalles"
                        },
                        onOrderUpdated = { updated ->
                            // Si la API no devuelve el pedido, el detalle lo vuelve a consultar por Id.
                            updated?.let { pedidoSeleccionado = toOrder(it) }
                            pedidoEnEdicion = null
                            refreshDashboard++
                            pantallaActual = "Detalles"
                        },
                        onLogout = cerrarSesion
                    )
                } else {
                    pantallaActual = "Detalles"
                }
            }
        }
    }
}