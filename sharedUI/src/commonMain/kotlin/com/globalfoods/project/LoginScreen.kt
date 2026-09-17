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
import org.jetbrains.compose.resources.painterResource

import globalfoods.sharedui.generated.resources.Res
import globalfoods.sharedui.generated.resources.global_foods_logo


@Composable
fun LoginScreen() {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val primaryBlue = Color(0xFF006699)
    val darkBlue = Color(0xFF0A2E46)
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0A4D68), Color(0xFF6FA8DC))
    )

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
                        placeholder = { Text("Correo electrónico o Usuario") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = primaryBlue) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text("Contraseña") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = primaryBlue) },
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { /* Lógica de iniciar sesión */ },
                        colors = ButtonDefaults.buttonColors(containerColor = darkBlue),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text("Iniciar Sesión", color = Color.White, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(onClick = { /* Lógica para recuperar contraseña */ }) {
                        Text(
                            text = "Recuperar Contraseña",
                            color = primaryBlue,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    TextButton(onClick = { /* Lógica para registro */ }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "¿Nuevo usuario? ",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Regístrate aquí.",
                                color = darkBlue,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}