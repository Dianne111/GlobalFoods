package com.globalfoods.project

import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import globalfoods.sharedui.generated.resources.Res
import globalfoods.sharedui.generated.resources.global_foods_logo
import org.jetbrains.compose.resources.painterResource

// Imports de red
import com.globalfoods.project.sharedlogic.network.auth.UserRole
import com.globalfoods.project.sharedlogic.network.core.sessionIdCliente
import com.globalfoods.project.sharedlogic.network.core.sessionNombreUsuario
import com.globalfoods.project.sharedlogic.network.core.sessionRol
import com.globalfoods.project.sharedlogic.network.dashboard.DashboardRepository
import com.globalfoods.project.sharedlogic.network.dashboard.ArticuloInventario
import com.globalfoods.project.sharedlogic.network.orders.Client
import com.globalfoods.project.sharedlogic.network.orders.OrderObservations
import com.globalfoods.project.sharedlogic.network.orders.OrderRepository
import com.globalfoods.project.sharedlogic.network.orders.OrderResponse

internal val Navy = Color(0xFF143D58)
internal val Background = Color(0xFFF1F8FA)
internal val Green = Color(0xFF0DB982)
internal val Gray = Color(0xFF64748B)
private val CardShape = RoundedCornerShape(12.dp)
internal val DashboardHorizontalPadding = 16.dp
internal val DashboardHeaderHeight = 123.dp
private val InventoryHeaderColor = Color(0xFFF8FAFC)
private val InventoryHeaders = listOf("TALLA", "PRECIO", "MASTER", "STOCK")

@Composable
fun DashboardScreen(
    refreshKey: Int = 0,
    onOrderSelected: (Order) -> Unit = {},
    onNewOrder: () -> Unit = {},
    onLogout: () -> Unit = {},
    onTabSelected: (MainTab) -> Unit = {}
) {
    val listState = rememberLazyListState()

    val dashboardRepository = remember { DashboardRepository() }
    val orderRepository = remember { OrderRepository() }

    var inventarioList by remember { mutableStateOf<List<ArticuloInventario>>(emptyList()) }
    var isLoadingInventario by remember { mutableStateOf(true) }
    var errorMensaje by remember { mutableStateOf<String?>(null) }
    var orderItems by remember { mutableStateOf<List<Order>>(emptyList()) }
    var orderError by remember { mutableStateOf<String?>(null) }
    var ordersReloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        val resultado = dashboardRepository.obtenerInventario()
        isLoadingInventario = false

        resultado.fold(
            onSuccess = { datos -> inventarioList = datos },
            onFailure = { error -> errorMensaje = error.message }
        )
    }

    LaunchedEffect(refreshKey, ordersReloadKey) {
        orderError = null
        loadOrders(
            orderRepository,
            onOrders = { orderItems = it },
            onError = { orderError = it }
        )
    }

    Scaffold(
        containerColor = Background,
        topBar = { DashboardHeader(onLogout) },
        bottomBar = { DashboardNavigation(MainTab.Inicio, onTabSelected) }
    ) { insets ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .align(Alignment.Center)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = DashboardHorizontalPadding),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    item {
                        Spacer(Modifier.height(16.dp))
                        NewOrderButton(onClick = onNewOrder)
                        Spacer(Modifier.height(17.dp))
                        SectionLabel("INVENTARIO")
                        Spacer(Modifier.height(8.dp))

                        if (isLoadingInventario) {
                            Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Green)
                            }
                        } else if (errorMensaje != null) {
                            Text(text = errorMensaje!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
                        } else {
                            InventoryTable(inventarioList)
                        }

                        Spacer(Modifier.height(18.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SectionLabel("PEDIDOS ACTUALES")
                            Spacer(Modifier.weight(1f))
                            // Los estados vienen de la API; este botón vuelve a consultarlos.
                            TextButton(onClick = { ordersReloadKey++ }) {
                                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Actualizar", fontSize = 12.sp)
                            }
                        }
                    }

                    orderError?.let { message ->
                        item {
                            Column {
                                Text(message, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                                TextButton(onClick = { ordersReloadKey++ }) { Text("Reintentar") }
                            }
                        }
                    }

                    items(orderItems) { order ->
                        OrderCard(order, onClick = { onOrderSelected(order) })
                        Spacer(Modifier.height(10.dp))
                    }

                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "GLOBAL FOODS MÉXICO © 2026",
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            color = Navy.copy(alpha = .65f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                }
            }

        }
    }
}

/**
 * Consulta los pedidos y los entrega primero tal como llegan; si a alguno le falta el nombre del cliente,
 * consulta el catálogo de clientes y los vuelve a entregar ya completos.
 */
internal suspend fun loadOrders(
    orderRepository: OrderRepository,
    onOrders: (List<Order>) -> Unit,
    onError: (String) -> Unit
) {
    orderRepository.getOrders().fold(
        onSuccess = { all ->
            val response = visibleToCurrentUser(all)
            // Un cliente no puede (ni debe) consultar el catálogo de clientes: sus pedidos llevan su propio nombre.
            val ownClient = ownClientForSession()
            if (ownClient != null) {
                val clients = mapOf(ownClient.id to ownClient)
                onOrders(response.map { toOrder(it, clients) })
                // Sus sucursales dan el nombre del destino a los pedidos que solo traen IdSucursal.
                orderRepository.getBranches(ownClient.id).onSuccess { branches ->
                    val names = branches.associate { it.id to it.name }
                    onOrders(response.map { toOrder(it, clients, names) })
                }
                return@fold
            }
            onOrders(response.map { toOrder(it) })
            val needsClients = response.any {
                it.clientId != null && it.client?.name.isNullOrBlank() &&
                    OrderObservations.decode(it.notes).delivery?.clientName.isNullOrBlank()
            }
            if (needsClients) {
                orderRepository.getClients().fold(
                    onSuccess = { clients ->
                        val clientsById = clients.associateBy { it.id }
                        onOrders(response.map { toOrder(it, clientsById) })
                    },
                    onFailure = { onError("No se pudieron cargar los nombres de los clientes.") }
                )
            }
        },
        onFailure = { onError("No se pudieron cargar los pedidos.") }
    )
}

/** Para el rol Cliente, su propio cliente armado con los datos de la sesión; null para los demás roles. */
internal fun ownClientForSession(
    role: UserRole = sessionRol,
    clientId: Int? = sessionIdCliente,
    userName: String? = sessionNombreUsuario
): Client? =
    if (role == UserRole.Client && clientId != null) Client(id = clientId, name = userName.orEmpty()) else null

/**
 * Un Cliente solo ve sus pedidos. Esto es solo para la pantalla: la protección real debe hacerla
 * la API filtrando por el IdCliente del token. Sin IdCliente en la sesión no se muestra ninguno.
 */
internal fun visibleToCurrentUser(
    orders: List<OrderResponse>,
    role: UserRole = sessionRol,
    clientId: Int? = sessionIdCliente
): List<OrderResponse> =
    if (role != UserRole.Client) orders
    else orders.filter { clientId != null && (it.clientId ?: it.client?.id) == clientId }

/** Convierte la fecha de la API (yyyyMMdd) a dd/MM/yyyy. */
internal fun formatOrderDate(date: Int?): String? {
    val digits = date?.toString()?.takeIf { it.length == 8 && it.all(Char::isDigit) } ?: return null
    return "${digits.substring(6, 8)}/${digits.substring(4, 6)}/${digits.substring(0, 4)}"
}

internal fun toOrder(
    response: OrderResponse,
    clientsById: Map<Int, Client> = emptyMap(),
    branchNames: Map<Int, String> = emptyMap()
): Order {
    val client = response.client?.takeIf { it.name.isNotBlank() }
        ?: response.clientId?.let { clientsById[it] }
        ?: response.client
    val delivery = OrderObservations.decode(response.notes).delivery
    val clientName = delivery?.clientName?.takeIf { it.isNotBlank() }
        ?: client?.name?.takeIf { it.isNotBlank() }
        ?: "Nombre no disponible"
    return Order(
        number = "#${response.id}",
        client = clientName,
        total = "$${response.total}",
        items = "${response.lines.size} Items",
        status = statusLabel(response.status),
        statusColor = statusColor(response.status),
        elapsed = formatOrderDate(response.date)?.let { "Entrega $it" } ?: "Sin fecha",
        accent = statusColor(response.status),
        totalKg = "${response.totalKg} kg",
        products = response.lines.map { OrderProduct(it.size, it.price, it.quantity, it.subtotal) },
        id = response.id,
        response = response.copy(client = client),
        destination = when {
            delivery?.method == "Recolección" -> "Recolección"
            !delivery?.branchName.isNullOrBlank() -> delivery?.branchName.orEmpty()
            else -> response.branchId?.let(branchNames::get).orEmpty()
        }
    )
}

@Composable
private fun InventoryTable(articulos: List<ArticuloInventario>) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Color.White)
            .heightIn(max = 240.dp)
    ) {
        // ENCABEZADO FIJO
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp) // Un poco más de altura para el encabezado
                .background(InventoryHeaderColor),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InventoryHeaders.forEach { TableCell(it, isHeader = true) }
        }

        // CUERPO CON SCROLL Y BARRA LATERAL
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .simpleVerticalScrollbar(scrollState) // <-- Barra de scroll activada
        ) {
            if (articulos.isEmpty()) {
                Text(
                    "No hay inventario disponible",
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    color = Gray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            } else {
                articulos.forEach { articulo ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 40.dp) // Altura mínima elástica para textos largos
                            .background(Color.White)
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            articulo.talla,
                            "$${articulo.precio}",
                            articulo.master.toString(),
                            articulo.cantidadKg.toString()
                        ).forEach { TableCell(it, isHeader = false) }
                    }
                    HorizontalDivider(color = Color(0xFFF1F8FA), thickness = 1.dp)
                }
            }
        }
    }
}

@Composable
private fun DashboardHeader(onLogout: () -> Unit) {
    var showSettings by remember { mutableStateOf(false) }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("Configuración", color = Navy, fontWeight = FontWeight.Bold) },
            text = {
                TextButton(onClick = onLogout) {
                    Icon(Icons.Outlined.Logout, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Cerrar sesión")
                }
            },
            confirmButton = {}
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(DashboardHeaderHeight)
            .background(Navy)
            .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 12.dp)
            .statusBarsPadding()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(Res.drawable.global_foods_logo),
                contentDescription = "Global Foods México",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(width = 100.dp, height = 50.dp)
            )
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = { showSettings = true },
                modifier = Modifier.size(32.dp).background(Color.White.copy(alpha = .12f), RoundedCornerShape(50))
            ) {
                Icon(Icons.Outlined.Settings, "Configuración", tint = Color.White, modifier = Modifier.size(19.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("PANEL DE CONTROL", color = Green, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = .5.sp)
        Text("DASHBOARD DE VENTAS", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun NewOrderButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().height(40.dp).clip(CardShape).background(Green)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text("+  Nuevo Pedido", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SectionLabel(label: String) {
    Text(label, color = Navy, fontSize = 12.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun RowScope.TableCell(value: String, isHeader: Boolean) {
    Text(
        text = value,
        modifier = Modifier.weight(1f).padding(horizontal = 4.dp), // Margen para evitar choques
        textAlign = TextAlign.Center,
        color = if (isHeader) Color(0xFF475569) else Color(0xFF374151),
        fontSize = if (isHeader) 9.sp else 12.sp,
        fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Medium,
        lineHeight = 14.sp // Mejora la lectura si el texto baja a dos líneas
    )
}

enum class MainTab(val label: String) {
    Inicio("INICIO"),
    Pedidos("PEDIDOS"),
    Clientes("CLIENTES"),
    Sucursales("SUCURSALES")
}

/** Pestañas que ve cada rol. El cliente administra sus sucursales en lugar del catálogo de clientes. */
internal fun tabsFor(role: UserRole): List<MainTab> = when (role) {
    UserRole.Administrator, UserRole.Employee -> listOf(MainTab.Inicio, MainTab.Pedidos, MainTab.Clientes)
    UserRole.Client -> listOf(MainTab.Inicio, MainTab.Pedidos, MainTab.Sucursales)
    UserRole.Reports -> listOf(MainTab.Inicio)
}

private val MainTab.icon
    get() = when (this) {
        MainTab.Inicio -> Icons.Outlined.Home
        MainTab.Pedidos -> Icons.Outlined.ReceiptLong
        MainTab.Clientes -> Icons.Outlined.People
        MainTab.Sucursales -> Icons.Outlined.Storefront
    }

@Composable
internal fun DashboardNavigation(
    selected: MainTab = MainTab.Inicio,
    onTabSelected: (MainTab) -> Unit = {}
) {
    NavigationBar(
        modifier = Modifier.navigationBarsPadding().height(66.dp),
        containerColor = Color.White,
        tonalElevation = 0.dp
    ) {
        tabsFor(sessionRol).forEach { tab ->
            NavigationBarItem(
                selected = tab == selected,
                onClick = { if (tab != selected) onTabSelected(tab) },
                icon = { Icon(tab.icon, tab.label, modifier = Modifier.size(19.dp)) },
                label = { Text(tab.label, fontSize = 8.sp) }
            )
        }
    }
}

@Composable
internal fun OrderCard(order: Order, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        // La altura la define el contenido (antes era fija y se cortaba el tercer renglón).
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(order.accent))
            Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp).fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(order.number, color = Navy, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(order.status, color = order.statusColor, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.background(order.statusColor.copy(alpha = .12f), RoundedCornerShape(5.dp)).padding(horizontal = 7.dp, vertical = 4.dp))
                }
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // El cliente ya sabe que los pedidos son suyos: se le muestra a dónde van.
                    val (label, value) = if (sessionRol == UserRole.Client) {
                        "Entrega en: " to order.destination.ifBlank { "Sucursal no indicada" }
                    } else {
                        "Cliente: " to order.client
                    }
                    Text(label, color = Gray, fontSize = 10.sp)
                    Text(value, color = Navy, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("Total: ${order.total}  ›", color = Navy, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(order.items, color = Gray, fontSize = 10.sp, modifier = Modifier.weight(1f))
                    Icon(Icons.Outlined.LocalShipping, null, tint = Gray, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(order.elapsed, color = Gray, fontSize = 10.sp)
                }
            }
        }
    }
}

// MODELOS
data class OrderProduct(
    val name: String,
    val price: Double,
    val quantity: Double,
    val subtotal: Double
)

data class Order(
    val number: String,
    val client: String,
    val total: String,
    val items: String,
    val status: String,
    val statusColor: Color,
    val elapsed: String,
    val accent: Color,
    val totalKg: String = "0 kg",
    val products: List<OrderProduct> = emptyList(),
    val id: Int = 0,
    val response: OrderResponse? = null,
    /** A dónde va el pedido: nombre de la sucursal o "Recolección". */
    val destination: String = ""
)

val orders = listOf(
    Order(
        number = "#12345",
        client = "Victoria Ontiveros",
        total = "$560.00",
        items = "2 Items",
        status = "PENDIENTE",
        statusColor = Color(0xFFFFA800),
        elapsed = "hace 5 min",
        accent = Color(0xFFFFA000),
        totalKg = "15.5 kg",
        products = listOf(
            OrderProduct("Camarón Talla 41/50", 280.0, 2.0, 560.0)
        )
    ),
    Order(
        number = "#12346",
        client = "Restaurante Mar y Tierra",
        total = "$1,450.00",
        items = "5 Items",
        status = "EN PREPARACIÓN",
        statusColor = Color(0xFF367CF4),
        elapsed = "hace 20 min",
        accent = Color(0xFF367CF4),
        totalKg = "32.0 kg",
        products = listOf(
            OrderProduct("Pulpo Cocido", 450.0, 2.0, 900.0),
            OrderProduct("Filete de Pescado", 183.33, 3.0, 550.0)
        )
    ),
    Order(
        number = "#12347",
        client = "Mariscos El Faro",
        total = "$980.00",
        items = "3 Items",
        status = "EN REPARTO",
        statusColor = Color(0xFF7026DF),
        elapsed = "hace 1 hora",
        accent = Color(0xFF7026DF),
        totalKg = "24.5 kg",
        products = listOf(
            OrderProduct("Callo de Hacha", 980.0, 1.0, 980.0)
        )
    )
)

// FUNCIÓN EXTENSORA PARA DIBUJAR LA BARRA DE SCROLL
fun Modifier.simpleVerticalScrollbar(
    state: ScrollState,
    width: Dp = 6.dp,
    paddingRight: Dp = 4.dp, // La despegamos del borde derecho
    thumbColor: Color = Color(0xFF94A3B8)
): Modifier = drawWithContent {
    drawContent()

    if (state.maxValue > 0) {
        val thumbHeight = 40.dp.toPx()
        val trackHeight = this.size.height
        val scrollableArea = trackHeight - thumbHeight
        val scrollFraction = state.value.toFloat() / state.maxValue.toFloat()
        val scrollbarY = scrollableArea * scrollFraction

        drawRoundRect(
            color = thumbColor,
            topLeft = Offset(this.size.width - width.toPx() - paddingRight.toPx(), scrollbarY),
            size = Size(width.toPx(), thumbHeight),
            cornerRadius = CornerRadius(width.toPx() / 2, width.toPx() / 2)
        )
    }
}
