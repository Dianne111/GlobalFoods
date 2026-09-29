package com.globalfoods.project.sharedlogic.network

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val usuario: String,
    val contrasena: String
)

@Serializable
data class LoginResponse(
    val tokenType: String? = null,
    val accessToken: String,
    val expiresIn: Int? = null,
    val nombre: String
)

// Nuevo modelo para leer el mensaje de error del backend
@Serializable
data class ErrorResponse(
    val error: String? = null
)