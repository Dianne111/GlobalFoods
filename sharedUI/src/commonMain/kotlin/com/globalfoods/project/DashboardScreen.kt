package com.globalfoods.project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DashboardNavy = Color(0xFF123B56)
private val DashboardBackground = Color(0xFFF1F6F9)
private val AccentGreen = Color(0xFF10B981)
private val MutedText = Color(0xFF718096)
private val CardShape = RoundedCornerShape(12.dp)
private val ProductImageBackground = Color(0xFFF4F7F8)
private val StockBackground = Color(0xFFE2F8EF)
private val NavigationItems = listOf(
    "INICIO" to Icons.Outlined.Business,
    "PEDIDOS" to Icons.Outlined.ReceiptLong,
    "CLIENTES" to Icons.Outlined.People,
    "REPORTES" to Icons.Outlined.BarChart
)

private data class Product(
    val name: String,
    val weight: String,
    val stock: String,
    val price: String
)

private data class RecentOrder(
    val number: String,
    val client: String,
    val total: String,
    val items: String,
    val status: String,
    val statusColor: Color,
    val elapsed: String,
    val accentColor: Color
)

private val Products = listOf(
    Product("Camarón Sin Cabeza", "21/25 kg", "45 kg", "$280/kg"),
    Product("Camarón Pelado", "41/50 kg", "15 kg", "$280/kg")
)

private val RecentOrders = listOf(
    RecentOrder("#12345", "Victoria Ontiveros", "$560", "2 Items", "PENDIENTE", Color(0xFFFFB020), "hace 5 min", Color(0xFFFFA000)),
    RecentOrder("#12346", "Restaurante Mar y Tierra", "$1,450", "5 Items", "EN PREPARACIÓN", Color(0xFF3B82F6), "hace 20 min", Color(0xFF3B82F6)),
    RecentOrder("#12347", "Mariscos El Faro", "$980", "3 Items", "EN REPARTO", AccentGreen, "hace 1 hora", AccentGreen)
)

@Composable
fun DashboardScreen() {
    Scaffold(
        containerColor = DashboardBackground,
        topBar = { DashboardHeader() },
        bottomBar = { DashboardNavigation() }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Button(
                    onClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                ) {
                    Text("+  Nuevo Pedido", fontWeight = FontWeight.Bold)
                }
            }
            item { SectionTitle("INVENTARIO DE CAMARÓN", "Ver todo") }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Products.forEach { product ->
                        ProductCard(product = product, modifier = Modifier.weight(1f))
                    }
                }
            }
            item {
                Text(
                    text = "PEDIDOS RECIENTES",
                    color = DashboardNavy,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            items(RecentOrders) { order ->
                OrderCard(order)
            }
            item {
                Text(
                    text = "GLOBAL FOODS MÉXICO © 2026",
                    color = DashboardNavy.copy(alpha = 0.65f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun DashboardHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DashboardNavy)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("PANEL DE CONTROL", color = AccentGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text("DASHBOARD DE VENTAS", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        IconButton(onClick = {}) {
            Icon(Icons.Outlined.Settings, contentDescription = "Configuración", tint = Color.White)
        }
    }
}

@Composable
private fun SectionTitle(title: String, action: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = DashboardNavy, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text(action, color = AccentGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ProductCard(product: Product, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .background(ProductImageBackground, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = Color(0xFFB7C4CA), modifier = Modifier.size(38.dp))
            }
            Spacer(modifier = Modifier.height(7.dp))
            Text(product.name, color = DashboardNavy, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(product.weight, color = DashboardNavy, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Stock: ${product.stock}", color = MutedText, fontSize = 10.sp)
            Text("Precio: ${product.price}", color = MutedText, fontSize = 10.sp)
            Text(
                "En Stock",
                color = AccentGreen,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .background(StockBackground, RoundedCornerShape(5.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
private fun OrderCard(order: RecentOrder) {
    Card(
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.width(4.dp).height(88.dp).background(order.accentColor))
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp).weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(order.number, color = DashboardNavy, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    StatusBadge(order.status, order.statusColor)
                }
                Spacer(modifier = Modifier.height(5.dp))
                Row {
                    Text("Cliente: ", color = MutedText, fontSize = 10.sp)
                    Text(order.client, color = DashboardNavy, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("Total: ${order.total}", color = DashboardNavy, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(7.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(order.items, color = MutedText, fontSize = 10.sp, modifier = Modifier.weight(1f))
                    Icon(Icons.Outlined.LocalShipping, contentDescription = null, tint = MutedText, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(order.elapsed, color = MutedText, fontSize = 10.sp)
                    Text("  ›", color = DashboardNavy, fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String, color: Color) {
    Text(
        text = status,
        color = color,
        fontSize = 8.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp)
    )
}

@Composable
private fun DashboardNavigation() {
    NavigationBar(containerColor = Color.White) {
        NavigationItems.forEachIndexed { index, (label, icon) ->
            NavigationBarItem(
                selected = index == 0,
                onClick = {},
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label, fontSize = 8.sp) }
            )
        }
    }
}
