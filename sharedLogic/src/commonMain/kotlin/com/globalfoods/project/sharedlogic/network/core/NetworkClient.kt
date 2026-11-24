package com.globalfoods.project.sharedlogic.network.core

import io.ktor.client.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

// La sesión (token, rol, etc.) está en Session.kt.

/** Tiempo máximo para conectar con el servidor. */
const val CONNECT_TIMEOUT_MS = 10_000L
/** Tiempo máximo total de una petición; después se muestra un error en lugar de seguir cargando. */
const val REQUEST_TIMEOUT_MS = 20_000L

val globalFoodsClient = HttpClient {
    // Sin esto, si la red o el servidor no responden, la app se queda cargando sin fin.
    install(HttpTimeout) {
        connectTimeoutMillis = CONNECT_TIMEOUT_MS
        requestTimeoutMillis = REQUEST_TIMEOUT_MS
        socketTimeoutMillis = REQUEST_TIMEOUT_MS
    }

    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            prettyPrint = true
        })
    }

    defaultRequest {
        sessionToken?.let { token ->
            header("Authorization", listOf("Bearer", token).joinToString(" "))
        }
    }
}
