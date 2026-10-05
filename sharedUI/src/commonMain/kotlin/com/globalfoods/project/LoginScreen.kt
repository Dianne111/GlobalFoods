package com.globalfoods.project

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import globalfoods.sharedui.generated.resources.Res
import globalfoods.sharedui.generated.resources.global_foods_logo

import com.globalfoods.project.sharedlogic.network.auth.AuthRepository

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit = {},
    onLoginSuccess: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Variables para controlar la alerta visual
    var showDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository() }

    val primaryBlue = Color(0xFF006699)
    val darkBlue = Color(0xFF0A2E46)
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0A4D68), Color(0xFF6FA8DC))
    )

    // Alerta que se mostrará si las credenciales fallan
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    text = "Aviso",
                    fontWeight = FontWeight.Bold,
                    color = darkBlue
                )
            },
            text = {
                Text(text = dialogMessage)
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Aceptar", color = primaryBlue, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color.White
        )
    }

    Box(
        modifier = Modifier.fillMaxSize().background(backgroundGradient),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "¡Bienvenido a su portal\nde pedidos frescos!",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Image(
                        painter = painterResource(Res.drawable.global_foods_logo),
                        contentDescription = "Logo Global Foods México",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .padding(bottom = 24.dp)
                    )

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        placeholder = { Text("Usuario") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = primaryBlue) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text("Contraseña") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = primaryBlue) },
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (username.isNotBlank() && password.isNotBlank()) {
                                isLoading = true
                                coroutineScope.launch {
                                    val resultado = authRepository.iniciarSesion(username, password)
                                    isLoading = false

                                    resultado.fold(
                                        onSuccess = {
                                            onLoginSuccess() // Si es exitoso, avisa a App.kt para navegar
                                        },
                                        onFailure = { error ->
                                            // Si falla, lanza la alerta
                                            dialogMessage = error.message ?: "Error desconocido"
                                            showDialog = true
                                        }
                                    )
                                }
                            } else {
                                dialogMessage = "Por favor, ingresa tu usuario y contraseña."
                                showDialog = true
                            }
                        },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = darkBlue),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Iniciar Sesión", color = Color.White, fontSize = 16.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(onClick = { /* ... */ }) {
                        Text("Recuperar Contraseña", color = primaryBlue, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}