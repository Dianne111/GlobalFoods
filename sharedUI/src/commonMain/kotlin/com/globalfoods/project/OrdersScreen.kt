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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.globalfoods.project.sharedlogic.network.orders.OrderRepository
import com.globalfoods.project.sharedlogic.network.orders.OrderStatus

/** Ordena del pedido más reciente al más antiguo (por fecha de entrega y luego por número). */
internal fun sortOrders(orders: List<Order>): List<Order> =
    orders.sortedWith(compareByDescending<Order> { it.response?.date ?: 0 }.thenByDescending { it.id })

/** Agrupaciones de la pestaña Pedidos: lo que sigue en proceso, lo entregado y lo cancelado. */
internal enum class OrdersView(val label: String) {
    Active("Activos"),
    Delivered("Entregados"),
    Cancelled("Cancelados");

    fun includes(order: Order): Boolean = when (OrderStatus.fromCode(order.response?.status ?: 0)) {
        OrderStatus.Delivered -> this == Delivered
        OrderStatus.Cancelled -> this == Cancelled
        // Creado, Surtido, En reparto y cualquier código desconocido siguen a la vista como activos.
        else -> this == Active
    }
}

/** Busca por nombre del cliente o por número de pedido ("4" o "#4" encuentran el pedido #4). */
internal fun searchOrders(orders: List<Order>, query: String): List<Order> {
    val text = query.trim()
    if (text.isEmpty()) return orders
    val number = text.removePrefix("#").toIntOrNull()
    return orders.filter { order ->
        order.client.contains(text, ignoreCase = true) || (number != null && order.id == number)
    }
}

@Composable
fun OrdersScreen(
    refreshKey: Int = 0,
    onOrderSelected: (Order) -> Unit,
    onTabSelected: (MainTab) -> Unit
) {
    val orderRepository = remember { OrderRepository() }
    var orders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }
    // Se conservan al ir al detalle y regresar.
    var view by rememberSaveable { mutableStateOf(OrdersView.Active) }
    var query by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(refreshKey, reloadKey) {
        isLoading = true
        error = null
        loadOrders(
            orderRepository,
            onOrders = { orders = sortOrders(it) },
            onError = { error = it }
        )
        isLoading = false
    }

    val searched = remember(orders, query) { searchOrders(orders, query) }
    val visible = remember(searched, view) { searched.filter(view::includes) }

    Scaffold(
        containerColor = Background,
        topBar = {
            ScreenHeader(eyebrow = "PANEL DE CONTROL", title = "PEDIDOS") {
                // Los estados los actualiza almacén/reparto en la API; aquí solo se vuelven a consultar.
                HeaderIconAction(Icons.Outlined.Refresh, "Actualizar pedidos", enabled = !isLoading) { reloadKey++ }
            }
        },
        bottomBar = { DashboardNavigation(MainTab.Pedidos, onTabSelected) }
    ) { insets ->
        Box(Modifier.fillMaxSize().padding(insets)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 520.dp)
                    .align(Alignment.TopCenter)
                    .padding(horizontal = DashboardHorizontalPadding),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Spacer(Modifier.height(6.dp))
                    SearchField(query = query, onQueryChange = { query = it })
                }
                item {
                    ViewTabs(orders = searched, selected = view, onSelected = { view = it })
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
                        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Green)
                        }
                    }
                    visible.isEmpty() && error == null -> item {
                        Text(
                            when {
                                orders.isEmpty() -> "Todavía no hay pedidos registrados."
                                query.isNotBlank() -> "No hay pedidos que coincidan con la búsqueda."
                                else -> "No hay pedidos ${view.label.lowercase()}."
                            },
                            color = Gray,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp)
                        )
                    }
                    else -> items(visible, key = { it.id }) { order ->
                        OrderCard(order, onClick = { onOrderSelected(order) })
                    }
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp)
    ) {
        Icon(Icons.Outlined.Search, contentDescription = null, tint = Gray, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text("Buscar pedido", color = Gray, fontSize = 14.sp)
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(color = Navy, fontSize = 14.sp),
                cursorBrush = SolidColor(Navy),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (query.isNotEmpty()) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = "Limpiar búsqueda",
                tint = Gray,
                modifier = Modifier.size(20.dp).clickable { onQueryChange("") }
            )
        }
    }
}

@Composable
private fun ViewTabs(
    orders: List<Order>,
    selected: OrdersView,
    onSelected: (OrdersView) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .padding(4.dp)
    ) {
        OrdersView.entries.forEach { view ->
            val isSelected = view == selected
            val count = orders.count(view::includes)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .background(if (isSelected) Navy else Color.Transparent, RoundedCornerShape(9.dp))
                    .clickable { onSelected(view) }
            ) {
                Text(
                    "${view.label} · $count",
                    color = if (isSelected) Color.White else Navy,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}
