package com.globalfoods.project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.globalfoods.project.sharedlogic.network.orders.OrderRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    order: Order,
    onNavigateBack: () -> Unit,
    onDeleted: () -> Unit
) {
    val repository = remember { OrderRepository() }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    Scaffold(
        containerColor = Color(0xFFF1F8FA),
        topBar = {
            TopAppBar(
                title = { Text(text = "Detalle del ${order.number}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF143D58))
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
                                text = order.status,
                                color = order.statusColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(order.statusColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Text(text = order.client, color = Color(0xFF143D58), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color(0xFFF1F8FA))
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "Peso Total:", color = Color(0xFF64748B), fontSize = 12.sp)
                                Text(text = order.totalKg, color = Color(0xFF143D58), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "Monto Total:", color = Color(0xFF64748B), fontSize = 12.sp)
                                Text(text = order.total, color = Color(0xFF0DB982), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            TextButton(
                                onClick = { showDeleteDialog = true },
                                enabled = !isDeleting,
                                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFB91C1C))
                            ) { Text("Eliminar pedido") }
                        }
                    }
                }
                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        title = { Text("Eliminar pedido") },
                        text = { Text("Esta acción eliminará físicamente el pedido y sus líneas. ¿Deseas continuar?") },
                        confirmButton = {
                            TextButton(onClick = {
                                isDeleting = true
                                kotlinx.coroutines.MainScope().launch {
                                    repository.deleteOrder(order.id).fold(
                                        onSuccess = { onDeleted() },
                                        onFailure = { isDeleting = false; error = it.message }
                                    )
                                }
                            }) { Text("Eliminar", color = Color(0xFFB91C1C)) }
                        },
                        dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") } }
                    )
                }
                error?.let {
                    AlertDialog(onDismissRequest = { error = null }, title = { Text("No se pudo eliminar") }, text = { Text(it) }, confirmButton = {
                        TextButton(onClick = { error = null }) { Text("Aceptar") }
                    })
                }
            }

            item {
                Text(
                    text = "PRODUCTOS (${order.products.size})",
                    color = Color(0xFF143D58),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Esta función "items()" soluciona tu error de la línea 164
            items(order.products) { product ->
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
        }
    }
}