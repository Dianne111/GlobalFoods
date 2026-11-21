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
import com.globalfoods.project.sharedlogic.network.dashboard.DashboardRepository
import com.globalfoods.project.sharedlogic.network.dashboard.ArticuloInventario

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
fun DashboardScreen(onOrderSelected: (String) -> Unit = {}) {
    val listState = rememberLazyListState()

    val dashboardRepository = remember { DashboardRepository() }

    var inventarioList by remember { mutableStateOf<List<ArticuloInventario>>(emptyList()) }
    var isLoadingInventario by remember { mutableStateOf(true) }
    var errorMensaje by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val resultado = dashboardRepository.obtenerInventario()
        isLoadingInventario = false

        resultado.fold(
            onSuccess = { datos -> inventarioList = datos },
            onFailure = { error -> errorMensaje = error.message }
        )
    }

    Scaffold(
        containerColor = Background,
        topBar = { DashboardHeader() },
        bottomBar = { DashboardNavigation() }
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
                        NewOrderButton()
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
                        SectionLabel("PEDIDOS ACTUALES")
                        Spacer(Modifier.height(8.dp))
                    }

                    items(orders) { order ->
                        OrderCard(order, onClick = { onOrderSelected(order.number) })
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
private fun DashboardHeader() {
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
                onClick = {},
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
private fun NewOrderButton() {
    Box(
        modifier = Modifier.fillMaxWidth().height(40.dp).clip(CardShape).background(Green),
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

@Composable
internal fun DashboardNavigation() {
    val items = listOf(
        "INICIO" to Icons.Outlined.Home,
        "CLIENTES" to Icons.Outlined.People,
        "REPORTES" to Icons.Outlined.BarChart
    )
    NavigationBar(
        modifier = Modifier.navigationBarsPadding().height(66.dp),
        containerColor = Color.White,
        tonalElevation = 0.dp
    ) {
        items.forEachIndexed { index, (label, icon) ->
            NavigationBarItem(
                selected = index == 0,
                onClick = {},
                icon = { Icon(icon, label, modifier = Modifier.size(19.dp)) },
                label = { Text(label, fontSize = 8.sp) }
            )
        }
    }
}

@Composable
private fun OrderCard(order: Order, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(86.dp).clickable(onClick = onClick),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.width(4.dp).fillMaxSize().background(order.accent))
            Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp).fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(order.number, color = Navy, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(order.status, color = order.statusColor, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.background(order.statusColor.copy(alpha = .12f), RoundedCornerShape(5.dp)).padding(horizontal = 7.dp, vertical = 4.dp))
                }
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Cliente: ", color = Gray, fontSize = 10.sp)
                    Text(order.client, color = Navy, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
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
    val quantity: Int,
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
    val products: List<OrderProduct> = emptyList()
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
            OrderProduct("Camarón Talla 41/50", 280.0, 2, 560.0)
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
            OrderProduct("Pulpo Cocido", 450.0, 2, 900.0),
            OrderProduct("Filete de Pescado", 183.33, 3, 550.0)
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
            OrderProduct("Callo de Hacha", 980.0, 1, 980.0)
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