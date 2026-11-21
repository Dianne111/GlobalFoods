package com.globalfoods.project.sharedlogic.network.core

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

// El equivalente a tu localStorage
var sessionToken: String? = null
var sessionNombreUsuario: String? = null

val globalFoodsClient = HttpClient {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true // Ignora campos extra si el backend los manda
            prettyPrint = true
        })
    }

    // MÉTODO INFALIBLE: Inyectamos el header manualmente (igual que en JavaScript)
    defaultRequest {
        if (sessionToken != null) {
            header("Authorization", "Bearer $sessionToken")
        }
    }
}