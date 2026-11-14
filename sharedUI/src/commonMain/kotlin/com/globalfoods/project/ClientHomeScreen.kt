package com.globalfoods.project

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.globalfoods.project.sharedlogic.network.core.sessionIdCliente
import com.globalfoods.project.sharedlogic.network.core.sessionNombreUsuario
import com.globalfoods.project.sharedlogic.network.orders.OrderRepository
import com.globalfoods.project.sharedlogic.network.orders.OrderResponse
import com.globalfoods.project.sharedlogic.network.orders.OrderStatus

/** Números del mes que se muestran en el Inicio del cliente. */
internal data class ClientMonthSummary(val active: Int, val deliveredThisMonth: Int, val kgThisMonth: Double)

/**
 * [yearMonth] con formato yyyyMM. Los kilos del mes cuentan los pedidos no cancelados
 * cuya fecha de entrega cae en ese mes.
 */
internal fun clientMonthSummary(orders: List<Order>, yearMonth: Int): ClientMonthSummary {
    fun Order.status() = OrderStatus.fromCode(response?.status ?: 0)
    fun Order.inMonth() = (response?.date ?: 0) / 100 == yearMonth
    return ClientMonthSummary(
        active = orders.count { OrdersView.Active.includes(it) },
        deliveredThisMonth = orders.count { it.status() == OrderStatus.Delivered && it.inMonth() },
        kgThisMonth = orders
            .filter { it.status() != OrderStatus.Cancelled && it.inMonth() }
            .sumOf { it.response?.totalKg ?: 0.0 }
    )
}

private fun currentYearMonth(): Int {
    val days = kotlin.time.Clock.System.now().toEpochMilliseconds() / 86_400_000L
    // Algoritmo civil (Howard Hinnant) para obtener año y mes sin depender de kotlinx-datetime.
    val z = days + 719468
    val era = (if (z >= 0) z else z - 146096) / 146097
    val doe = z - era * 146097
    val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val month = (mp + if (mp < 10) 3 else -9).toInt()
    val year = (yoe + era * 400).toInt() + if (month <= 2) 1 else 0
    return year * 100 + month
}

@Composable
fun ClientHomeScreen(
    refreshKey: Int = 0,
    onNewOrder: () -> Unit,
    onRepeatOrder: (OrderResponse) -> Unit,
    onOrderSelected: (Order) -> Unit,
    onTabSelected: (MainTab) -> Unit,
    onLogout: () -> Unit
) {
    val orderRepository = remember { OrderRepository() }
    var orders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    val linked = sessionIdCliente != null

    LaunchedEffect(refreshKey, reloadKey) {
        isLoading = true
        error = null
        loadOrders(orderRepository, onOrders = { orders = sortOrders(it) }, onError = { error = it })
        isLoading = false
    }

    val active = orders.filter(OrdersView.Active::includes)
    val current = active.firstOrNull()
    val lastOrder = orders.firstOrNull { OrderStatus.fromCode(it.response?.status ?: 0) != OrderStatus.Cancelled }
    val summary = remember(orders) { clientMonthSummary(orders, currentYearMonth()) }

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

    Scaffold(
        containerColor = Background,
        topBar = {
            ScreenHeader(eyebrow = "BIENVENIDO", title = sessionNombreUsuario?.takeIf { it.isNotBlank() } ?: "Mi cuenta") {
                HeaderIconAction(Icons.Outlined.Settings, "Configuración") { showSettings = true }
            }
        },
        bottomBar = { DashboardNavigation(MainTab.Inicio, onTabSelected) }
    ) { insets ->
        Box(Modifier.fillMaxSize().padding(insets)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 520.dp)
                    .align(Alignment.TopCenter)
                    .padding(horizontal = DashboardHorizontalPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(Modifier.height(4.dp)) }
                if (!linked) {
                    item {
                        Notice(
                            "Tu usuario aún no está vinculado a un cliente. " +
                                "Comunícate con tu asesor de ventas para poder hacer y consultar pedidos."
                        )
                    }
                }
                item {
                    PrimaryAction("+  Nuevo pedido", enabled = linked, onClick = onNewOrder)
                    Spacer(Modifier.height(8.dp))
                    SecondaryAction(
                        "Repetir último pedido",
                        enabled = linked && lastOrder?.response != null,
                        onClick = { lastOrder?.response?.let(onRepeatOrder) }
                    )
                }
                error?.let { message ->
                    item {
                        Column {
                            Text(message, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                            TextButton(onClick = { reloadKey++ }) { Text("Reintentar") }
                        }
                    }
                }
                when {
                    isLoading && orders.isEmpty() -> item {
                        Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Green)
                        }
                    }
                    current != null -> {
                        item { Label("PEDIDO EN CURSO") }
                        item { CurrentOrderCard(current, onClick = { onOrderSelected(current) }) }
                        val others = active.drop(1)
                        if (others.isNotEmpty()) {
                            item {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Label("OTROS PEDIDOS ACTIVOS (${others.size})", Modifier.weight(1f))
                                    TextButton(onClick = { onTabSelected(MainTab.Pedidos) }) {
                                        Text("Ver todos", fontSize = 12.sp)
                                    }
                                }
                            }
                            items(others.take(2), key = { it.id }) { order ->
                                OrderCard(order, onClick = { onOrderSelected(order) })
                            }
                        }
                    }
                    linked && error == null -> item {
                        Text(
                            if (orders.isEmpty()) "Aún no tienes pedidos. ¡Haz el primero!"
                            else "No tienes pedidos en curso.",
                            color = Gray,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                        )
                    }
                }
                if (orders.isNotEmpty()) {
                    item { Label("ESTE MES") }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatTile("Activos", summary.active.toString(), Modifier.weight(1f))
                            StatTile("Entregados", summary.deliveredThisMonth.toString(), Modifier.weight(1f))
                            StatTile("Kg pedidos", formatKg(summary.kgThisMonth), Modifier.weight(1f))
                        }
                    }
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }
}

private fun formatKg(value: Double): String =
    value.toString().let { if (it.contains('.')) it.trimEnd('0').trimEnd('.') else it }

@Composable
private fun CurrentOrderCard(order: Order, onClick: () -> Unit) {
    val status = OrderStatus.fromCode(order.response?.status ?: 0)
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Pedido ${order.number}", color = Navy, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(
                    order.status,
                    color = order.statusColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(order.statusColor.copy(alpha = .12f), RoundedCornerShape(5.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(order.elapsed, color = Gray, fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))
            OrderStatusProgress(status)
            Spacer(Modifier.height(16.dp))
            Row {
                Text(order.totalKg, color = Gray, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text("Total ${order.total}  ›", color = Navy, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier) {
    Column(
        modifier
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = Navy, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Gray, fontSize = 11.sp)
    }
}

@Composable
private fun Label(text: String, modifier: Modifier = Modifier) {
    Text(text, color = Navy, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = modifier)
}

@Composable
internal fun Notice(text: String) {
    Text(
        text,
        color = Color(0xFF92400E),
        fontSize = 13.sp,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFEF3C7), RoundedCornerShape(10.dp))
            .padding(12.dp)
    )
}

@Composable
internal fun PrimaryAction(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(if (enabled) Green else Green.copy(alpha = .4f), RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SecondaryAction(text: String, enabled: Boolean, onClick: () -> Unit) {
    val color = if (enabled) Navy else Gray.copy(alpha = .6f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, color.copy(alpha = .4f), RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}
