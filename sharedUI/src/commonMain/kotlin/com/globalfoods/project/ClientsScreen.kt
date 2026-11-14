package com.globalfoods.project

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.globalfoods.project.sharedlogic.network.clients.AdminClient
import com.globalfoods.project.sharedlogic.network.clients.ClientRepository
import kotlinx.coroutines.launch

internal enum class ClientsView(val label: String) { Active("Activos"), Inactive("Inactivos") }

/** Busca por nombre, usuario o teléfono (sin importar mayúsculas ni espacios en el teléfono). */
internal fun searchClients(clients: List<AdminClient>, query: String): List<AdminClient> {
    val text = query.trim()
    if (text.isEmpty()) return clients
    val digits = text.filter(Char::isDigit)
    return clients.filter { client ->
        client.name.contains(text, ignoreCase = true) ||
            client.user.contains(text, ignoreCase = true) ||
            (digits.length >= 3 && client.phone.orEmpty().filter(Char::isDigit).contains(digits))
    }
}

internal fun sortClients(clients: List<AdminClient>): List<AdminClient> =
    clients.sortedBy { it.name.trim().lowercase() }

@Composable
fun ClientsScreen(
    refreshKey: Int = 0,
    onClientSelected: (AdminClient) -> Unit,
    onTabSelected: (MainTab) -> Unit
) {
    val repository = remember { ClientRepository() }
    val scope = rememberCoroutineScope()
    var clients by remember { mutableStateOf<List<AdminClient>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var view by rememberSaveable { mutableStateOf(ClientsView.Active) }
    var creating by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(refreshKey, reloadKey) {
        isLoading = true
        loadError = null
        repository.getClients().fold(
            onSuccess = { clients = sortClients(it) },
            onFailure = { loadError = it.message }
        )
        isLoading = false
    }

    val searched = remember(clients, query) { searchClients(clients, query) }
    val visible = searched.filter { it.isActive == (view == ClientsView.Active) }

    Scaffold(
        containerColor = Background,
        topBar = {
            ScreenHeader(eyebrow = "CATÁLOGO", title = "CLIENTES") {
                HeaderIconAction(Icons.Outlined.Refresh, "Actualizar clientes", enabled = !isLoading) { reloadKey++ }
            }
        },
        bottomBar = { DashboardNavigation(MainTab.Clientes, onTabSelected) }
    ) { insets ->
        Box(Modifier.fillMaxSize().padding(insets)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 520.dp)
                    .align(Alignment.TopCenter)
                    .padding(horizontal = DashboardHorizontalPadding),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Spacer(Modifier.height(6.dp))
                    PrimaryAction("+  Nuevo cliente") { creating = true }
                }
                item { SearchField(query, { query = it }, placeholder = "Buscar cliente") }
                item {
                    SegmentedTabs(
                        options = ClientsView.entries,
                        selected = view,
                        label = { option -> "${option.label} · ${searched.count { it.isActive == (option == ClientsView.Active) }}" },
                        onSelected = { view = it }
                    )
                }
                loadError?.let { message ->
                    item {
                        Column {
                            Text(message, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                            TextButton(onClick = { reloadKey++ }) { Text("Reintentar") }
                        }
                    }
                }
                when {
                    isLoading && clients.isEmpty() -> item {
                        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Green)
                        }
                    }
                    visible.isEmpty() && loadError == null -> item {
                        Text(
                            when {
                                clients.isEmpty() -> "Todavía no hay clientes registrados."
                                query.isNotBlank() -> "No hay clientes que coincidan con la búsqueda."
                                else -> "No hay clientes ${view.label.lowercase()}."
                            },
                            color = Gray,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp)
                        )
                    }
                    else -> items(visible, key = { it.id }) { client ->
                        ClientCard(client, onClick = { onClientSelected(client) })
                    }
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }

    if (creating) {
        ClientDialog(
            client = null,
            saving = isSaving,
            onDismiss = { if (!isSaving) creating = false },
            onSave = { name, phone, user, password ->
                isSaving = true
                scope.launch {
                    repository.createClient(name, phone, user, password).fold(
                        onSuccess = {
                            creating = false
                            reloadKey++
                        },
                        onFailure = { saveError = it.message }
                    )
                    isSaving = false
                }
            }
        )
    }

    saveError?.let { message ->
        AlertDialog(
            onDismissRequest = { saveError = null },
            title = { Text("No se pudo guardar") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { saveError = null }) { Text("Aceptar") } }
        )
    }
}

@Composable
private fun ClientCard(client: AdminClient, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(client.name.ifBlank { "Sin nombre" }, color = Navy, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(
                    listOfNotNull(
                        client.user.takeIf { it.isNotBlank() }?.let { "@$it" },
                        client.phone?.takeIf { it.isNotBlank() }
                    ).joinToString("  ·  ").ifBlank { "Sin datos de contacto" },
                    color = Gray,
                    fontSize = 12.sp
                )
            }
            if (!client.isActive) {
                Text(
                    "INACTIVO",
                    color = Gray,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(Gray.copy(alpha = .12f), RoundedCornerShape(5.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
            Text("  ›", color = Navy, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Alta ([client] null) o edición de un cliente. Al editar, la contraseña es opcional:
 * si se deja vacía se conserva la actual. La baja y la reactivación se hacen desde el detalle.
 */
@Composable
internal fun ClientDialog(
    client: AdminClient?,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, user: String, password: String) -> Unit
) {
    var name by remember(client) { mutableStateOf(client?.name.orEmpty()) }
    var phone by remember(client) { mutableStateOf(client?.phone.orEmpty()) }
    var user by remember(client) { mutableStateOf(client?.user.orEmpty()) }
    var password by remember(client) { mutableStateOf("") }
    var showPassword by remember(client) { mutableStateOf(false) }
    val isNew = client == null
    val valid = name.isNotBlank() && user.isNotBlank() && (!isNew || password.length >= 4)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "Nuevo cliente" else "Editar cliente", color = Navy, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nombre o razón social") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    phone,
                    { value -> phone = value.filter { it.isDigit() || it in " +-()" }.take(20) },
                    label = { Text("Celular") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    user,
                    { value -> user = value.filterNot(Char::isWhitespace) },
                    label = { Text("Usuario para iniciar sesión") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    password,
                    { password = it },
                    label = { Text(if (isNew) "Contraseña (mínimo 4 caracteres)" else "Nueva contraseña (opcional)") },
                    supportingText = if (isNew) null else ({ Text("Déjala vacía para conservar la actual.") }),
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (showPassword) "Ocultar contraseña" else "Mostrar contraseña"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, phone, user, password) }, enabled = valid && !saving) {
                Text(if (saving) "Guardando…" else "Guardar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Cancelar") } }
    )
}
