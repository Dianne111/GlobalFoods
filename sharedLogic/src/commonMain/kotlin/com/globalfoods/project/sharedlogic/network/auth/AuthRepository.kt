package com.globalfoods.project.sharedlogic.network.auth

import com.globalfoods.project.sharedlogic.network.core.globalFoodsClient
import com.globalfoods.project.sharedlogic.network.core.sessionNombreUsuario
import com.globalfoods.project.sharedlogic.network.core.sessionToken
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class AuthRepository {
    private val loginUrl = "https://api.globalfoodsmexico.com/api/auth/login"

    suspend fun iniciarSesion(usuario: String, contrasena: String): Result<LoginResponse> {
        return try {
            val response = globalFoodsClient.post(loginUrl) {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(usuario = usuario, contrasena = contrasena))
            }

            if (response.status.isSuccess()) {
                val loginResponse: LoginResponse = response.body()
                sessionToken = loginResponse.accessToken
                sessionNombreUsuario = loginResponse.nombre

                Result.success(loginResponse)
            } else {
                // Si el servidor responde con error (401, 400, etc.), intentamos leer el JSON del error
                val mensajeError = try {
                    val errorBody: ErrorResponse = response.body()
                    errorBody.error ?: "Usuario no válido o contraseña incorrecta"
                } catch (e: Exception) {
                    "Usuario no válido o contraseña incorrecta" // Mensaje por defecto si no hay JSON
                }

                Result.failure(Exception(mensajeError))
            }
        }catch (e: Throwable) {
            e.printStackTrace()
            Result.failure(Exception("Error interno: ${e.message ?: e.toString()}"))
        }
    }

    fun cerrarSesion() {
        sessionToken = null
        sessionNombreUsuario = null
    }

    fun haySesion(): Boolean = sessionToken != null
}