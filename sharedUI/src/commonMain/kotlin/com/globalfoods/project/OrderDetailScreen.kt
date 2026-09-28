package com.globalfoods.project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DetailCardShape = RoundedCornerShape(12.dp)
private val ProductTableHeaderColor = Color(0xFFF8FAFC)
private val ProductTableHeaders = listOf("TALLA", "PRECIO", "CANTIDAD", "SUBTOTAL")

@Composable
fun OrderDetailScreen(orderId: String, onBack: () -> Unit) {
    val order = orders.firstOrNull { it.number == orderId }
    Scaffold(
        containerColor = Background,
        topBar = { DetailHeader(onBack) },
        bottomBar = { DashboardNavigation() }
    ) { insets ->
        if (order == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(insets),
                contentAlignment = Alignment.Center
            ) {
                Text("Pedido no encontrado", color = Navy, fontWeight = FontWeight.Bold)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(insets)
                    .padding(horizontal = DashboardHorizontalPadding),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                item {
                    Spacer(Modifier.height(16.dp))
                    Text("PEDIDO ${order.number}", color = Navy, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    DetailCard(order)
                    Spacer(Modifier.height(24.dp))
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

@Composable
private fun DetailHeader(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().height(DashboardHeaderHeight).background(Navy)
            .padding(start = 8.dp, end = 12.dp, top = 10.dp, bottom = 12.dp)
            .statusBarsPadding()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, "Regresar", tint = Color.White)
            }
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
private fun DetailCard(order: Order) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = DetailCardShape,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(order.accent))
            Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp).fillMaxWidth()) {
                DetailActions()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(order.number, color = Navy, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    StatusBadge(order.status, order.statusColor)
                }
                Spacer(Modifier.height(7.dp))
                Row {
                    Text("Cliente: ", color = Gray, fontSize = 10.sp)
                    Text(order.client, color = Navy, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(14.dp))
                ProductTable(order.products)
                Spacer(Modifier.height(20.dp))
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
                    Text("Total Kg:   ${order.totalKg}", color = Navy, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Total:      ${order.total}", color = Navy, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.LocalShipping, null, tint = Gray, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(order.elapsed, color = Gray, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun ProductTable(products: List<OrderProduct>) {
    Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(6.dp))) {
        Row(Modifier.fillMaxWidth().height(28.dp).background(ProductTableHeaderColor)) {
            ProductTableHeaders.forEach { TableHeader(it) }
        }
        products.forEach { product ->
            Row(Modifier.fillMaxWidth().height(32.dp)) {
                TableValue(product.size)
                TableValue(product.price)
                TableValue(product.quantity)
                TableValue(product.subtotal)
            }
        }
    }
}

@Composable
private fun DetailActions() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        DetailActionButton(
            icon = Icons.Outlined.Edit,
            description = "Editar pedido",
            tint = Color(0xFF64748B)
        )
        DetailActionButton(
            icon = Icons.Outlined.Delete,
            description = "Eliminar pedido",
            tint = Color(0xFFEF4444)
        )
    }
}

@Composable
private fun DetailActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    tint: Color
) {
    IconButton(onClick = {}, modifier = Modifier.size(28.dp)) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(18.dp))
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
            .background(color.copy(alpha = .12f), RoundedCornerShape(5.dp))
            .padding(horizontal = 7.dp, vertical = 4.dp)
    )
}

@Composable
private fun RowScope.TableHeader(value: String) {
    Text(value, Modifier.weight(1f), textAlign = TextAlign.Center, color = Color(0xFF475569), fontSize = 8.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun RowScope.TableValue(value: String) {
    Text(value, Modifier.weight(1f), textAlign = TextAlign.Center, color = Navy, fontSize = 11.sp)
}
