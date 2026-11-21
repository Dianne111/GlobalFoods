package com.globalfoods.project.sharedlogic.network.orders

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

@Serializable
data class OrderDeliveryInfo(
    val clientName: String,
    val clientType: String,
    val method: String,
    val address: String,
    val phone: String = "",
    val branchName: String = "",
    val references: String = "",
    val shippingCost: Double = 0.0
)

data class OrderObservations(
    val notes: String,
    val delivery: OrderDeliveryInfo? = null
) {
    fun encode(): String {
        val info = delivery ?: return notes
        val prefix = if (notes.isEmpty()) "" else "$notes\n"
        return prefix + deliveryMarker + json.encodeToString(OrderDeliveryInfo.serializer(), info)
    }

    companion object {
        // Observaciones is the existing text field available for storing delivery details.
        private const val deliveryMarker = "[GlobalFoods:Entrega:v1]\n"
        private val json = Json { ignoreUnknownKeys = true }

        fun decode(value: String?): OrderObservations {
            val text = value.orEmpty()
            val markerIndex = text.lastIndexOf(deliveryMarker)
            if (markerIndex == -1 || (markerIndex > 0 && text[markerIndex - 1] != '\n')) {
                return OrderObservations(text)
            }

            return try {
                val payload = text.substring(markerIndex + deliveryMarker.length)
                OrderObservations(
                    notes = text.substring(0, markerIndex).removeSuffix("\n"),
                    delivery = json.decodeFromString(OrderDeliveryInfo.serializer(), payload)
                )
            } catch (_: SerializationException) {
                OrderObservations(text)
            }
        }
    }
}
