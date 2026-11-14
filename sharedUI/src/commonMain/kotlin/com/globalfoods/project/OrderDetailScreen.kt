package com.globalfoods.project

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.globalfoods.project.sharedlogic.network.orders.Branch
import com.globalfoods.project.sharedlogic.network.orders.Client
import com.globalfoods.project.sharedlogic.network.orders.OrderObservations
import com.globalfoods.project.sharedlogic.network.orders.OrderRepository
import com.globalfoods.project.sharedlogic.network.orders.OrderResponse
import com.globalfoods.project.sharedlogic.network.orders.OrderStatus
import com.globalfoods.project.sharedlogic.network.orders.orderStatus
import com.globalfoods.project.sharedlogic.network.core.sessionRol
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    order: Order,
    onNavigateBack: () -> Unit,
    /** El pedido se canceló (Estatus 5); se queda en el detalle y se refrescan las listas. */
    onCancelled: () -> Unit,
    onEdit: (OrderResponse) -> Unit = {}
) {
    val repository = remember { OrderRepository() }
    val openAddress = rememberAddressLauncher()
    var mapError by remember(order.id) { mutableStateOf<String?>(null) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var isCancelling by remember { mutableStateOf(false) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    var response by remember(order.id) { mutableStateOf(order.response) }
    var linkedClient by remember(order.id) { mutableStateOf<Client?>(null) }
    var linkedBranch by remember(order.id) { mutableStateOf<Branch?>(null) }
    var isLoading by remember(order.id) { mutableStateOf(false) }
    var loadError by remember(order.id) { mutableStateOf<String?>(null) }
    var reloadKey by remember(order.id) { mutableStateOf(0) }

    LaunchedEffect(order.id, reloadKey) {
        if (order.id <= 0) return@LaunchedEffect
        isLoading = true
        loadError = null
        val detailResult = repository.getOrder(order.id)
        val detail = detailResult.getOrNull() ?: response
        if (detailResult.isFailure) {
            loadError = "No se pudo actualizar la información del pedido."
        }
        if (detail == null) {
            isLoading = false
            return@LaunchedEffect
        }
        response = detail
        val delivery = OrderObservations.decode(detail.notes).delivery
        val clientId = detail.clientId ?: detail.client?.id
        val ownClient = ownClientForSession()
        val embeddedClient = detail.client?.takeIf { it.id == clientId && it.name.isNotBlank() }
            ?: order.response?.client?.takeIf { it.id == clientId && it.name.isNotBlank() }
            ?: ownClient?.takeIf { it.id == clientId }
        linkedClient = embeddedClient
        linkedBranch = null
        // Un cliente no consulta el catálogo de clientes; usa los datos de su sesión.
        if (ownClient == null && clientId != null && delivery == null &&
            (embeddedClient?.name.isNullOrBlank() || embeddedClient.phone.isBlank())
        ) {
            val clientsResult = repository.getClients()
            val client = clientsResult.getOrNull()?.firstOrNull { it.id == clientId }
            linkedClient = client ?: embeddedClient
            if (clientsResult.isFailure) {
                loadError = "No se pudieron cargar los datos del cliente."
            }
        }
        val branchId = detail.branchId
        if (branchId != null && delivery?.address.isNullOrBlank()) {
            val branch = linkedClient?.branches?.firstOrNull { it.id == branchId && it.clientId == clientId }
            linkedBranch = branch
            if (clientId != null && branch?.address.isNullOrBlank()) {
                val branchesResult = repository.getBranches(clientId)
                linkedBranch = branchesResult.getOrNull()?.firstOrNull {
                    it.id == branchId && it.clientId == clientId
                } ?: branch
                if (branchesResult.isFailure) {
                    loadError = "No se pudieron cargar los datos de la sucursal."
                }
            }
        }
        isLoading = false
    }

    val summary = remember(order, response) { response?.let { toOrder(it) } ?: order }
    val observations = remember(response?.notes) { OrderObservations.decode(response?.notes) }
    val delivery = observations.delivery
    val clientName = delivery?.clientName?.takeIf { it.isNotBlank() }
        ?: linkedClient?.name?.takeIf { it.isNotBlank() }
        ?: summary.client
    val phone = delivery?.phone ?: linkedClient?.phone
    val branchName = delivery?.branchName?.takeIf { it.isNotBlank() } ?: linkedBranch?.name
    val address = delivery?.address?.takeIf { it.isNotBlank() } ?: linkedBranch?.address
    val isPickup = delivery?.method == "Recolección"
    val deliveryMethod = when {
        isPickup -> "Recolección"
        response?.branchId != null -> "Entrega a sucursal"
        delivery?.method == "Dirección" -> "Envío a domicilio"
        else -> null
    }
    Scaffold(
        containerColor = Color(0xFFF1F8FA),
        topBar = {
            ScreenHeader(
                eyebrow = "DETALLE DEL PEDIDO",
                title = "Pedido ${order.number}",
                onBack = onNavigateBack
            ) {
                // El estado lo cambia almacén/reparto en la API; aquí solo se vuelve a consultar.
                HeaderIconAction(Icons.Outlined.Refresh, "Actualizar estado", enabled = !isLoading) { reloadKey++ }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isLoading) {
                item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Green) }
            }
            loadError?.let { message ->
                item {
                    Column {
                        Text(message, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                        TextButton(onClick = { reloadKey++ }, enabled = !isLoading) {
                            Text("Reintentar")
                        }
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Cliente:", color = Color(0xFF64748B), fontSize = 14.sp)
                            Text(
                                text = summary.status,
                                color = summary.statusColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(summary.statusColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Text(text = clientName, color = Navy, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(14.dp))
                        OrderStatusProgress(response?.orderStatus)
                        Spacer(Modifier.height(14.dp))
                        delivery?.clientType?.let { Text(it, color = Gray, fontSize = 13.sp) }
                        Spacer(Modifier.height(8.dp))
                        OrderDetailField("Celular", phone)
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color(0xFFF1F8FA))
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "Peso Total:", color = Color(0xFF64748B), fontSize = 12.sp)
                                Text(text = summary.totalKg, color = Color(0xFF143D58), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "Monto Total:", color = Color(0xFF64748B), fontSize = 12.sp)
                                Text(text = summary.total, color = Color(0xFF0DB982), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                if (showCancelDialog) {
                    AlertDialog(
                        onDismissRequest = { if (!isCancelling) showCancelDialog = false },
                        title = { Text("Cancelar pedido") },
                        text = {
                            Text(
                                "El pedido ${order.number} pasará a Cancelado y ya no se surtirá. " +
                                    "Se conserva en el historial y no se puede reactivar desde la app."
                            )
                        },
                        confirmButton = {
                            TextButton(
                                enabled = !isCancelling && response != null,
                                onClick = {
                                    val current = response ?: return@TextButton
                                    isCancelling = true
                                    scope.launch {
                                        repository.cancelOrder(current).fold(
                                            onSuccess = { updated ->
                                                response = updated ?: current.copy(status = OrderStatus.Cancelled.code)
                                                showCancelDialog = false
                                                reloadKey++
                                                onCancelled()
                                            },
                                            onFailure = { showCancelDialog = false; error = it.message }
                                        )
                                        isCancelling = false
                                    }
                                }
                            ) { Text("Cancelar pedido", color = Color(0xFFB91C1C)) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showCancelDialog = false }, enabled = !isCancelling) { Text("Volver") }
                        }
                    )
                }
                error?.let {
                    AlertDialog(onDismissRequest = { error = null }, title = { Text("No se pudo cancelar") }, text = { Text(it) }, confirmButton = {
                        TextButton(onClick = { error = null }) { Text("Aceptar") }
                    })
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("ENTREGA Y OBSERVACIONES", color = Navy, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        OrderDetailField("Fecha de entrega", formatOrderDate(response?.date))
                        OrderDetailField("Método de entrega", deliveryMethod)
                        if (isPickup) {
                            OrderDetailField("Punto de recolección", "Congeladora Global Foods México")
                        } else {
                            if (response?.branchId != null || !branchName.isNullOrBlank()) {
                                OrderDetailField("Sucursal", branchName)
                            }
                            OrderAddressField(address) { destination ->
                                mapError = null
                                if (!openAddress(destination)) {
                                    mapError = "No se pudo abrir la dirección. Revisa las aplicaciones de mapas o el navegador."
                                }
                            }
                            mapError?.let { message ->
                                Text(message, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                            }
                        }
                        delivery?.references?.takeIf { it.isNotBlank() }?.let {
                            OrderDetailField("Referencias", it)
                        }
                        OrderDetailField("Observaciones", observations.notes, "Sin observaciones")
                        OrderDetailField("Costo de envío", delivery?.shippingCost?.let { "$$it" })
                    }
                }
            }

            item {
                Text(
                    text = "PRODUCTOS (${summary.products.size})",
                    color = Color(0xFF143D58),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(summary.products) { product ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = product.name, color = Color(0xFF143D58), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row {
                                Text(
                                    text = "Cant: ${product.quantity.toString().trimEnd('0').trimEnd('.')}",
                                    color = Color(0xFF64748B),
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(text = "Precio: $${product.price}", color = Color(0xFF64748B), fontSize = 12.sp)
                            }
                        }

                        Text(
                            text = "$${product.subtotal}",
                            color = Color(0xFF143D58),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // El cliente solo consulta: no ve Editar ni Cancelar.
            if (sessionRol.canEditOrders || sessionRol.canCancelOrders) item {
                val editable = response?.takeIf { it.orderStatus?.isEditable == true }
                if (sessionRol.canEditOrders) {
                    if (editable != null) {
                        Button(
                            onClick = { onEdit(editable) },
                            enabled = !isLoading && !isCancelling,
                            colors = ButtonDefaults.buttonColors(containerColor = Navy),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) { Text("Editar pedido", fontWeight = FontWeight.Bold) }
                    } else if (response != null) {
                        Text(
                            "Solo se pueden editar pedidos en estado CREADO.",
                            color = Gray,
                            fontSize = 12.sp
                        )
                    }
                }
                if (sessionRol.canCancelOrders && response?.orderStatus?.isCancellable == true) {
                    TextButton(
                        onClick = { showCancelDialog = true },
                        enabled = !isLoading && !isCancelling,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFB91C1C))
                    ) { Text("Cancelar pedido") }
                }
            }
        }
    }
}

@Composable
private fun OrderAddressField(address: String?, onOpenAddress: (String) -> Unit) {
    val destination = address?.trim().orEmpty()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(
                enabled = destination.isNotEmpty(),
                onClickLabel = "Abrir dirección en mapas",
                role = Role.Button,
                onClick = { onOpenAddress(destination) }
            )
            .padding(vertical = 4.dp)
    ) {
        Text("Dirección de entrega", color = Gray, fontSize = 12.sp)
        if (destination.isEmpty()) {
            Text("Sin dato registrado", color = Navy, fontSize = 14.sp)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = Color(0xFF168BC1))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(destination, color = Color(0xFF168BC1), fontSize = 14.sp)
                    Text("Abrir en mapas", color = Gray, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun OrderDetailField(label: String, value: String?, fallback: String = "Sin dato registrado") {
    Column {
        Text(label, color = Gray, fontSize = 12.sp)
        Text(value?.takeIf { it.isNotBlank() } ?: fallback, color = Navy, fontSize = 14.sp)
    }
}

