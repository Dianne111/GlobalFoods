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
import com.globalfoods.project.sharedlogic.network.dashboard.ArticuloInventario
import com.globalfoods.project.sharedlogic.network.dashboard.DashboardRepository

private enum class NewOrderStep { Details, Catalog, Summary }

private val freezerAddress = "Congeladora Global Foods México, Av. Principal 123, Guadalajara, Jalisco"
private val customerBranches = listOf(
    "Sucursal Centro" to "Av. Juárez 120, Centro, Guadalajara, Jalisco",
    "Sucursal Norte" to "Av. Federalismo 850, Col. Lomas, Guadalajara, Jalisco"
)

private data class NewOrderFormState(
    val quantities: Map<String, String> = emptyMap(),
    val client: String = "",
    val clientType: String = "",
    val phone: String = "",
    val branch: String = "",
    val address: String = "",
    val street: String = "",
    val neighborhood: String = "",
    val postalCode: String = "",
    val references: String = "",
    val deliveryDate: String = "",
    val notes: String = "",
    val shipping: String = "",
    val deliveryMode: String = "Dirección",
    val payment: String = "Transferencia"
) {
    val hasCapturedData: Boolean
        get() = quantities.values.any { it.toDoubleOrNull()?.let { value -> value > 0 } == true } ||
            client.isNotBlank() || phone.isNotBlank() || branch.isNotBlank() ||
            address.isNotBlank() || street.isNotBlank() || neighborhood.isNotBlank() ||
            postalCode.isNotBlank() || references.isNotBlank() || deliveryDate.isNotBlank() ||
            notes.isNotBlank() || shipping.isNotBlank()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewOrderScreen(onBack: () -> Unit, onLogout: () -> Unit) {
    val repository = remember { DashboardRepository() }
    var inventory by remember { mutableStateOf<List<ArticuloInventario>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var step by remember { mutableStateOf(NewOrderStep.Details) }
    var form by remember { mutableStateOf(NewOrderFormState()) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        repository.obtenerInventario().fold(
            onSuccess = { inventory = it; loading = false },
            onFailure = { error = it.message; loading = false }
        )
    }

    fun goBack() {
        when (step) {
            NewOrderStep.Catalog, NewOrderStep.Summary -> step = NewOrderStep.Details
            NewOrderStep.Details -> {
                if (form.hasCapturedData) {
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
                    Text("¿Descartar información?", fontWeight = FontWeight.Bold, color = Navy, fontSize = 18.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Si regresas al dashboard, los datos capturados se perderán.", color = Gray)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showDiscardDialog = false }) { Text("Continuar editando") }
                        TextButton(onClick = {
                            form = NewOrderFormState()
                            showDiscardDialog = false
                            step = NewOrderStep.Details
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
                onQuantityChange = { talla, value ->
                    form = form.copy(quantities = form.quantities + (talla to value))
                },
                onNext = { step = NewOrderStep.Details },
                modifier = Modifier.padding(insets)
            )
            NewOrderStep.Details -> DetailsStep(
                form = form,
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
}

@Composable
private fun NewOrderHeader(
    step: NewOrderStep,
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
            "NUEVO PEDIDO",
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
                    NewOrderStep.Details -> "GENERAR PEDIDO"
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
                    CatalogProductCard(product, quantities[product.talla] ?: "0") {
                        onQuantityChange(product.talla, normalizeQuantity(it, product.cantidadKg))
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
private fun CatalogProductCard(product: ArticuloInventario, quantity: String, onQuantityChange: (String) -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(product.talla, color = Navy, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Precio: $${product.precio} / kg", color = Color.Black, fontSize = 14.sp)
                Text("Disponible: ${product.cantidadKg} kg", color = Gray, fontSize = 11.sp)
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
            DropdownField(
                label = "Tipo de cliente",
                value = form.clientType,
                options = listOf("Cliente registrado", "Cliente esporádico"),
                onSelected = {
                    onFormChange(
                        form.copy(
                            clientType = it,
                            branch = "",
                            address = "",
                            street = "",
                            neighborhood = "",
                            postalCode = ""
                        )
                    )
                }
            )
            if (form.clientType.isNotBlank()) {
                FormField("Nombre del cliente", form.client, { onFormChange(form.copy(client = it)) })
                FormField("Celular", form.phone, { onFormChange(form.copy(phone = it)) }, KeyboardType.Phone)
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
                    onSelected = { branch ->
                        onFormChange(form.copy(branch = branch, address = branchAddress(branch)))
                    }
                )
                if (form.branch.isBlank()) {
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
            FormField("Referencias", form.references, { onFormChange(form.copy(references = it)) }, singleLine = false)
            DateField(form.deliveryDate, onClick = onDatePicker)
            FormField("Notas opcionales", form.notes, { onFormChange(form.copy(notes = it)) }, singleLine = false)
            FormField("Costo de envío", form.shipping, { onFormChange(form.copy(shipping = it)) }, KeyboardType.Decimal)
            val hasProducts = form.quantities.values.any { it.toDoubleOrNull()?.let { value -> value > 0 } == true }
            val hasLocation = form.deliveryMode == "Recolección" ||
                (form.clientType == "Cliente registrado" && form.branch.isNotBlank()) ||
                (form.clientType == "Cliente esporádico" &&
                    form.street.isNotBlank() &&
                    form.neighborhood.isNotBlank() &&
                    form.postalCode.length == 5)
            Button(
                onClick = onContinue,
                enabled = form.clientType.isNotBlank() && hasProducts && hasLocation,
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
    modifier: Modifier
) {
    val products = selectedProducts(form.quantities, inventory)
    val totalKg = products.sumOf { form.quantities[it.talla]?.toDoubleOrNull() ?: 0.0 }
    val subtotal = products.sumOf { product ->
        (form.quantities[product.talla]?.toDoubleOrNull() ?: 0.0) * product.precio
    }
    val shippingCost = form.shipping.replace(',', '.').toDoubleOrNull() ?: 0.0
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
            Text("FORMA DE PAGO", color = Navy, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
            listOf("Transferencia", "Efectivo", "Crédito").forEach { option ->
                Row(Modifier.fillMaxWidth().clickable { onFormChange(form.copy(payment = option)) }, verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = form.payment == option, onClick = { onFormChange(form.copy(payment = option)) })
                    Text(option, color = Navy)
                }
            }

            FormField("Referencia bancaria (si aplica)", "", {})
            Button(onClick = onEdit, colors = ButtonDefaults.buttonColors(containerColor = Navy), modifier = Modifier.fillMaxWidth().padding(top = 14.dp).height(52.dp)) {
                Text("Registrar pedido", fontWeight = FontWeight.Bold)
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
private fun BranchField(value: String, onSelected: (String) -> Unit) {
    DropdownField(
        label = "Sucursal",
        value = value,
        options = customerBranches.map { it.first },
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
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = singleLine,
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
