package com.globalfoods.project.sharedlogic.network.dashboard

import com.globalfoods.project.sharedlogic.network.core.globalFoodsClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class DashboardRepository {
    private val inventarioUrl = "https://api.globalfoodsmexico.com/api/inventario/actual"

    suspend fun obtenerInventario(): Result<List<ArticuloInventario>> {
        return try {
            val response = globalFoodsClient.get(inventarioUrl)

            if (response.status.isSuccess()) {
                val inventario: List<ArticuloInventario> = response.body()
                Result.success(inventario)
            } else {
                Result.failure(Exception("Error del servidor: ${response.status.value}"))
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            // CAMBIO AQUÍ: Ahora la pantalla mostrará el error técnico real
            Result.failure(Exception("Fallo técnico: ${e.message}"))
        }
    }
}