package com.globalfoods.project

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import globalfoods.sharedui.generated.resources.Res
import globalfoods.sharedui.generated.resources.global_foods_logo

private val PrimaryDark = Color(0xFF0C3B5E)
private val InputBackground = Color(0xFFF4F9FB)
private val InputBorder = Color(0xFF90C2D6)
private val TextLight = Color(0xFF7FA8BC)
private val BackgroundGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF165A84), Color(0xFF08263D))
)
private val Roles = listOf("Admin", "Empleado", "Cliente")

@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onRegisterClick: (String, String, String, String, String) -> Unit
) {
    var selectedRole by remember { mutableStateOf("Admin") }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var acceptedTerms by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGradient)
            .safeContentPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(Res.drawable.global_foods_logo),
                    contentDescription = "Global Foods Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(94.dp)
                        .padding(bottom = 14.dp)
                )

                RoleSelectionTabs(
                    selectedRole = selectedRole,
                    onRoleSelected = { selectedRole = it }
                )

                RegistrationForm(
                    name = name,
                    onNameChange = { name = it },
                    phone = phone,
                    onPhoneChange = { phone = it },
                    username = username,
                    onUsernameChange = { username = it },
                    password = password,
                    onPasswordChange = { password = it },
                    acceptedTerms = acceptedTerms,
                    onTermsChange = { acceptedTerms = it },
                    onRegister = {
                        onRegisterClick(selectedRole, name, phone, username, password)
                    },
                    onNavigateToLogin = onNavigateToLogin
                )
            }
        }
    }
}


@Composable
private fun RoleSelectionTabs(
    selectedRole: String,
    onRoleSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Roles.forEach { role ->
            val isSelected = selectedRole == role
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onRoleSelected(role) }
                    .padding(horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = role,
                    color = if (isSelected) PrimaryDark else TextLight,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    fontSize = 14.sp,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .height(2.dp)
                        .fillMaxWidth()
                        .background(
                            if (isSelected) PrimaryDark
                            else InputBorder.copy(alpha = 0.75f)
                        )
                )
            }
        }
    }
}

@Composable
private fun RegistrationForm(
    name: String,
    onNameChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    acceptedTerms: Boolean,
    onTermsChange: (Boolean) -> Unit,
    onRegister: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    Spacer(modifier = Modifier.height(14.dp))

    GlobalFoodsTextField(name, onNameChange, "Nombre", Icons.Outlined.Person)
    GlobalFoodsTextField(phone, onPhoneChange, "Celular", Icons.Outlined.Phone)
    GlobalFoodsTextField(username, onUsernameChange, "Usuario", Icons.Outlined.MailOutline)
    GlobalFoodsTextField(
        value = password,
        onValueChange = onPasswordChange,
        placeholder = "Contraseña",
        icon = Icons.Outlined.Lock,
        isPassword = true,
        modifier = Modifier.padding(bottom = 20.dp)
    )

    Button(
        onClick = onRegister,
        enabled = acceptedTerms,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(25.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PrimaryDark,
            disabledContainerColor = PrimaryDark.copy(alpha = 0.45f)
        )
    ) {
        Text("Registrar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }

    TermsCheckbox(checked = acceptedTerms, onCheckedChange = onTermsChange)

    Text(
        text = "¿Ya tienes una cuenta? Inicia sesión",
        color = TextLight,
        fontSize = 13.sp,
        textDecoration = TextDecoration.Underline,
        modifier = Modifier.clickable(onClick = onNavigateToLogin)
    )
}

@Composable
private fun TermsCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = PrimaryDark,
                uncheckedColor = PrimaryDark
            )
        )
        Text(
            text = "Acepto los Términos y Privacidad",
            color = PrimaryDark,
            fontSize = 13.sp,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable { onCheckedChange(!checked) }
        )
    }
}

@Composable
private fun GlobalFoodsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    modifier: Modifier = Modifier.padding(bottom = 12.dp),
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().then(modifier),
        placeholder = { Text(placeholder, color = TextLight) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = InputBorder,
            unfocusedBorderColor = InputBorder,
            focusedContainerColor = InputBackground,
            unfocusedContainerColor = InputBackground,
            focusedLeadingIconColor = PrimaryDark,
            unfocusedLeadingIconColor = PrimaryDark,
            focusedTextColor = PrimaryDark,
            unfocusedTextColor = PrimaryDark
        )
    )
}