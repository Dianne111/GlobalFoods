package com.globalfoods.project

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.globalfoods.project.sharedlogic.network.auth.AuthRepository
import com.globalfoods.project.sharedlogic.network.auth.UserRole
import com.globalfoods.project.sharedlogic.network.clients.AdminClient
import com.globalfoods.project.sharedlogic.network.core.sessionRol
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
        // Pedido anterior que se usa como base en "Repetir último pedido"
        var pedidoBase by remember { mutableStateOf<OrderResponse?>(null) }
        // Cliente abierto desde la pestaña Clientes (administrador/empleado)
        var clienteSeleccionado by remember { mutableStateOf<AdminClient?>(null) }

        val abrirPestana: (MainTab) -> Unit = { tab ->
            when (tab) {
                MainTab.Inicio -> pantallaActual = "Dashboard"
                MainTab.Pedidos -> pantallaActual = "Pedidos"
                MainTab.Clientes -> pantallaActual = "Clientes"
                MainTab.Sucursales -> pantallaActual = "Sucursales"
            }
        }
        val cerrarSesion: () -> Unit = {
            // Limpia token, nombre y rol para que el siguiente usuario no herede la sesión.
            AuthRepository().cerrarSesion()
            pedidoSeleccionado = null
            pedidoEnEdicion = null
            pedidoBase = null
            clienteSeleccionado = null
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
                // El cliente tiene su propio Inicio (sin inventario); asesoras y administradores, el panel de ventas.
                if (sessionRol == UserRole.Client) {
                    ClientHomeScreen(
                        refreshKey = refreshDashboard,
                        onNewOrder = {
                            pedidoBase = null
                            pantallaActual = "NuevoPedido"
                        },
                        onRepeatOrder = { anterior ->
                            pedidoBase = anterior
                            pantallaActual = "NuevoPedido"
                        },
                        onOrderSelected = { order -> abrirDetalle(order, "Dashboard") },
                        onTabSelected = abrirPestana,
                        onLogout = cerrarSesion
                    )
                } else {
                    DashboardScreen(
                        refreshKey = refreshDashboard,
                        onOrderSelected = { order -> abrirDetalle(order, "Dashboard") },
                        onNewOrder = {
                            pedidoBase = null
                            pantallaActual = "NuevoPedido"
                        },
                        onLogout = cerrarSesion,
                        onTabSelected = abrirPestana
                    )
                }
            }
            "Sucursales" -> {
                BranchesScreen(onTabSelected = abrirPestana)
            }
            "Clientes" -> {
                ClientsScreen(
                    refreshKey = refreshDashboard,
                    onClientSelected = { cliente ->
                        clienteSeleccionado = cliente
                        pantallaActual = "DetalleCliente"
                    },
                    onTabSelected = abrirPestana
                )
            }
            "DetalleCliente" -> {
                val cliente = clienteSeleccionado
                if (cliente != null) {
                    ClientDetailScreen(
                        client = cliente,
                        onBack = { pantallaActual = "Clientes" },
                        onChanged = { refreshDashboard++ },
                        onDeactivated = {
                            clienteSeleccionado = null
                            refreshDashboard++
                            pantallaActual = "Clientes"
                        },
                        onOrderSelected = { order -> abrirDetalle(order, "DetalleCliente") }
                    )
                } else {
                    pantallaActual = "Clientes"
                }
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
                    template = pedidoBase,
                    onBack = {
                        pedidoBase = null
                        pantallaActual = "Dashboard"
                    },
                    onOrderCreated = {
                        pedidoBase = null
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
                        onCancelled = {
                            // Se queda en el detalle (ya muestra Cancelado); las listas se recargan al volver.
                            refreshDashboard++
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