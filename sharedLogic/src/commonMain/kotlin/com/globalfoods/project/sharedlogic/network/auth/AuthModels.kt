package com.globalfoods.project.sharedlogic.network.auth

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
    val nombre: String,
    /** Rol del usuario: Administrador, Empleado, Reporte o Cliente. */
    val tipoUsuario: String? = null,
    /**
     * Cliente al que pertenece el usuario (solo para el rol Cliente).
     * Pendiente en la API: se pidió que el login lo devuelva y que filtre por el del token.
     */
    val idCliente: Int? = null
)

// Nuevo modelo para leer el mensaje de error del backend
@Serializable
data class ErrorResponse(
    val error: String? = null
)