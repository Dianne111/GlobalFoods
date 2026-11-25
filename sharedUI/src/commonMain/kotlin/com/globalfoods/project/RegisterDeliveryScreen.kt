package com.globalfoods.project

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.globalfoods.project.sharedlogic.network.orders.MIXED_PAYMENT_PENDING_MESSAGE
import com.globalfoods.project.sharedlogic.network.orders.OrderObservations
import com.globalfoods.project.sharedlogic.network.orders.OrderRepository
import com.globalfoods.project.sharedlogic.network.orders.OrderResponse
import com.globalfoods.project.sharedlogic.network.orders.PaymentType
import com.globalfoods.project.sharedlogic.network.orders.amountInput
import com.globalfoods.project.sharedlogic.network.orders.checkDeliveryPayment
import com.globalfoods.project.sharedlogic.network.orders.remainingForCard
import com.globalfoods.project.sharedlogic.network.orders.sanitizeAmountInput
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/*
 * Registrar entrega (vendedor/administrador), según la maqueta de la etapa 2. Por ahora es solo la
 * entrega completa: se cobra el total en efectivo, con tarjeta o en ambos (pago mixto), y se puede
 * tomar la foto del comprobante. La foto todavía no se envía (la API aún no la recibe) y la entrega
 * parcial se agrega cuando la API defina cómo guardarla.
 */

private val DangerRed = Color(0xFFB91C1C)

@Composable
internal fun RegisterDeliveryScreen(
    order: Order,
    onBack: () -> Unit,
    onDelivered: (OrderResponse) -> Unit
) {
    val response = order.response
    val total = response?.total ?: 0.0
    val repository = remember { OrderRepository() }
    val scope = rememberCoroutineScope()

    // Se propone cobrar todo en efectivo; al cambiarlo, el resto pasa solo a tarjeta.
    var cash by remember(order.id) { mutableStateOf(amountInput(total)) }
    var card by remember(order.id) { mutableStateOf(amountInput(0.0)) }
    var triedToSave by remember(order.id) { mutableStateOf(false) }
    var saving by remember(order.id) { mutableStateOf(false) }
    var saveError by remember(order.id) { mutableStateOf<String?>(null) }
    // Foto del comprobante en JPEG; opcional mientras la API no tenga dónde guardarla.
    var receipt by remember(order.id) { mutableStateOf<ByteArray?>(null) }
    var receiptError by remember(order.id) { mutableStateOf<String?>(null) }
    // Solo el texto libre de Observaciones; la modalidad y la dirección que van antes no se tocan.
    val originalNotes = remember(order.id) { OrderObservations.decode(response?.notes).notes }
    var notes by remember(order.id) { mutableStateOf(originalNotes.take(MAX_ORDER_NOTES)) }
    val photoPicker = rememberReceiptPhotoPicker(
        onPhoto = { receipt = it; receiptError = null },
        onError = { receiptError = it }
    )

    val check = checkDeliveryPayment(total, cash, card)
    val type = check.payment?.type
    val mixedPending = type == PaymentType.Mixed

    PlatformBackHandler(enabled = !saving) { onBack() }

    Scaffold(
        containerColor = Background,
        topBar = {
            ScreenHeader(
                eyebrow = "REGISTRAR ENTREGA",
                title = "Pedido ${order.number}",
                onBack = { if (!saving) onBack() }
            )
        }
    ) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val sectionModifier = Modifier.fillMaxWidth().widthIn(max = 520.dp)

            DeliverySection(sectionModifier) {
                Text("Cliente", color = Gray, fontSize = 13.sp)
                Text(order.client, color = Navy, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                if (order.destination.isNotBlank()) {
                    Text("Entrega en: ${order.destination}", color = Gray, fontSize = 13.sp)
                }
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Background)
                Spacer(Modifier.height(12.dp))
                order.products.forEach { product ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Text(product.name, color = Navy, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Text("${formatOrderKg(product.quantity)} kg", color = Gray, fontSize = 14.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Total a cobrar", color = Navy, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(formatMoney(total), color = Navy, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            DeliverySection(sectionModifier) {
                Text("¿Cómo pagó el cliente?", color = Navy, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Escribe lo que recibiste en efectivo; lo que falte se pasa solo a tarjeta.",
                    color = Gray,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(12.dp))
                AmountField(
                    label = "Efectivo",
                    icon = Icons.Outlined.Payments,
                    value = cash,
                    enabled = !saving,
                    onValueChange = { text ->
                        cash = sanitizeAmountInput(text)
                        card = remainingForCard(total, cash)
                        saveError = null
                    }
                )
                Spacer(Modifier.height(10.dp))
                AmountField(
                    label = "Tarjeta / transferencia",
                    icon = Icons.Outlined.CreditCard,
                    value = card,
                    enabled = !saving,
                    onValueChange = { text ->
                        card = sanitizeAmountInput(text)
                        saveError = null
                    }
                )
                Spacer(Modifier.height(12.dp))
                when {
                    check.error != null && (triedToSave || cash.isNotEmpty() || card.isNotEmpty()) ->
                        StatusLine(Icons.Outlined.ErrorOutline, check.error!!, DangerRed)
                    type != null -> StatusLine(Icons.Outlined.CheckCircle, "Cuadra con el total · Pago: ${type.label}", Green)
                }
                if (mixedPending) {
                    Spacer(Modifier.height(8.dp))
                    Text(MIXED_PAYMENT_PENDING_MESSAGE, color = Gray, fontSize = 13.sp)
                }
            }

            DeliverySection(sectionModifier) {
                Text("Comprobante de entrega", color = Navy, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Foto del recibo firmado por el cliente.", color = Gray, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                val photo = receipt
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (photo != null) {
                        ReceiptPhotoPreview(
                            jpeg = photo,
                            modifier = Modifier.size(96.dp).clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = photoPicker::takePhoto, enabled = !saving) {
                                Text("Tomar otra", color = Navy)
                            }
                            TextButton(onClick = { receipt = null }, enabled = !saving) {
                                Text("Quitar", color = DangerRed)
                            }
                        }
                    } else {
                        PhotoButton("Tomar foto", Icons.Outlined.PhotoCamera, !saving, photoPicker::takePhoto)
                        Spacer(Modifier.width(10.dp))
                        PhotoButton("Galería", Icons.Outlined.PhotoLibrary, !saving, photoPicker::pickFromGallery)
                    }
                }
                receiptError?.let {
                    Spacer(Modifier.height(8.dp))
                    StatusLine(Icons.Outlined.ErrorOutline, it, DangerRed)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Por ahora es opcional: la foto se enviará cuando la API pueda recibirla.",
                    color = Gray,
                    fontSize = 12.sp
                )
            }

            DeliverySection(sectionModifier) {
                Text("Observaciones (opcional)", color = Navy, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Agrega o corrige las indicaciones del pedido.", color = Gray, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it.take(MAX_ORDER_NOTES) },
                    placeholder = { Text("Indicaciones para almacén o reparto (máximo $MAX_ORDER_NOTES caracteres)") },
                    supportingText = {
                        Text("${notes.length}/$MAX_ORDER_NOTES", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
                    },
                    minLines = 3,
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Text(
                "Al confirmar, el pedido quedará Entregado con la fecha de hoy. El pago ya no podrá cambiarse.",
                color = Gray,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = sectionModifier
            )
            saveError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp, modifier = sectionModifier)
            }
            Button(
                onClick = {
                    triedToSave = true
                    val payment = check.payment ?: return@Button
                    saving = true
                    saveError = null
                    val date = currentPaymentDate()
                    scope.launch {
                        try {
                            val result = withTimeoutOrNull(30_000L) {
                                // Solo se reescriben las observaciones si el vendedor las cambió.
                                val changedNotes = notes.trim().takeIf { it != originalNotes.trim() }
                                repository.registerDelivery(order.id, payment, date, changedNotes)
                            } ?: Result.failure(IllegalStateException(
                                "Se agotó el tiempo para confirmar el guardado. Actualiza el pedido antes de reintentar."
                            ))
                            result.fold(
                                onSuccess = onDelivered,
                                onFailure = { saveError = it.message ?: "No se pudo registrar la entrega." }
                            )
                        } finally {
                            saving = false
                        }
                    }
                },
                enabled = !saving && !mixedPending && response != null,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Navy, contentColor = Color.White),
                modifier = sectionModifier.height(52.dp)
            ) {
                Text(if (saving) "Guardando…" else "Registrar entrega", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DeliverySection(modifier: Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun AmountField(
    label: String,
    icon: ImageVector,
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = Navy) },
        prefix = { Text("$ ") },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun PhotoButton(label: String, icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Gray.copy(alpha = 0.5f)),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = Navy, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, color = Navy, fontSize = 13.sp)
        }
    }
}

@Composable
private fun StatusLine(icon: ImageVector, text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = color, fontSize = 13.sp)
    }
}
