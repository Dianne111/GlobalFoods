package com.globalfoods.project.sharedlogic.network.dashboard

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ArticuloInventario(
    val codigoProducto: Int,
    val talla: String,
    val precio: Double,
    val master: Double = 0.0,
    val cantidadKg: Double = 0.0
)