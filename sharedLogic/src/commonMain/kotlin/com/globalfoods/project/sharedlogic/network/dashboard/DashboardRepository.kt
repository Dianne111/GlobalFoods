package com.globalfoods.project.sharedlogic.network.dashboard

import com.globalfoods.project.sharedlogic.network.core.errorMessageFor
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
                Result.failure(Exception(errorMessageFor(response.status.value, "el catálogo de productos")))
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.failure(Exception("No se pudo conectar con el servidor para cargar el catálogo.", e))
        }
    }
}