package com.globalfoods.project

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.globalfoods.project.sharedlogic.network.auth.UserRole
import com.globalfoods.project.sharedlogic.network.core.sessionIdCliente
import com.globalfoods.project.sharedlogic.network.core.sessionNombreUsuario
import com.globalfoods.project.sharedlogic.network.core.sessionRol
import com.globalfoods.project.sharedlogic.network.dashboard.ArticuloInventario
import com.globalfoods.project.sharedlogic.network.dashboard.DashboardRepository
import com.globalfoods.project.sharedlogic.network.orders.Client
import com.globalfoods.project.sharedlogic.network.orders.Branch
import com.globalfoods.project.sharedlogic.network.orders.OrderDeliveryInfo
import com.globalfoods.project.sharedlogic.network.orders.OrderLineRequest
import com.globalfoods.project.sharedlogic.network.orders.OrderObservations
import com.globalfoods.project.sharedlogic.network.orders.OrderRepository
import com.globalfoods.project.sharedlogic.network.orders.OrderRequest
import com.globalfoods.project.sharedlogic.network.orders.OrderResponse

private enum class NewOrderStep { Details, Catalog, Summary }

private val freezerAddress = "Congeladora Global Foods México, Av. Principal 123, Guadalajara, Jalisco"
private val customerBranches = listOf(
    "Sucursal Centro" to "Av. Juárez 120, Centro, Guadalajara, Jalisco",
    "Sucursal Norte" to "Av. Federalismo 850, Col. Lomas, Guadalajara, Jalisco"
)

internal data class NewOrderFormState(
    val quantities: Map<String, String> = emptyMap(),
    val clientId: Int? = null,
    val branchId: Int? = null,
    val client: String = "",
    val clientType: String = "",
    val phone: String = "",
    val branch: String = "",
    val address: String = "",
    val street: String = "",
    val neighborhood: String = "",
    val postalCode: String = "",
    val deliveryDate: String = millisToDate(currentDateMillis()),
    val notes: String = "",
    val shipping: String = "",
    val deliveryMode: String = "Dirección",
    val payment: String = "Transferencia",
    val references: String = ""
) {
    val shippingCost: Double
        get() = shipping.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() } ?: 0.0

    val hasCapturedData: Boolean
        get() = quantities.values.any { it.toDoubleOrNull()?.let { value -> value > 0 } == true } ||
            client.isNotBlank() || phone.isNotBlank() || branch.isNotBlank() ||
            address.isNotBlank() || street.isNotBlank() || neighborhood.isNotBlank() ||
            postalCode.isNotBlank() || deliveryDate.isNotBlank() ||
            notes.isNotBlank() || shipping.isNotBlank()
}

internal const val OUT_OF_STOCK_MESSAGE =
    "No se cuenta con disponibilidad de la cantidad solicitada. Favor de comunicarse con su asesor de ventas."

/** true si algún producto pide más kilos de los disponibles en inventario. */
internal fun exceedsStock(quantities: Map<String, String>, products: List<ArticuloInventario>): Boolean =
    products.any { (quantities[it.talla]?.toDoubleOrNull() ?: 0.0) > it.cantidadKg }

/**
 * Formulario inicial de un pedido nuevo. Para un cliente queda fijo su propio cliente.
 * Con [template] ("Repetir último pedido") se copian productos y entrega, pero la fecha vuelve a hoy.
 */
internal fun initialNewOrderForm(
    isClient: Boolean,
    clientId: Int?,
    userName: String?,
    template: OrderResponse?
): NewOrderFormState {
    val base = template?.let(::formFromOrder)?.copy(deliveryDate = NewOrderFormState().deliveryDate)
        ?: NewOrderFormState()
    if (!isClient) return base
    return base.copy(
        clientType = "Cliente registrado",
        clientId = clientId,
        client = base.client.ifBlank { userName.orEmpty() }
    )
}

/** Reconstruye el formulario a partir de un pedido ya registrado para poder editarlo. */
internal fun formFromOrder(order: OrderResponse): NewOrderFormState {
    val observations = OrderObservations.decode(order.notes)
    val delivery = observations.delivery
    val clientType = delivery?.clientType?.takeIf { it.isNotBlank() }
        ?: if (order.clientId != null) "Cliente registrado" else "Cliente esporádico"
    val isPickup = delivery?.method == "Recolección"
    val address = if (isPickup) freezerAddress else delivery?.address.orEmpty()
    val (street, neighborhood, postalCode) =
        if (!isPickup && clientType == "Cliente esporádico") splitAddress(address) else Triple("", "", "")
    return NewOrderFormState(
        quantities = order.lines
            .filter { it.quantity > 0 }
            .associate { it.size to formatQuantity(it.quantity) },
        clientId = order.clientId ?: order.client?.id,
        branchId = order.branchId.takeUnless { isPickup },
        client = delivery?.clientName?.takeIf { it.isNotBlank() } ?: order.client?.name.orEmpty(),
        clientType = clientType,
        phone = delivery?.phone?.takeIf { it.isNotBlank() } ?: order.client?.phone.orEmpty(),
        branch = if (isPickup) "Congeladora Global Foods" else delivery?.branchName.orEmpty(),
        address = address,
        street = street,
        neighborhood = neighborhood,
        postalCode = postalCode,
        deliveryDate = orderDateToText(order.date) ?: millisToDate(currentDateMillis()),
        notes = observations.notes,
        shipping = delivery?.shippingCost?.takeIf { it > 0 }?.let(::formatQuantity).orEmpty(),
        deliveryMode = if (isPickup) "Recolección" else "Dirección",
        references = delivery?.references.orEmpty()
    )
}

/**
 * Inventario usado al editar: conserva el precio pactado de los productos que ya traía el pedido,
 * permite mantener al menos la cantidad ya pedida y agrega las tallas que ya no aparecen en inventario.
 */
internal fun editableInventory(inventory: List<ArticuloInventario>, order: OrderResponse?): List<ArticuloInventario> {
    if (order == null) return inventory
    val lines = order.lines.filter { it.quantity > 0 }.associateBy { it.size }
    val merged = inventory.map { product ->
        lines[product.talla]?.let { line ->
            product.copy(
                codigoProducto = product.codigoProducto.takeIf { it > 0 } ?: line.productCode,
                precio = line.price,
                cantidadKg = maxOf(product.cantidadKg, line.quantity)
            )
        } ?: product
    }
    val missing = lines.values
        .filter { line -> inventory.none { it.talla == line.size } }
        .map { line -> ArticuloInventario(line.productCode, line.size, line.price, 0.0, line.quantity) }
    return merged + missing
}

private fun splitAddress(address: String): Triple<String, String, String> {
    val parts = address.split(", ").map(String::trim).filter(String::isNotBlank).toMutableList()
    val postalCode = parts.lastOrNull()
        ?.takeIf { it.startsWith("C.P. ") }
        ?.also { parts.removeAt(parts.lastIndex) }
        ?.removePrefix("C.P. ")
        ?.filter(Char::isDigit)
        ?.take(5)
        .orEmpty()
    val neighborhood = if (parts.size >= 2) parts.removeAt(parts.lastIndex) else ""
    return Triple(parts.joinToString(", "), neighborhood, postalCode)
}

private fun orderDateToText(date: Int): String? {
    val digits = date.toString().takeIf { it.length == 8 } ?: return null
    return "${digits.substring(6, 8)}/${digits.substring(4, 6)}/${digits.substring(0, 4)}"
}

private fun formatQuantity(value: Double): String =
    value.toString().let { if (it.contains('.')) it.trimEnd('0').trimEnd('.') else it }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewOrderScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onOrderCreated: () -> Unit = {},
    orderToEdit: OrderResponse? = null,
    onOrderUpdated: (OrderResponse?) -> Unit = {},
    /** Pedido anterior para "Repetir último pedido": precarga productos y entrega, pero se registra como nuevo. */
    template: OrderResponse? = null
) {
    val isEditing = orderToEdit != null
    // El cliente pide para sí mismo: no elige cliente ni ve existencias.
    val isClient = sessionRol == UserRole.Client
    val initialForm = remember(orderToEdit, template) {
        orderToEdit?.let(::formFromOrder)
            ?: initialNewOrderForm(isClient, sessionIdCliente, sessionNombreUsuario, template)
    }
    val repository = remember { DashboardRepository() }
    val orderRepository = remember { OrderRepository() }
    var rawInventory by remember { mutableStateOf<List<ArticuloInventario>>(emptyList()) }
    val inventory = remember(rawInventory, orderToEdit) { editableInventory(rawInventory, orderToEdit) }
    var clients by remember { mutableStateOf<List<Client>>(emptyList()) }
    var selectedClientBranches by remember { mutableStateOf<List<Branch>>(emptyList()) }
    var loadingBranches by remember { mutableStateOf(false) }
    var branchesClientId by remember { mutableStateOf<Int?>(null) }
    var branchesRequestId by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var step by remember { mutableStateOf(NewOrderStep.Details) }
    var form by remember { mutableStateOf(initialForm) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        repository.obtenerInventario().fold(
            onSuccess = { rawInventory = it; loading = false },
            onFailure = { error = it.message; loading = false }
        )
    }

    LaunchedEffect(Unit) {
        // El catálogo de clientes solo lo usan asesoras y administradores.
        if (isClient) return@LaunchedEffect
        orderRepository.getClients().fold(
            onSuccess = { loaded ->
                clients = loaded
                val current = form
                if (current.clientType == "Cliente registrado" && current.clientId != null) {
                    loaded.firstOrNull { it.id == current.clientId }?.let { client ->
                        form = current.copy(
                            client = current.client.ifBlank { client.name },
                            phone = current.phone.ifBlank { client.phone }
                        )
                    }
                }
            },
            onFailure = { saveError = it.message }
        )
    }

    fun loadBranches(clientId: Int?) {
        val requestId = branchesRequestId + 1
        branchesRequestId = requestId
        selectedClientBranches = emptyList()
        branchesClientId = clientId
        loadingBranches = clientId != null
        if (clientId == null) return
        scope.launch {
            orderRepository.getBranches(clientId).fold(
                onSuccess = { branches ->
                    if (branchesRequestId == requestId && branchesClientId == clientId) {
                        // Al editar se conserva la sucursal original aunque ya no esté activa.
                        selectedClientBranches = branches.filter { it.status == 1 || it.id == form.branchId }
                        loadingBranches = false
                        // Al repetir un pedido, la sucursal original pudo haberse eliminado.
                        val current = form
                        if (!isEditing && current.branchId != null &&
                            selectedClientBranches.none { it.id == current.branchId }
                        ) {
                            form = current.copy(branchId = null, branch = "", address = "")
                        }
                    }
                },
                onFailure = {
                    if (branchesRequestId == requestId && branchesClientId == clientId) {
                        loadingBranches = false
                        saveError = it.message
                    }
                }
            )
        }
    }

    LaunchedEffect(orderToEdit) {
        if (initialForm.clientType == "Cliente registrado" && initialForm.clientId != null) {
            loadBranches(initialForm.clientId)
        }
    }

    fun goBack() {
        when (step) {
            NewOrderStep.Catalog, NewOrderStep.Summary -> step = NewOrderStep.Details
            NewOrderStep.Details -> {
                // Solo pregunta si se capturó algo respecto a como se abrió la pantalla.
                val hasChanges = form != initialForm
                if (hasChanges) {
                    showDiscardDialog = true
                } else {
                    onBack()
                }
            }
        }
    }

    if (showDiscardDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showDiscardDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(28.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        if (isEditing) "¿Descartar cambios?" else "¿Descartar información?",
                        fontWeight = FontWeight.Bold,
                        color = Navy,
                        fontSize = 18.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (isEditing) "Si regresas al detalle, los cambios al pedido se perderán."
                        else "Si regresas al dashboard, los datos capturados se perderán.",
                        color = Gray
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showDiscardDialog = false }) { Text("Continuar editando") }
                        TextButton(onClick = {
                            showDiscardDialog = false
                            if (isEditing) {
                                onBack()
                            } else {
                                form = NewOrderFormState()
                                step = NewOrderStep.Details
                            }
                        }) { Text("Descartar", color = Color(0xFFB91C1C)) }
                    }
                }
            }
        }
    }

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
            NewOrderHeader(
                step = step,
                isEditing = isEditing,
                onBack = ::goBack,
                onSettings = { showSettings = true }
            )
        },
        bottomBar = { DashboardNavigation() }
    ) { insets ->
        when (step) {
            NewOrderStep.Catalog -> CatalogStep(
                inventory = inventory,
                loading = loading,
                error = error,
                quantities = form.quantities,
                showStock = sessionRol.canSeeInventory,
                onQuantityChange = { talla, value ->
                    form = form.copy(quantities = form.quantities + (talla to value))
                },
                onNext = { step = NewOrderStep.Details },
                modifier = Modifier.padding(insets)
            )
            NewOrderStep.Details -> DetailsStep(
                form = form,
                isClient = isClient,
                clients = clients,
                selectedClientBranches = selectedClientBranches,
                loadingBranches = loadingBranches,
                onClientSelected = { client -> loadBranches(client?.id) },
                onFormChange = { form = it },
                onDatePicker = { showDatePicker = true },
                onContinue = { step = NewOrderStep.Summary },
                onCatalog = { step = NewOrderStep.Catalog },
                inventory = inventory,
                modifier = Modifier.padding(insets)
            )
            NewOrderStep.Summary -> SummaryStep(
                form = form,
                inventory = inventory,
                onFormChange = { form = it },
                onEdit = { step = NewOrderStep.Details },
                onProducts = { step = NewOrderStep.Catalog },
                submitLabel = if (isEditing) "Guardar cambios" else "Registrar pedido pendiente",
                onSubmit = {
                    val products = selectedProducts(form.quantities, inventory)
                    val totalKg = products.sumOf { form.quantities[it.talla]?.toDoubleOrNull() ?: 0.0 }
                    val total = products.sumOf { it.precio * (form.quantities[it.talla]?.toDoubleOrNull() ?: 0.0) } +
                        form.shippingCost
                    val date = form.deliveryDate.split('/').let {
                        if (it.size == 3) "${it[2]}${it[1]}${it[0]}".toIntOrNull() else null
                    } ?: 0
                    if (exceedsStock(form.quantities, products)) {
                        saveError = OUT_OF_STOCK_MESSAGE
                        return@SummaryStep
                    }
                    val invalidProductCode = products.firstOrNull { it.codigoProducto <= 0 }
                    if (invalidProductCode != null) {
                        saveError = "El producto ${invalidProductCode.talla} no tiene un CodigoProducto válido en el inventario."
                        return@SummaryStep
                    }
                    val isPickup = form.deliveryMode == "Recolección"
                    val delivery = OrderDeliveryInfo(
                        clientName = form.client.trim(),
                        clientType = form.clientType,
                        method = form.deliveryMode,
                        address = if (isPickup) "" else form.address,
                        phone = form.phone.trim(),
                        branchName = if (isPickup) "" else form.branch,
                        references = form.references,
                        shippingCost = form.shippingCost
                    )
                    val request = OrderRequest(
                        clientId = form.clientId.takeIf { form.clientType == "Cliente registrado" },
                        branchId = form.branchId.takeUnless { isPickup },
                        date = date,
                        totalKg = totalKg,
                        total = total,
                        status = orderToEdit?.status ?: 1,
                        notes = OrderObservations(form.notes, delivery).encode(),
                        lines = products.map {
                            val quantity = form.quantities[it.talla]?.toDoubleOrNull() ?: 0.0
                            OrderLineRequest(it.codigoProducto, it.talla, quantity, it.precio, quantity * it.precio)
                        }
                    )
                    isSaving = true
                    saveError = null
                    scope.launch {
                        if (orderToEdit != null) {
                            orderRepository.updateOrder(orderToEdit.id, request).fold(
                                onSuccess = { isSaving = false; onOrderUpdated(it) },
                                onFailure = { isSaving = false; saveError = it.message }
                            )
                        } else {
                            orderRepository.createOrder(request).fold(
                                onSuccess = { isSaving = false; onOrderCreated() },
                                onFailure = { isSaving = false; saveError = it.message }
                            )
                        }
                    }
                },
                modifier = Modifier.padding(insets)
            )
        }
    }

    if (showDatePicker) {
        DateSelectionDialog(
            initialValue = form.deliveryDate,
            onDismissRequest = { showDatePicker = false },
            onConfirm = { date ->
                form = form.copy(deliveryDate = date)
                showDatePicker = false
            }
        )
    }

    if (isSaving) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(if (isEditing) "Guardando cambios" else "Registrando pedido") },
            text = { CircularProgressIndicator() },
            confirmButton = {}
        )
    }
    saveError?.let { message ->
        AlertDialog(
            onDismissRequest = { saveError = null },
            title = { Text(if (isEditing) "No se pudo guardar el pedido" else "No se pudo registrar el pedido") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { saveError = null }) { Text("Aceptar") } }
        )
    }
}

@Composable
private fun NewOrderHeader(
    step: NewOrderStep,
    isEditing: Boolean,
    onBack: () -> Unit,
    onSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Navy)
            .statusBarsPadding()
            .padding(start = 8.dp, end = 12.dp, top = 10.dp, bottom = 16.dp),
    ) {
        Text(
            if (isEditing) "EDITAR PEDIDO" else "NUEVO PEDIDO",
            color = Green,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(40.dp)
            ) {
                Icon(Icons.Outlined.ArrowBack, "Regresar", tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Text(
                when (step) {
                    NewOrderStep.Catalog -> "CATÁLOGO"
                    NewOrderStep.Details -> if (isEditing) "DATOS DEL PEDIDO" else "GENERAR PEDIDO"
                    NewOrderStep.Summary -> "RESUMEN DEL PEDIDO"
                },
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center)
            )
            IconButton(
                onClick = onSettings,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(40.dp)
            ) {
                Icon(Icons.Outlined.Settings, "Configuración", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun CatalogStep(
    inventory: List<ArticuloInventario>,
    loading: Boolean,
    error: String?,
    quantities: Map<String, String>,
    showStock: Boolean,
    onQuantityChange: (String, String) -> Unit,
    onNext: () -> Unit,
    modifier: Modifier
) {
    Column(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Text("CATÁLOGO", color = Navy, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 24.dp, bottom = 16.dp))
        when {
            loading -> Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Green)
            }
            error != null -> Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
            inventory.isEmpty() -> Text("No hay productos disponibles.", color = Gray)
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                items(inventory, key = { it.talla }) { product ->
                    CatalogProductCard(product, quantities[product.talla] ?: "0", showStock) {
                        // Sin ver existencias no se limita la cantidad; se valida al registrar el pedido.
                        val maximum = if (showStock) product.cantidadKg else Double.MAX_VALUE
                        onQuantityChange(product.talla, normalizeQuantity(it, maximum))
                    }
                }
            }
        }
        Button(
            onClick = onNext,
                    enabled = quantities.values.any { it.toDoubleOrNull()?.let { value -> value > 0 } == true },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1197C5)),
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).height(52.dp)
        ) { Text("Siguiente", fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun CatalogProductCard(
    product: ArticuloInventario,
    quantity: String,
    showStock: Boolean,
    onQuantityChange: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(product.talla, color = Navy, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Precio: $${product.precio} / kg", color = Color.Black, fontSize = 14.sp)
                if (showStock) {
                    Text("Disponible: ${product.cantidadKg} kg", color = Gray, fontSize = 11.sp)
                }
            }
            OutlinedTextField(
                value = quantity,
                onValueChange = onQuantityChange,
                label = { Text("Kg") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.size(width = 100.dp, height = 64.dp)
            )
        }
    }
}

@Composable
private fun DetailsStep(
    form: NewOrderFormState,
    isClient: Boolean,
    clients: List<Client>,
    selectedClientBranches: List<Branch>,
    loadingBranches: Boolean,
    onClientSelected: (Client?) -> Unit,
    onFormChange: (NewOrderFormState) -> Unit,
    onDatePicker: () -> Unit,
    onContinue: () -> Unit,
    onCatalog: () -> Unit,
    inventory: List<ArticuloInventario>,
    modifier: Modifier
) {
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("DETALLES DEL PEDIDO", color = Navy, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 22.dp))
            if (isClient) {
                if (form.clientId == null) {
                    Notice("Tu usuario aún no está vinculado a un cliente. Comunícate con tu asesor de ventas.")
                } else {
                    Text("Cliente", color = Gray, fontSize = 12.sp)
                    Text(form.client.ifBlank { "Mi cuenta" }, color = Navy, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            } else {
            DropdownField(
                label = "Tipo de cliente",
                value = form.clientType,
                options = listOf("Cliente registrado", "Cliente esporádico"),
                onSelected = {
                    onClientSelected(null)
                    onFormChange(
                        form.copy(
                            clientType = it,
                            client = "",
                            clientId = null,
                            phone = "",
                            branch = "",
                            branchId = null,
                            address = "",
                            street = "",
                            neighborhood = "",
                            postalCode = ""
                        )
                    )
                }
            )
            if (form.clientType == "Cliente registrado") {
                DropdownField(
                    label = "Cliente",
                    value = form.client,
                    options = clients.map { it.name },
                    onSelected = { name ->
                        val client = clients.firstOrNull { it.name == name }
                        onClientSelected(client)
                        onFormChange(
                            form.copy(
                                client = name,
                                clientId = client?.id,
                                phone = client?.phone.orEmpty(),
                                branch = "",
                                branchId = null,
                                address = ""
                            )
                        )
                    }
                )
            } else if (form.clientType == "Cliente esporádico") {
                FormField("Nombre del cliente", form.client, { onFormChange(form.copy(client = it)) })
            }
            if (form.clientType.isNotBlank()) {
                FormField(
                    label = "Celular",
                    value = form.phone,
                    onValueChange = { onFormChange(form.copy(phone = it)) },
                    keyboardType = KeyboardType.Phone,
                    readOnly = form.clientType == "Cliente registrado"
                )
            }
            }
            Spacer(Modifier.height(10.dp))
            Text("RESUMEN DEL PEDIDO", color = Navy, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        items(selectedProducts(form.quantities, inventory), key = { it.talla }) { product ->
            MiniProductCard(
                product = product,
                quantity = form.quantities[product.talla] ?: "0",
                onEdit = onCatalog,
                onRemove = {
                    onFormChange(form.copy(quantities = form.quantities - product.talla))
                }
            )
        }
        item {
            Button(onClick = onCatalog, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1197C5)), modifier = Modifier.fillMaxWidth()) {
                Text("Agregar productos")
            }
            Spacer(Modifier.height(14.dp))
            Text("MÉTODO DE ENTREGA", color = Navy, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth()) {
                listOf("Dirección", "Recolección").forEach { mode ->
                    TextButton(
                        onClick = {
                            onFormChange(
                                form.copy(
                                    deliveryMode = mode,
                                    branchId = null,
                                    branch = if (mode == "Recolección") "Congeladora Global Foods" else "",
                                    address = if (mode == "Recolección") freezerAddress else "",
                                    street = if (mode == "Recolección") "" else form.street,
                                    neighborhood = if (mode == "Recolección") "" else form.neighborhood,
                                    postalCode = if (mode == "Recolección") "" else form.postalCode
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(mode, color = if (form.deliveryMode == mode) Green else Gray, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (form.deliveryMode == "Recolección") {
                AddressCard(freezerAddress, title = "Dirección de la congeladora")
            } else if (form.clientType == "Cliente registrado") {
                BranchField(
                    value = form.branch,
                    options = selectedClientBranches.map { it.name },
                    onSelected = { branchName ->
                        val branch = selectedClientBranches.firstOrNull { it.name == branchName }
                        onFormChange(form.copy(branch = branchName, branchId = branch?.id, address = branch?.address.orEmpty()))
                    }
                )
                if (loadingBranches) {
                    CircularProgressIndicator(color = Green, modifier = Modifier.size(20.dp))
                } else if (selectedClientBranches.isEmpty()) {
                    Text(
                        if (isClient) "No tienes sucursales registradas. Agrégalas en la pestaña Sucursales o elige Recolección."
                        else "La API no devolvió sucursales para este cliente.",
                        color = Color(0xFFB91C1C),
                        fontSize = 12.sp
                    )
                } else if (form.branch.isBlank()) {
                    Text(
                        "Selecciona una sucursal de tu perfil para continuar.",
                        color = Color(0xFFB45309),
                        fontSize = 12.sp
                    )
                } else {
                    AddressCard(form.address)
                }
            } else {
                Text(
                    "Dirección de entrega",
                    color = Navy,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                FormField(
                    "Calle",
                    form.street,
                    {
                        onFormChange(
                            form.copy(
                                street = it,
                                address = buildAddress(it, form.neighborhood, form.postalCode)
                            )
                        )
                    }
                )
                FormField(
                    "Colonia",
                    form.neighborhood,
                    {
                        onFormChange(
                            form.copy(
                                neighborhood = it,
                                address = buildAddress(form.street, it, form.postalCode)
                            )
                        )
                    }
                )
                FormField(
                    "Código postal",
                    form.postalCode,
                    {
                        val postalCode = it.filter(Char::isDigit).take(5)
                        onFormChange(
                            form.copy(
                                postalCode = postalCode,
                                address = buildAddress(form.street, form.neighborhood, postalCode)
                            )
                        )
                    },
                    KeyboardType.Number
                )
                if (form.street.isBlank() || form.neighborhood.isBlank() || form.postalCode.isBlank()) {
                    Text(
                        "Captura calle, colonia y código postal para definir la ubicación de entrega.",
                        color = Color(0xFFB45309),
                        fontSize = 12.sp
                    )
                }
            }
            if (form.clientType.isBlank()) {
                Text(
                    "Selecciona el tipo de cliente para definir la ubicación de entrega.",
                    color = Color(0xFFB45309),
                    fontSize = 12.sp
                )
            }
            DateField(form.deliveryDate, onClick = onDatePicker)
            FormField("Observaciones", form.notes, { onFormChange(form.copy(notes = it)) }, singleLine = false)
            FormField("Costo de envío", form.shipping, { onFormChange(form.copy(shipping = it)) }, KeyboardType.Decimal)
            val hasProducts = form.quantities.values.any { it.toDoubleOrNull()?.let { value -> value > 0 } == true }
            val hasClient = if (form.clientType == "Cliente registrado") form.clientId != null else form.client.isNotBlank()
            val hasLocation = form.deliveryMode == "Recolección" ||
                (form.clientType == "Cliente registrado" && form.branchId != null) ||
                (form.clientType == "Cliente esporádico" &&
                    form.street.isNotBlank() &&
                    form.neighborhood.isNotBlank() &&
                    form.postalCode.length == 5)
            Button(
                onClick = onContinue,
                enabled = hasClient && hasProducts && hasLocation,
                colors = ButtonDefaults.buttonColors(containerColor = Navy),
                modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp).height(52.dp)
            ) {
                Text("Continuar al resumen", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SummaryStep(
    form: NewOrderFormState,
    inventory: List<ArticuloInventario>,
    onFormChange: (NewOrderFormState) -> Unit,
    onEdit: () -> Unit,
    onProducts: () -> Unit,
    submitLabel: String,
    onSubmit: () -> Unit,
    modifier: Modifier
) {
    val products = selectedProducts(form.quantities, inventory)
    val totalKg = products.sumOf { form.quantities[it.talla]?.toDoubleOrNull() ?: 0.0 }
    val subtotal = products.sumOf { product ->
        (form.quantities[product.talla]?.toDoubleOrNull() ?: 0.0) * product.precio
    }
    val shippingCost = form.shippingCost
    val total = subtotal + shippingCost
    LazyColumn(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("CLIENTE", color = Gray, fontSize = 12.sp)
            Text(form.client.ifBlank { "Cliente esporádico" }, color = Navy, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("UBICACIÓN DE ENTREGA", color = Gray, fontSize = 12.sp)
            Text(
                if (form.deliveryMode == "Recolección") freezerAddress
                else form.address.ifBlank { "Ubicación pendiente" },
                color = Navy,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            Text("RESUMEN DEL PEDIDO", color = Navy, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        items(products, key = { it.talla }) { product ->
            MiniProductCard(
                product = product,
                quantity = form.quantities[product.talla] ?: "0",
                onEdit = onProducts,
                onRemove = {
                    onFormChange(form.copy(quantities = form.quantities - product.talla))
                }
            )
        }
        item {
            HorizontalDivider()
            SummaryAmountRow("Total de kilos", "${totalKg} kg")
            SummaryAmountRow("Subtotal", "$${subtotal}")
            SummaryAmountRow("Costo de envío", "$${shippingCost}")
            SummaryAmountRow("Descuento", "$0.00")
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SummaryAmountRow("Importe total", "$${total}", emphasized = true)
            Text("El pago se registrará en una etapa posterior.", color = Gray, fontSize = 12.sp)
            Button(onClick = onSubmit, colors = ButtonDefaults.buttonColors(containerColor = Navy), modifier = Modifier.fillMaxWidth().padding(top = 14.dp).height(52.dp)) {
                Text(submitLabel, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SummaryAmountRow(label: String, value: String, emphasized: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            color = Navy,
            fontSize = if (emphasized) 18.sp else 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            value,
            color = Navy,
            fontSize = if (emphasized) 18.sp else 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun selectedProducts(quantities: Map<String, String>, inventory: List<ArticuloInventario>) =
    inventory.filter { quantities[it.talla]?.toDoubleOrNull()?.let { value -> value > 0 } == true }

private fun normalizeQuantity(value: String, maximum: Double): String {
    val normalized = value.replace(',', '.')
        .filter { it.isDigit() || it == '.' }
    val amount = normalized.toDoubleOrNull() ?: return ""
    return amount.coerceIn(0.0, maximum).let { result ->
        if (result == 0.0) "" else result.toString().trimEnd('0').trimEnd('.')
    }
}

@Composable
private fun MiniProductCard(
    product: ArticuloInventario,
    quantity: String,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(product.talla, color = Navy, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text("Precio: $${product.precio} / kg", color = Color.Black, fontSize = 12.sp)
            }
            Text("$quantity kg", color = Navy, fontWeight = FontWeight.Bold)
            IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, "Editar producto", tint = Gray) }
            IconButton(onClick = onRemove) { Icon(Icons.Outlined.Delete, "Eliminar producto", tint = Color(0xFFB91C1C)) }
        }
    }
}

@Composable
private fun DropdownField(
    label: String,
    value: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text("Seleccionar") },
            modifier = Modifier.fillMaxWidth(),
            colors = outlinedFieldColors()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun BranchField(value: String, options: List<String>, onSelected: (String) -> Unit) {
    DropdownField(
        label = "Sucursal",
        value = value,
        options = options,
        onSelected = onSelected
    )
}

private fun branchAddress(branch: String): String =
    customerBranches.firstOrNull { it.first == branch }?.second.orEmpty()

private fun buildAddress(street: String, neighborhood: String, postalCode: String): String =
    listOf(
        street.trim(),
        neighborhood.trim(),
        postalCode.trim().let { if (it.isBlank()) "" else "C.P. $it" }
    ).filter(String::isNotBlank).joinToString(", ")

@Composable
private fun AddressCard(address: String, title: String = "Dirección de entrega") {
    val uriHandler = LocalUriHandler.current
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, color = Navy, fontWeight = FontWeight.Bold)
            Text(
                address.ifBlank { "La sucursal seleccionada no tiene dirección disponible." },
                color = Gray,
                modifier = Modifier.padding(vertical = 6.dp)
            )
            TextButton(onClick = {
                uriHandler.openUri(
                    "https://www.google.com/maps/search/?api=1&query=${address.ifBlank { "Global Foods Mexico" }.replace(" ", "+")}"
                )
            }) {
                Text("Revisar ubicación en Google Maps", color = Color(0xFF168BC1))
            }
        }
    }
}

@Composable
private fun DateField(value: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text("Fecha de entrega") },
            placeholder = { Text("Seleccionar fecha") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = { Text("▣", color = Navy, modifier = Modifier.padding(end = 12.dp)) },
            colors = outlinedFieldColors(
                disabledTextColor = Navy,
                disabledBorderColor = Color.Black,
                disabledLabelColor = Gray,
                disabledPlaceholderColor = Gray,
                disabledTrailingIconColor = Navy
            )
        )
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateSelectionDialog(
    initialValue: String,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val today = currentDateMillis()
    val firstSelectableDate = today + MILLIS_PER_DAY
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = dateToMillis(initialValue)
            ?.takeIf { it >= firstSelectableDate },
        yearRange = 1900..2100,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                utcTimeMillis >= firstSelectableDate
        }
    )
    val selectedDate = datePickerState.selectedDateMillis

    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        confirmButton = {
            TextButton(
                onClick = { onConfirm(millisToDate(selectedDate!!)) },
                enabled = selectedDate != null
            ) { Text("Aceptar") }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("Cancelar") }
        }
    ) {
        DatePicker(
            state = datePickerState,
            showModeToggle = false,
            title = {
                Text(
                    "Seleccionar fecha",
                    color = Navy,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            },
            headline = {
                Text(
                    "Selecciona la fecha de entrega",
                    color = Gray,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun currentDateMillis(): Long =
    (kotlin.time.Clock.System.now().toEpochMilliseconds() / MILLIS_PER_DAY) * MILLIS_PER_DAY

private fun dateToMillis(value: String): Long? {
    if (!value.matches(Regex("\\d{2}/\\d{2}/\\d{4}"))) return null

    val day = value.substring(0, 2).toInt()
    val month = value.substring(3, 5).toInt()
    val year = value.substring(6, 10).toInt()

    if (year !in 1900..2100 || month !in 1..12) return null

    val daysInMonth = when (month) {
        2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }

    if (day !in 1..daysInMonth) return null

    return daysFromCivil(year, month, day) * MILLIS_PER_DAY
}

private fun millisToDate(millis: Long): String {
    val (year, month, day) = civilFromDays(millis / MILLIS_PER_DAY)
    return buildString {
        append(day.toString().padStart(2, '0'))
        append('/')
        append(month.toString().padStart(2, '0'))
        append('/')
        append(year.toString().padStart(4, '0'))
    }
}

private const val MILLIS_PER_DAY = 86_400_000L

private fun daysFromCivil(year: Int, month: Int, day: Int): Long {
    var adjustedYear = year
    adjustedYear -= if (month <= 2) 1 else 0
    val era = if (adjustedYear >= 0) adjustedYear / 400 else (adjustedYear - 399) / 400
    val yearOfEra = adjustedYear - era * 400
    val monthFromMarch = month + if (month > 2) -3 else 9
    val dayOfYear = (153 * monthFromMarch + 2) / 5 + day - 1
    val dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear
    return era * 146097L + dayOfEra - 719468L
}

private fun civilFromDays(days: Long): Triple<Int, Int, Int> {
    val adjustedDays = days + 719468L
    val era = if (adjustedDays >= 0) adjustedDays / 146097 else (adjustedDays - 146096) / 146097
    val dayOfEra = adjustedDays - era * 146097
    val yearOfEra = (dayOfEra - dayOfEra / 1460 + dayOfEra / 36524 - dayOfEra / 146096) / 365
    var year = (yearOfEra + era * 400).toInt()
    val dayOfYear = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100)
    val monthPart = (5 * dayOfYear + 2) / 153
    val day = (dayOfYear - (153 * monthPart + 2) / 5 + 1).toInt()
    val month = (monthPart + if (monthPart < 10) 3 else -9).toInt()
    year += if (month <= 2) 1 else 0
    return Triple(year, month, day)
}

@Composable
private fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    readOnly: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = singleLine,
        readOnly = readOnly,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = outlinedFieldColors()
    )
}

@Composable
private fun outlinedFieldColors(
    disabledTextColor: Color = Navy,
    disabledBorderColor: Color = Color.Black,
    disabledLabelColor: Color = Gray,
    disabledPlaceholderColor: Color = Gray,
    disabledTrailingIconColor: Color = Navy
) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color.Black,
    unfocusedBorderColor = Color.Black,
    disabledBorderColor = disabledBorderColor,
    disabledTextColor = disabledTextColor,
    disabledLabelColor = disabledLabelColor,
    disabledPlaceholderColor = disabledPlaceholderColor,
    disabledTrailingIconColor = disabledTrailingIconColor
)
