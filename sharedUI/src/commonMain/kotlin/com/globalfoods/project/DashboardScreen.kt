package com.globalfoods.project

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import globalfoods.sharedui.generated.resources.Res
import globalfoods.sharedui.generated.resources.global_foods_logo
import org.jetbrains.compose.resources.painterResource

internal val Navy = Color(0xFF143D58)
internal val Background = Color(0xFFF1F8FA)
internal val Green = Color(0xFF0DB982)
internal val Gray = Color(0xFF64748B)
private val CardShape = RoundedCornerShape(12.dp)
internal val DashboardHorizontalPadding = 16.dp
internal val DashboardHeaderHeight = 123.dp
private val InventoryHeaderColor = Color(0xFFF8FAFC)
private val InventoryRows = List(7) {
    InventoryRow(size = "41/50", price = "$280", master = "20", stock = "45")
}
private val InventoryHeaders = listOf("TALLA", "PRECIO", "MASTER", "STOCK")

internal data class Order(
    val number: String,
    val client: String,
    val total: String,
    val items: String,
    val status: String,
    val statusColor: Color,
    val elapsed: String,
    val accent: Color,
    val totalKg: String,
    val products: List<OrderProduct>
)

internal data class OrderProduct(
    val size: String,
    val price: String,
    val quantity: String,
    val subtotal: String
)

internal val orders = listOf(
    Order(
        number = "#12345",
        client = "Victoria Ontiveros",
        total = "$5,600",
        items = "2 Items",
        status = "PENDIENTE",
        statusColor = Color(0xFFFFA800),
        elapsed = "hace 5 min",
        accent = Color(0xFFFFA000),
        totalKg = "40 kg",
        products = listOf(OrderProduct("41/50", "$280", "20", "$5,600"))
    ),
    Order(
        number = "#12346",
        client = "Restaurante Mar y Tierra",
        total = "$1,000",
        items = "5 Items",
        status = "EN PREPARACIÓN",
        statusColor = Color(0xFF367CF4),
        elapsed = "hace 20 min",
        accent = Color(0xFF367CF4),
        totalKg = "100 kg",
        products = List(4) { OrderProduct("41/50", "$10", "20", "$200") }
    ),
    Order(
        number = "#12347",
        client = "Mariscos El Faro",
        total = "$800",
        items = "3 Items",
        status = "EN REPARTO",
        statusColor = Color(0xFF7026DF),
        elapsed = "hace 1 hora",
        accent = Color(0xFF7026DF),
        totalKg = "60 kg",
        products = List(3) { OrderProduct("41/50", "$10", "20", "$200") }
    )
)

private data class InventoryRow(
    val size: String,
    val price: String,
    val master: String,
    val stock: String
)

@Composable
fun DashboardScreen(onOrderSelected: (String) -> Unit = {}) {
    val listState = rememberLazyListState()
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
                        InventoryTable()
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
                DashboardScrollbar(listState, Modifier.align(Alignment.CenterEnd))
            }
        }
    }
}

@Composable
private fun DashboardScrollbar(state: LazyListState, modifier: Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                onDrawWithContent {
                    val layout = state.layoutInfo
                    val totalItems = layout.totalItemsCount
                    val visibleItems = layout.visibleItemsInfo.size
                    if (totalItems > 0 && layout.viewportSize.height > 0) {
                        val trackHeight = size.height
                        val thumbHeight = if (totalItems > visibleItems) {
                            (trackHeight * visibleItems.toFloat() / totalItems.toFloat())
                                .coerceAtLeast(32.dp.toPx())
                        } else {
                            trackHeight
                        }
                        val maxIndex = (totalItems - visibleItems).coerceAtLeast(1)
                        val progress = if (totalItems > visibleItems) {
                            (state.firstVisibleItemIndex.toFloat() / maxIndex).coerceIn(0f, 1f)
                        } else {
                            0f
                        }
                        val top = (trackHeight - thumbHeight) * progress
                        drawRoundRect(
                            color = Navy.copy(alpha = .10f),
                            topLeft = androidx.compose.ui.geometry.Offset(size.width - 5.dp.toPx(), 0f),
                            size = androidx.compose.ui.geometry.Size(3.dp.toPx(), trackHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                        )
                        drawRoundRect(
                            color = Navy.copy(alpha = .55f),
                            topLeft = androidx.compose.ui.geometry.Offset(size.width - 4.dp.toPx(), top),
                            size = androidx.compose.ui.geometry.Size(3.dp.toPx(), thumbHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                        )
                    }
                }
            }
    )
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
                modifier = Modifier.size(width = 31.dp, height = 27.dp)
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
private fun InventoryTable() {
    Column(
        modifier = Modifier.fillMaxWidth().clip(CardShape).background(Color.White)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(28.dp).background(InventoryHeaderColor),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InventoryHeaders.forEach { TableCell(it, isHeader = true) }
        }
        InventoryRows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().height(32.dp).background(Color.White),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(row.size, row.price, row.master, row.stock)
                    .forEach { TableCell(it, isHeader = false) }
            }
        }
    }
}

@Composable
private fun RowScope.TableCell(value: String, isHeader: Boolean) {
    Text(
        value,
        modifier = Modifier.weight(1f),
        textAlign = TextAlign.Center,
        color = if (isHeader) Color(0xFF475569) else Color(0xFF374151),
        fontSize = if (isHeader) 9.sp else 12.sp,
        fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Medium
    )
}

@Composable
private fun OrderCard(order: Order, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(86.dp)
            .clickable(onClick = onClick),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.width(4.dp).fillMaxSize().background(order.accent))
            Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp).fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(order.number, color = Navy, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(
                        order.status,
                        color = order.statusColor,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.background(order.statusColor.copy(alpha = .12f), RoundedCornerShape(5.dp))
                            .padding(horizontal = 7.dp, vertical = 4.dp)
                    )
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

@Composable
internal fun DashboardNavigation() {
    val items = listOf(
        "INICIO" to Icons.Outlined.Home,
        "CLIENTES" to Icons.Outlined.People,
        "REPORTES" to Icons.Outlined.BarChart
    )
    NavigationBar(
        modifier = Modifier
            .navigationBarsPadding()
            .height(66.dp),
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
