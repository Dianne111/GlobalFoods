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
    val references: String = ""
)

data class OrderObservations(
    val notes: String,
    val delivery: OrderDeliveryInfo? = null
) {
    /** Formato que lee la web actual; no agrega campos al contrato de la API. */
    fun encodeForWeb(date: Int): String {
        val info = requireNotNull(delivery) { "Falta la modalidad del pedido." }
        val mode = when (info.method) {
            "Dirección" -> "A domicilio"
            "Recolección" -> "Recolección en congeladora"
            else -> error("La modalidad del pedido no es válida.")
        }
        val digits = date.toString()
        require(digits.length == 8) { "La fecha del pedido no es válida." }
        val formattedDate = "${digits.substring(6, 8)}/${digits.substring(4, 6)}/${digits.substring(0, 4)}"
        return buildList {
            add(mode)
            add("Entrega $formattedDate")
            if (info.method == "Dirección") {
                if (info.address.isNotBlank()) add("Dir: ${info.address.trim()}")
                if (info.references.isNotBlank()) add("Ref: ${info.references.trim()}")
            }
            if (notes.isNotBlank()) add(notes.trim())
        }.joinToString(separator)
    }

    /** Se conserva para los clientes esporádicos; los registrados usan encodeForWeb. */
    fun encode(): String {
        val info = delivery ?: return notes
        val prefix = if (notes.isEmpty()) "" else "$notes\n"
        return prefix + deliveryMarker + json.encodeToString(OrderDeliveryInfo.serializer(), info)
    }

    companion object {
        private const val separator = " · "
        private val webDate = Regex("^Entrega (\\d{2})/(\\d{2})/(\\d{4})$")
        // Compatibilidad de lectura con pedidos móviles anteriores.
        private const val deliveryMarker = "[GlobalFoods:Entrega:v1]\n"
        private val json = Json { ignoreUnknownKeys = true }

        fun decode(value: String?): OrderObservations {
            val text = value.orEmpty()
            val markerIndex = text.lastIndexOf(deliveryMarker)
            if (markerIndex == -1 || (markerIndex > 0 && text[markerIndex - 1] != '\n')) {
                return decodeWeb(text) ?: OrderObservations(text)
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

        /**
         * Cambia solo el texto libre de Observaciones (lo que escribió el vendedor) y conserva lo que
         * la app y la web guardan antes: modalidad, fecha de entrega, dirección y referencias.
         */
        fun replaceNotes(value: String?, notes: String): String {
            val text = value.orEmpty()
            val clean = notes.trim()
            val markerIndex = text.lastIndexOf(deliveryMarker)
            val hasMarker = markerIndex == 0 || (markerIndex > 0 && text[markerIndex - 1] == '\n')
            if (hasMarker && decode(text).delivery != null) {
                val tail = text.substring(markerIndex)
                return if (clean.isEmpty()) tail else "$clean\n$tail"
            }
            val prefix = webPrefix(text) ?: return clean
            return (prefix + listOfNotNull(clean.ifEmpty { null })).joinToString(separator)
        }

        /** Partes iniciales del formato web (modalidad, fecha, Dir, Ref); null si no es ese formato. */
        private fun webPrefix(text: String): List<String>? {
            if (decodeWeb(text) == null) return null
            val parts = text.trim().split(separator)
            return parts.take(webPrefixSize(parts))
        }

        private fun webPrefixSize(parts: List<String>): Int {
            var index = 2
            if (parts.firstOrNull() != "A domicilio") return index
            var address = false
            var references = false
            while (index < parts.size) {
                val part = parts[index]
                when {
                    !address && part.startsWith("Dir: ") -> address = true
                    !references && part.startsWith("Ref: ") -> references = true
                    else -> break
                }
                index++
            }
            return index
        }

        private fun decodeWeb(text: String): OrderObservations? {
            val parts = text.trim().split(separator)
            val method = when (parts.firstOrNull()) {
                "A domicilio" -> "Dirección"
                "Recolección en congeladora" -> "Recolección"
                else -> return null
            }
            if (!webDate.matches(parts.getOrNull(1).orEmpty())) return null
            var address = ""
            var references = ""
            var index = 2
            while (method == "Dirección" && index < parts.size) {
                val part = parts[index]
                when {
                    address.isEmpty() && part.startsWith("Dir: ") -> address = part.removePrefix("Dir: ").trim()
                    references.isEmpty() && part.startsWith("Ref: ") -> references = part.removePrefix("Ref: ").trim()
                    else -> break
                }
                index++
            }
            return OrderObservations(
                notes = parts.drop(index).joinToString(separator).trim(),
                delivery = OrderDeliveryInfo("", "", method, address, references = references)
            )
        }
    }
}
