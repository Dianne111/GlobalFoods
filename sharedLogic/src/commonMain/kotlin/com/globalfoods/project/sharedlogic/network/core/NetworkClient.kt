package com.globalfoods.project.sharedlogic.network

import io.ktor.client.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
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

    install(Auth) {
        bearer {
            loadTokens {
                // Aquí usamos el sessionToken (que será el accessToken de la API)
                sessionToken?.let { BearerTokens(it, "") }
            }
        }
    }
}