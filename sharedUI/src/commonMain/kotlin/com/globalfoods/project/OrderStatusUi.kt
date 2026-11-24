package com.globalfoods.project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.globalfoods.project.sharedlogic.network.orders.OrderStatus

private val UnknownStatusColor = Color(0xFF64748B)
private val PendingStepColor = Color(0xFFCBD5E1)
private val CurrentStepSize = 14.dp

internal val OrderStatus.color: Color
    get() = when (this) {
        OrderStatus.Created -> Color(0xFFFFA000)
        OrderStatus.Supplied -> Color(0xFF367CF4)
        OrderStatus.InDelivery -> Color(0xFF7026DF)
        OrderStatus.Delivered -> Color(0xFF0DB982)
        OrderStatus.Cancelled -> Color(0xFFB91C1C)
    }

internal fun statusLabel(code: Int): String =
    OrderStatus.fromCode(code)?.label?.uppercase() ?: "ESTATUS $code"

internal fun statusColor(code: Int): Color =
    OrderStatus.fromCode(code)?.color ?: UnknownStatusColor

/** Línea de avance Creado → Surtido → En reparto → Entregado, o aviso si el pedido se canceló. */
@Composable
internal fun OrderStatusProgress(status: OrderStatus?, modifier: Modifier = Modifier) {
    if (status == OrderStatus.Cancelled) {
        Text(
            "Este pedido fue cancelado.",
            color = status.color,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = modifier
                .fillMaxWidth()
                .background(status.color.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
                .padding(12.dp)
        )
        return
    }
    val currentIndex = OrderStatus.flow.indexOf(status)
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        OrderStatus.flow.forEachIndexed { index, step ->
            val reached = currentIndex >= 0 && index <= currentIndex
            val color = if (reached) step.color else PendingStepColor
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                // Altura fija = punto más grande, para que la línea quede al mismo nivel en todas las etapas.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().height(CurrentStepSize)
                ) {
                    StepLine(visible = index > 0, reached = reached, color = color, modifier = Modifier.weight(1f))
                    Box(
                        Modifier
                            .size(if (index == currentIndex) CurrentStepSize else 10.dp)
                            .background(color, CircleShape)
                    )
                    val nextReached = currentIndex >= 0 && index + 1 <= currentIndex
                    StepLine(
                        visible = index < OrderStatus.flow.lastIndex,
                        reached = nextReached,
                        color = if (nextReached) OrderStatus.flow[index + 1].color else PendingStepColor,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    step.label,
                    color = if (reached) Navy else Gray,
                    fontSize = 11.sp,
                    fontWeight = if (index == currentIndex) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun StepLine(visible: Boolean, reached: Boolean, color: Color, modifier: Modifier) {
    Box(
        modifier
            .height(2.dp)
            .background(if (visible) (if (reached) color else PendingStepColor) else Color.Transparent)
    )
}
