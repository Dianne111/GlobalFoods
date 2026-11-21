package com.globalfoods.project

import com.globalfoods.project.sharedlogic.network.orders.OrderObservations
import com.globalfoods.project.sharedlogic.network.orders.OrderResponse
import kotlin.math.round

/** Los pedidos registrados de la app envían una sucursal solo cuando hay envío. */
internal fun OrderResponse.isPickupDelivery(): Boolean {
    val legacyMethod = OrderObservations.decode(notes).delivery?.method?.takeIf { it.isNotBlank() }
    if (legacyMethod != null) return legacyMethod == "Recolección"
    return (clientId ?: client?.id ?: 0) > 0 && (branchId == null || branchId == 0)
}

/** El total enviado por la app incluye productos y envío. Conserva el importe al editar. */
internal fun OrderResponse.deliveryShippingCost(): Double? {
    OrderObservations.decode(notes).delivery?.let { return it.shippingCost }
    if ((clientId ?: client?.id ?: 0) <= 0 || lines.isEmpty()) return null
    return round((total - lines.sumOf { it.subtotal }).coerceAtLeast(0.0) * 100) / 100
}
