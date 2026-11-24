package com.globalfoods.project

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.globalfoods.project.sharedlogic.network.auth.UserRole
import com.globalfoods.project.sharedlogic.network.core.sessionRol
import com.globalfoods.project.sharedlogic.network.dashboard.ArticuloInventario
import kotlin.math.roundToLong

/*
- Productos del pedido en la vista del vendedor/administrador, igual que en la app web: se agregan
- desde un panel con búsqueda y luego se ajusta la cantidad con − / +. Muestra los kg disponibles
- (CU-04, RF-PRO-01). La vista del cliente sigue usando el catálogo.
 */

internal const val QUANTITY_MESSAGE = "La cantidad debe ser mayor que cero."

/** "Solo hay 40 kg disponibles de 26-30. Ajuste la cantidad." (RF-PED-01). */
internal fun notEnoughStockMessage(product: ArticuloInventario): String =
    "Solo hay ${formatOrderKg(product.cantidadKg)} kg disponibles de ${product.talla}. Ajuste la cantidad."

/** Productos agregados al pedido, en el orden en que se agregaron. */
internal fun addedProducts(quantities: Map<String, String>, inventory: List<ArticuloInventario>): List<ArticuloInventario> {
    val bySize = inventory.associateBy { it.talla }
    return quantities.keys.mapNotNull { bySize[it] }
}

/** Busca por talla (sin importar mayúsculas) o por código de producto. */
internal fun searchInventory(inventory: List<ArticuloInventario>, query: String): List<ArticuloInventario> {
    val text = query.trim()
    if (text.isEmpty()) return inventory
    return inventory.filter {
        it.talla.contains(text, ignoreCase = true) || it.codigoProducto.toString().contains(text)
    }
}

/** Cantidad inicial al agregar: 1 kg, o lo disponible si hay menos. */
internal fun initialQuantity(product: ArticuloInventario, role: UserRole = sessionRol): String {
    val maxLimit = if (role.canSeeInventory) product.cantidadKg else Double.MAX_VALUE
    return formatOrderKg(minOf(1.0, maxLimit).takeIf { it > 0 } ?: 1.0)
}

/** Suma [delta] kg a la cantidad, sin bajar de 1 ni pasar de [maximum]. */
internal fun stepQuantity(current: String, delta: Double, maximum: Double): String {
    val value = (current.replace(',', '.').toDoubleOrNull() ?: 0.0) + delta
    return formatOrderKg(value.coerceAtMost(maximum).coerceAtLeast(minOf(1.0, maximum)))
}

/** Cantidad vacía, en cero o negativa, por producto agregado. */
internal fun zeroQuantityErrors(quantities: Map<String, String>): Map<String, String> =
    quantities.filterValues { (it.replace(',', '.').toDoubleOrNull() ?: 0.0) <= 0.0 }.mapValues { QUANTITY_MESSAGE }

/** Cantidad mayor que la disponible, por producto agregado. */
internal fun stockErrors(quantities: Map<String, String>, inventory: List<ArticuloInventario>): Map<String, String> =
    addedProducts(quantities, inventory)
        .filter { (quantities[it.talla]?.replace(',', '.')?.toDoubleOrNull() ?: 0.0) > it.cantidadKg }
        .associate { it.talla to notEnoughStockMessage(it) }

internal fun formatOrderKg(value: Double): String {
    val rounded = (value * 1000).roundToLong() / 1000.0
    return rounded.toString().let { if (it.contains('.')) it.trimEnd('0').trimEnd('.') else it }
}

/** "$1,234.50" */
internal fun formatMoney(value: Double): String {
    val cents = (value * 100).roundToLong()
    val whole = (cents / 100).toString().reversed().chunked(3).joinToString(",").reversed()
    return "$$whole.${(cents % 100).toString().padStart(2, '0')}"
}

/** Sección "Productos" del formulario: tarjetas con − / +, subtotales y el importe. */
@Composable
internal fun AdminProductsSection(
    quantities: Map<String, String>,
    inventory: List<ArticuloInventario>,
    loading: Boolean,
    loadError: String?,
    /** Error de la sección (sin productos) y errores por talla, ya filtrados para mostrarse. */
    sectionError: String?,
    lineErrors: Map<String, String>,
    onQuantitiesChange: (Map<String, String>) -> Unit
) {
    var showSheet by remember { mutableStateOf(false) }
    val products = addedProducts(quantities, inventory)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("PRODUCTOS", color = Navy, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = { showSheet = true }) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Agregar producto")
            }
        }
        if (products.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = Gray)
                    Text("Aún no hay productos en el pedido", color = Navy, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Use «Agregar producto» para elegirlos del inventario disponible.",
                        color = Gray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        products.forEach { product ->
            ProductQuantityCard(
                product = product,
                quantity = quantities[product.talla].orEmpty(),
                error = lineErrors[product.talla],
                onQuantityChange = { onQuantitiesChange(quantities + (product.talla to it)) },
                onRemove = { onQuantitiesChange(quantities - product.talla) }
            )
        }
        sectionError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
        }
        val totalKg = products.sumOf { quantities[it.talla]?.replace(',', '.')?.toDoubleOrNull() ?: 0.0 }
        val total = products.sumOf { (quantities[it.talla]?.replace(',', '.')?.toDoubleOrNull() ?: 0.0) * it.precio }
        Row(Modifier.fillMaxWidth().padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${products.size} ${if (products.size == 1) "producto" else "productos"} · ${formatOrderKg(totalKg)} kg",
                color = Gray,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )
            Text("Importe de productos  ", color = Gray, fontSize = 12.sp)
            Text(formatMoney(total), color = Navy, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }

    if (showSheet) {
        AddProductSheet(
            inventory = inventory,
            loading = loading,
            loadError = loadError,
            added = quantities.keys,
            onAdd = { product -> onQuantitiesChange(quantities + (product.talla to initialQuantity(product))) },
            onDismiss = { showSheet = false }
        )
    }
}

@Composable
private fun ProductQuantityCard(
    product: ArticuloInventario,
    quantity: String,
    error: String?,
    onQuantityChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    val isClient = sessionRol == UserRole.Client
    val maxLimit = if (isClient) Double.MAX_VALUE else product.cantidadKg
    val value = quantity.replace(',', '.').toDoubleOrNull() ?: 0.0
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (error != null) Modifier.border(1.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(12.dp)) else Modifier)
    ) {
        Column(Modifier.padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(product.talla, color = Navy, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        if (isClient) "${formatMoney(product.precio)} por kg"
                        else "${formatMoney(product.precio)} por kg · ${formatOrderKg(product.cantidadKg)} kg disponibles",
                        color = Gray,
                        fontSize = 12.sp
                    )
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Outlined.Delete, "Quitar ${product.talla}", tint = Color(0xFFB91C1C))
                }
            }
            Spacer(Modifier.size(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                StepButton(Icons.Outlined.Remove, "Quitar 1 kg", enabled = value > 1.0) {
                    onQuantityChange(stepQuantity(quantity, -1.0, maxLimit))
                }
                BasicTextField(
                    value = quantity,
                    onValueChange = { onQuantityChange(normalizeQuantity(it, maxLimit)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = TextStyle(color = Navy, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .width(64.dp)
                        .background(Background, RoundedCornerShape(8.dp))
                        .padding(vertical = 8.dp)
                )
                StepButton(Icons.Outlined.Add, "Agregar 1 kg", enabled = value < maxLimit) {
                    onQuantityChange(stepQuantity(quantity, 1.0, maxLimit))
                }
                Text(" kg", color = Gray, fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                Text(formatMoney(value * product.precio), color = Navy, fontWeight = FontWeight.Bold)
            }
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

@Composable
private fun StepButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .border(1.dp, if (enabled) Color(0xFFCBD5E1) else Color(0xFFE2E8F0), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(36.dp)) {
            Icon(icon, description, tint = if (enabled) Navy else Color(0xFFCBD5E1), modifier = Modifier.size(18.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddProductSheet(
    inventory: List<ArticuloInventario>,
    loading: Boolean,
    loadError: String?,
    added: Set<String>,
    onAdd: (ArticuloInventario) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val results = remember(inventory, query) { searchInventory(inventory, query) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White
    ) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding()) {
            Text("Agregar producto", color = Navy, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.size(12.dp))
            SearchField(query, { query = it }, placeholder = "Talla o código, por ejemplo 16/20")
            Spacer(Modifier.size(8.dp))
            when {
                loading -> Box(Modifier.fillMaxWidth().heightIn(min = 160.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Green)
                }
                loadError != null -> Text(loadError, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 16.dp))
                results.isEmpty() -> Text(
                    "No se encontraron productos.",
                    color = Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)
                )
                else -> LazyColumn(Modifier.weight(1f, fill = false).heightIn(max = 460.dp)) {
                    items(results, key = { it.talla }) { product ->
                        AddProductRow(product, alreadyAdded = product.talla in added, onAdd = { onAdd(product) })
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                    }
                }
            }
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Navy),
                modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp)
            ) { Text("Listo", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun AddProductRow(product: ArticuloInventario, alreadyAdded: Boolean, onAdd: () -> Unit) {
    val isClient = sessionRol == UserRole.Client
    val available = if (isClient) true else product.cantidadKg > 0
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(product.talla, color = Navy, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Venta por kg", color = Gray, fontSize = 12.sp)
            Text(
                if (isClient) "${formatMoney(product.precio)} por kg"
                else "${formatMoney(product.precio)} por kg · ${formatOrderKg(product.cantidadKg)} kg disponibles",
                color = Gray,
                fontSize = 12.sp
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (!isClient) {
                AvailabilityBadge(available)
            }
            Button(
                onClick = onAdd,
                enabled = available && !alreadyAdded,
                colors = ButtonDefaults.buttonColors(containerColor = Navy)
            ) { Text(if (alreadyAdded) "Agregado" else "Agregar") }
        }
    }
}

/** Etiqueta "Disponible" o "Agotado" (RF-PRO-01). */
@Composable
private fun AvailabilityBadge(available: Boolean) {
    val color = if (available) Color(0xFF15803D) else Color(0xFF64748B)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(color.copy(alpha = .12f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Box(Modifier.size(6.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(if (available) "DISPONIBLE" else "AGOTADO", color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}