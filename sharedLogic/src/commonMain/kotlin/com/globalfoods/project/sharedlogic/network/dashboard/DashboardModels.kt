package com.globalfoods.project.sharedlogic.network.dashboard

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ArticuloInventario(
    @SerialName("codigoProducto") val codigoProducto: Int,
    val talla: String,
    val precio: Double,
    val master: Double,
    val cantidadKg: Double
)