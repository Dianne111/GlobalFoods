package com.globalfoods.project.sharedlogic.network.cortes

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OrdenCorte(
    @SerialName("Id") val id: Int,
    @SerialName("Monto") val monto: Double,
    @SerialName("TotalPedidos") val totalPedidos: Int,
    @SerialName("Estatus") val estatus: String,
    @SerialName("Codigo") val codigo: String? = null, // Nulo cuando consulta el empleado
    @SerialName("IdAdmin") val idAdmin: Int,
    @SerialName("FechaSolicitud") val fechaSolicitud: String,
    @SerialName("FechaConfirmacion") val fechaConfirmacion: String? = null
)

@Serializable
data class CortePendienteResponse(
    @SerialName("orden") val orden: OrdenCorte?
)

@Serializable
data class ConfirmarCorteRequest(
    @SerialName("codigo") val codigo: String
)