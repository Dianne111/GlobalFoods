package com.globalfoods.project.sharedlogic.network.dashboard

import kotlinx.serialization.Serializable

@Serializable
data class ArticuloInventario(
    val talla: String,
    val precio: Double,
    val master: Double,
    val cantidadKg: Double
)