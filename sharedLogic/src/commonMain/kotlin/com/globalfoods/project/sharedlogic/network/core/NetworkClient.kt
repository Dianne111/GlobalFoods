package com.globalfoods.project.sharedlogic.network.core

import com.globalfoods.project.sharedlogic.network.auth.UserRole
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

var sessionToken: String? = null
var sessionNombreUsuario: String? = null
var sessionRol: UserRole = UserRole.Administrator

val globalFoodsClient = HttpClient {
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
