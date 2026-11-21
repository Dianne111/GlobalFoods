package com.globalfoods.project

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
internal fun RegisterPaymentDialog(order: Order, onDismissRequest: () -> Unit) {
    var selectedMethod by remember(order.id) { mutableStateOf("Efectivo") }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = Background
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Registrar pago", color = Navy, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("Pedido ${order.number} · Total ${order.total}", color = Gray, fontSize = 14.sp, textAlign = TextAlign.Center)
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Método de pago", color = Gray, fontSize = 14.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth().selectableGroup(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf("Efectivo", "Tarjeta").forEach { method ->
                            val isSelected = selectedMethod == method
                            OutlinedButton(
                                onClick = { selectedMethod = method },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp).semantics { selected = isSelected },
                                shape = RoundedCornerShape(24.dp),
                                border = BorderStroke(1.dp, if (isSelected) Navy else Gray.copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) Navy else Color.Transparent,
                                    contentColor = if (isSelected) Color.White else Navy
                                )
                            ) {
                                Text(method, modifier = Modifier.weight(1f), fontSize = 14.sp, textAlign = TextAlign.Center)
                                if (isSelected) {
                                    Spacer(Modifier.width(6.dp))
                                    Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = onDismissRequest) { Text("Cancelar", color = Gray) }
                    Button(
                        onClick = {},
                        // Solo interfaz: el guardado se integrará en una tarea posterior.
                        enabled = false,
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Confirmar pago")
                    }
                }
            }
        }
    }
}
