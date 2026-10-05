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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.globalfoods.project.sharedlogic.network.dashboard.ArticuloInventario
import com.globalfoods.project.sharedlogic.network.dashboard.DashboardRepository

private enum class NewOrderStep { Catalog, Details, Summary }

private data class NewOrderFormState(
    val quantities: Map<String, String> = emptyMap(),
    val client: String = "",
    val clientType: String = "",
    val phone: String = "",
    val branch: String = "",
    val address: String = "",
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
            address.isNotBlank() || references.isNotBlank() || deliveryDate.isNotBlank() ||
            notes.isNotBlank() || shipping.isNotBlank()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewOrderScreen(onBack: () -> Unit, onLogout: () -> Unit) {
    val repository = remember { DashboardRepository() }
    var inventory by remember { mutableStateOf<List<ArticuloInventario>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var step by remember { mutableStateOf(NewOrderStep.Catalog) }
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
            NewOrderStep.Catalog -> onBack()
            NewOrderStep.Details, NewOrderStep.Summary -> {
                if (form.hasCapturedData) {
                    showDiscardDialog = true
                } else {
                    step = NewOrderStep.Catalog
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
                    Text("Si regresas al catálogo, los datos capturados se perderán.", color = Gray)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showDiscardDialog = false }) { Text("Continuar editando") }
                        TextButton(onClick = {
                            form = NewOrderFormState()
                            showDiscardDialog = false
                            step = NewOrderStep.Catalog
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
                onCatalog = ::goBack,
                inventory = inventory,
                modifier = Modifier.padding(insets)
            )
            NewOrderStep.Summary -> SummaryStep(
                form = form,
                inventory = inventory,
                onFormChange = { form = it },
                onEdit = { step = NewOrderStep.Details },
                onProducts = ::goBack,
                modifier = Modifier.padding(insets)
            )
        }
    }

    if (showDatePicker) {
        Dialog(
            onDismissRequest = { showDatePicker = false },
        ) {
            DateSelectionCard(
                initialValue = form.deliveryDate,
                onDismiss = { showDatePicker = false },
                onConfirm = { date ->
                    form = form.copy(deliveryDate = date)
                    showDatePicker = false
                }
            )
        }
    }
}

@Composable
private fun NewOrderHeader(
    step: NewOrderStep,
    onBack: () -> Unit,
    onSettings: () -> Unit
) {
    Column(
        Modifier.fillMaxWidth().background(Navy).padding(start = 8.dp, end = 12.dp, top = 10.dp, bottom = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, "Regresar", tint = Color.White)
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onSettings) {
                Icon(Icons.Outlined.Settings, "Configuración", tint = Color.White)
            }
        }
        Text("NUEVO PEDIDO", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(
            when (step) {
                NewOrderStep.Catalog -> "CATÁLOGO"
                NewOrderStep.Details -> "GENERAR PEDIDO"
                NewOrderStep.Summary -> "RESUMEN DEL PEDIDO"
            },
            color = Color.White,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )
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
                onSelected = { onFormChange(form.copy(clientType = it)) }
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
                        onClick = { onFormChange(form.copy(deliveryMode = mode)) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(mode, color = if (form.deliveryMode == mode) Green else Gray, fontWeight = FontWeight.Bold)
                    }
                }
            }
            BranchField(
                value = form.branch,
                onSelected = { onFormChange(form.copy(branch = it)) }
            )
            if (form.branch.isBlank()) {
                Text(
                    "Debes ingresar al menos una sucursal en tu perfil para continuar.",
                    color = Color(0xFFB45309),
                    fontSize = 12.sp
                )
            } else if (form.deliveryMode == "Dirección") {
                AddressCard(form.address)
            }
            FormField("Referencias", form.references, { onFormChange(form.copy(references = it)) }, singleLine = false)
            DateField(form.deliveryDate, onClick = onDatePicker)
            FormField("Notas opcionales", form.notes, { onFormChange(form.copy(notes = it)) }, singleLine = false)
            FormField("Costo de envío", form.shipping, { onFormChange(form.copy(shipping = it)) }, KeyboardType.Decimal)
            Button(onClick = onContinue, colors = ButtonDefaults.buttonColors(containerColor = Navy), modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp).height(52.dp)) {
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
        options = emptyList(),
        onSelected = onSelected
    )
}

@Composable
private fun AddressCard(address: String) {
    val uriHandler = LocalUriHandler.current
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Text("Dirección de entrega", color = Navy, fontWeight = FontWeight.Bold)
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

@Composable
private fun DateSelectionCard(
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var date by remember { mutableStateOf(initialValue) }
    val isValid = date.matches(Regex("\\d{2}/\\d{2}/\\d{4}"))

    Card(
        modifier = Modifier.fillMaxWidth().widthIn(max = 340.dp).padding(16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Seleccionar fecha", color = Navy, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Captura la fecha de entrega", color = Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))
            OutlinedTextField(
                value = date,
                onValueChange = { value ->
                    date = value.filter { it.isDigit() || it == '/' }.take(10)
                },
                label = { Text("Fecha de entrega") },
                placeholder = { Text("DD/MM/AAAA") },
                supportingText = { Text("Formato: DD/MM/AAAA") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = outlinedFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Cancelar") }
                TextButton(onClick = { onConfirm(date) }, enabled = isValid) { Text("Aceptar") }
            }
        }
    }
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
