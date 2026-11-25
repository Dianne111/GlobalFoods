package com.globalfoods.project

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.globalfoods.project.sharedlogic.network.auth.AuthRepository
import com.globalfoods.project.sharedlogic.network.auth.UserRole
import com.globalfoods.project.sharedlogic.network.clients.AdminClient
import com.globalfoods.project.sharedlogic.network.core.sessionRol
import com.globalfoods.project.sharedlogic.network.orders.OrderResponse
import kotlinx.coroutines.launch

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
        // Cliente abierto desde la pestaña Clientes (administrador/vendedor)
        var clienteSeleccionado by remember { mutableStateOf<AdminClient?>(null) }
        // Pedido al que se le registra la entrega y pantalla a la que se regresa al terminar.
        var pedidoEntrega by remember { mutableStateOf<Order?>(null) }
        var origenEntrega by remember { mutableStateOf("Pedidos") }
        // Aviso que muestra la lista de pedidos al regresar (por ejemplo, tras registrar la entrega).
        var avisoPedidos by remember { mutableStateOf<String?>(null) }
        // Posición de las listas: se conserva al ir a un detalle y regresar; se reinicia al cerrar sesión.
        var sesion by remember { mutableStateOf(0) }
        val inicioListState = remember(sesion) { LazyListState() }
        val pedidosListState = remember(sesion) { LazyListState() }
        val clientesListState = remember(sesion) { LazyListState() }
        val listas = remember(sesion) { ListsCache() }

        val scope = rememberCoroutineScope()
        val abrirPestana: (MainTab) -> Unit = { tab ->
            val destino = when (tab) {
                MainTab.Inicio -> "Dashboard"
                MainTab.Pedidos -> "Pedidos"
                MainTab.Clientes -> "Clientes"
                MainTab.Sucursales -> "Sucursales"
            }
            if (destino != pantallaActual) {
                pantallaActual = destino
            } else {
                // Tocar la pestaña en la que ya estás regresa al inicio de su lista.
                val lista = when (destino) {
                    "Dashboard" -> inicioListState.takeIf { sessionRol != UserRole.Client }
                    "Pedidos" -> pedidosListState
                    "Clientes" -> clientesListState
                    else -> null
                }
                lista?.let { scope.launch { it.animateScrollToItem(0) } }
            }
        }
        val cerrarSesion: () -> Unit = {
            // Limpia token, nombre y rol para que el siguiente usuario no herede la sesión.
            AuthRepository().cerrarSesion()
            pedidoSeleccionado = null
            pedidoEnEdicion = null
            pedidoBase = null
            clienteSeleccionado = null
            pedidoEntrega = null
            avisoPedidos = null
            pantallaLista = "Dashboard"
            sesion++
            pantallaActual = "Login"
        }
        val abrirEntrega: (Order, String) -> Unit = { order, origen ->
            pedidoEntrega = order
            origenEntrega = origen
            pantallaActual = "RegistrarEntrega"
        }
        val abrirDetalle: (Order, String) -> Unit = { order, origen ->
            pedidoSeleccionado = order
            pantallaLista = origen
            pantallaActual = "Detalles"
        }

        // Botón "atrás" del sistema: regresa como la flecha de cada pantalla. En Inicio y en el login
        // cierra la app (comportamiento normal de Android); Nuevo/Editar pedido lo atienden ellas mismas.
        val atras: (() -> Unit)? = when (pantallaActual) {
            "Pedidos", "Clientes", "Sucursales" -> { { pantallaActual = "Dashboard" } }
            "Detalles" -> { { pantallaActual = pantallaLista } }
            "DetalleCliente" -> { { pantallaActual = "Clientes" } }
            else -> null
        }
        PlatformBackHandler(enabled = atras != null) { atras?.invoke() }

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
                        onTabSelected = abrirPestana,
                        listState = inicioListState,
                        cache = listas
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
                    onTabSelected = abrirPestana,
                    listState = clientesListState,
                    cache = listas
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
                    onTabSelected = abrirPestana,
                    listState = pedidosListState,
                    cache = listas,
                    onRegisterDelivery = { order -> abrirEntrega(order, "Pedidos") },
                    notice = avisoPedidos,
                    onNoticeShown = { avisoPedidos = null }
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
                        },
                        onRegisterDelivery = { abrirEntrega(it, "Detalles") }
                    )
                } else {
                    pantallaActual = pantallaLista
                }
            }
            "RegistrarEntrega" -> {
                val order = pedidoEntrega

                if (order != null) {
                    RegisterDeliveryScreen(
                        order = order,
                        onBack = {
                            pedidoEntrega = null
                            pantallaActual = origenEntrega
                        },
                        onDelivered = { updated ->
                            pedidoEntrega = null
                            refreshDashboard++
                            if (origenEntrega == "Detalles") {
                                pedidoSeleccionado = toOrder(updated)
                            } else {
                                avisoPedidos = "Pedido entregado exitosamente"
                            }
                            pantallaActual = origenEntrega
                        }
                    )
                } else {
                    pantallaActual = origenEntrega
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