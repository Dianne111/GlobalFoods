package com.globalfoods.project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.globalfoods.project.sharedlogic.network.core.sessionIdCliente
import com.globalfoods.project.sharedlogic.network.core.sessionNombreUsuario
import com.globalfoods.project.sharedlogic.network.core.sessionTelefono // <--- Importamos el teléfono de sesión
import com.globalfoods.project.sharedlogic.network.orders.Branch
import com.globalfoods.project.sharedlogic.network.orders.BranchRequest
import com.globalfoods.project.sharedlogic.network.orders.OrderRepository
import kotlinx.coroutines.launch

/** Sucursales del cliente en sesión: activas primero y luego por nombre. */
internal fun sortBranches(branches: List<Branch>, clientId: Int?): List<Branch> =
    branches
        .filter { clientId != null && it.clientId == clientId }
        .sortedWith(compareByDescending<Branch> { it.status == 1 }.thenBy { it.name.lowercase() })

@Composable
fun BranchesScreen(onTabSelected: (MainTab) -> Unit) {
    val repository = remember { OrderRepository() }
    val scope = rememberCoroutineScope()
    val clientId = sessionIdCliente
    var branches by remember { mutableStateOf<List<Branch>>(emptyList()) }
    var isLoading by remember { mutableStateOf(clientId != null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }

    val rawName = try { sessionNombreUsuario.toString() } catch (e: Exception) { "" }
    val realUserName = if (rawName.isBlank() || rawName == "null") "Victoria Adahi" else rawName

    // --- TELÉFONO OBTENIDO DE LA SESIÓN DE LA API ---
    val rawPhone = try { sessionTelefono.toString() } catch (e: Exception) { "" }
    val initialPhone = if (rawPhone.isBlank() || rawPhone == "null") "No registrado" else rawPhone
    var userPhone by remember { mutableStateOf(initialPhone) }

    var telegramUser by remember { mutableStateOf("") }

    // Estados para controlar los diálogos de edición
    var editing by remember { mutableStateOf<Branch?>(null) }
    var isEditingPhone by remember { mutableStateOf(false) }
    var toDeactivate by remember { mutableStateOf<Branch?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var blockedMessage by remember { mutableStateOf<String?>(null) }

    val initials = if (realUserName.isNotBlank()) {
        realUserName.split(" ")
            .filter { it.isNotBlank() }
            .mapNotNull { it.firstOrNull()?.toString() }
            .take(2)
            .joinToString("")
            .uppercase()
    } else {
        "VA"
    }

    LaunchedEffect(clientId, reloadKey) {
        if (clientId == null) return@LaunchedEffect
        isLoading = true
        loadError = null
        repository.getBranches(clientId).fold(
            onSuccess = { branches = sortBranches(it, clientId) },
            onFailure = { loadError = it.message ?: "No se pudieron cargar tus sucursales." }
        )
        isLoading = false
    }

    fun runAction(action: suspend () -> Result<Unit>) {
        isSaving = true
        scope.launch {
            action().fold(
                onSuccess = {
                    editing = null
                    isEditingPhone = false
                    toDeactivate = null
                    reloadKey++
                },
                onFailure = { actionError = it.message }
            )
            isSaving = false
        }
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            ScreenHeader(eyebrow = "MI CUENTA", title = "MI PERFIL") {
                HeaderIconAction(Icons.Outlined.Refresh, "Actualizar sucursales", enabled = !isLoading) { reloadKey++ }
            }
        },
        bottomBar = { DashboardNavigation(MainTab.Sucursales, onTabSelected) }
    ) { insets ->
        Box(Modifier.fillMaxSize().padding(insets)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 520.dp)
                    .align(Alignment.TopCenter)
                    .padding(horizontal = DashboardHorizontalPadding),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { Spacer(Modifier.height(8.dp)) }

                // --- SECCIÓN: ENCABEZADO DE PERFIL ---
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .background(Color(0xFF00838F), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = realUserName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Navy
                        )

                        Text(
                            text = "Cliente",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF00C853),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // --- SECCIÓN: MIS DATOS ---
                item {
                    Column {
                        Text(
                            text = "MIS DATOS",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Navy
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                DataFieldItem(label = "Nombre", value = realUserName, showEdit = false)
                                DataFieldItem(
                                    label = "Teléfono",
                                    value = userPhone,
                                    showEdit = true,
                                    onEditClick = { isEditingPhone = true }
                                )
                                DataFieldItem(
                                    label = "Usuario Telegram",
                                    value = telegramUser.ifEmpty { "--------" },
                                    showEdit = true,
                                    onEditClick = {}
                                )
                            }
                        }
                    }
                }

                // --- SECCIÓN: MIS SUCURSALES ---
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "MIS SUCURSALES",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Navy
                        )

                        if (clientId != null) {
                            Button(
                                onClick = { editing = Branch(id = 0, clientId = clientId, name = "") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Agregar",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Agregar", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    }
                }

                if (clientId == null) {
                    item {
                        Notice(
                            "Tu usuario aún no está vinculado a un cliente. " +
                                    "Comunícate con tu asesor de ventas para administrar tus sucursales."
                        )
                    }
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
                    isLoading && branches.isEmpty() -> item {
                        Box(Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Green)
                        }
                    }
                    clientId != null && branches.isEmpty() && loadError == null -> item {
                        Text(
                            "Aún no tienes sucursales registradas. Agrega la primera para recibir tus pedidos.",
                            color = Gray,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                        )
                    }
                    else -> items(branches, key = { it.id }) { branch ->
                        BranchCard(
                            branch,
                            onEdit = { editing = branch },
                            onDeactivate = { toDeactivate = branch },
                            onReactivate = { runAction { repository.setBranchActive(branch, active = true) } }
                        )
                    }
                }

                // --- BOTÓN DE CERRAR SESIÓN ---
                item {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { /* Lógica de cerrar sesión */ },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Cerrar sesión",
                            tint = Color(0xFFFF5252)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Cerrar sesión", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }

    // --- DIÁLOGO PARA EDITAR TELÉFONO ---
    if (isEditingPhone) {
        PhoneEditDialog(
            currentPhone = userPhone,
            saving = isSaving,
            onDismiss = { if (!isSaving) isEditingPhone = false },
            onSave = { newPhone ->
                isSaving = true
                scope.launch {
                    userPhone = newPhone
                    // Opcional: si tienes un método en tu repositorio para actualizar el teléfono en backend,
                    // puedes llamarlo aquí.
                    isEditingPhone = false
                    isSaving = false
                }
            }
        )
    }

    editing?.let { branch ->
        BranchDialog(
            branch = branch,
            saving = isSaving,
            onDismiss = { if (!isSaving) editing = null },
            onSave = { name, address, phone ->
                val request = BranchRequest(
                    clientId = branch.clientId,
                    name = name.trim(),
                    address = address.trim(),
                    phone = phone.trim(),
                    status = if (branch.id == 0) 1 else branch.status
                )
                runAction {
                    if (branch.id == 0) repository.createBranch(request)
                    else repository.updateBranch(branch.id, request)
                }
            }
        )
    }

    toDeactivate?.let { branch ->
        BranchDeactivateDialog(
            branch = branch,
            saving = isSaving,
            onConfirm = {
                isSaving = true
                scope.launch {
                    deactivateBranchIfNoPendingOrders(repository, branch).fold(
                        onSuccess = { toDeactivate = null },
                        onFailure = { e ->
                            toDeactivate = null
                            if (e is PendingOrdersException) blockedMessage = e.message else actionError = e.message
                        }
                    )
                    reloadKey++
                    isSaving = false
                }
            },
            onDismiss = { toDeactivate = null }
        )
    }

    blockedMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { blockedMessage = null },
            title = { Text("No se puede dar de baja la sucursal") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { blockedMessage = null }) { Text("Aceptar") } }
        )
    }

    actionError?.let { message ->
        AlertDialog(
            onDismissRequest = { actionError = null },
            title = { Text("No se pudo guardar") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { actionError = null }) { Text("Aceptar") } }
        )
    }
}

// --- COMPONENTE DE DIÁLOGO PARA EDITAR EL TELÉFONO ---
@Composable
internal fun PhoneEditDialog(
    currentPhone: String,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var phone by remember { mutableStateOf(currentPhone) }
    val valid = phone.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar teléfono", color = Navy, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { value -> phone = value.filter { it.isDigit() || it in " +-()" }.take(20) },
                    label = { Text("Número de teléfono") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(phone) }, enabled = valid && !saving) {
                Text(if (saving) "Guardando…" else "Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
internal fun DataFieldItem(
    label: String,
    value: String,
    showEdit: Boolean,
    onEditClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = Gray)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = Navy)
        }

        if (showEdit) {
            OutlinedButton(
                onClick = onEditClick,
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFE8F5E9))
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Editar",
                    tint = Color(0xFF00C853),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Editar", color = Color(0xFF00C853), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
internal fun BranchCard(
    branch: Branch,
    onEdit: () -> Unit,
    onDeactivate: () -> Unit,
    onReactivate: () -> Unit,
    editable: Boolean = true
) {
    val active = branch.status == 1
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFFE0F2F1), shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Business,
                    contentDescription = null,
                    tint = Color(0xFF00796B),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(branch.name, color = Navy, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f, fill = false))
                    if (!active) {
                        Text(
                            "INACTIVA",
                            color = Gray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .background(Gray.copy(alpha = .12f), RoundedCornerShape(5.dp))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(branch.address.ifBlank { "Sin dirección registrada" }, color = Gray, fontSize = 13.sp)
                if (branch.phone.isNotBlank()) {
                    Text("Tel. ${branch.phone}", color = Gray, fontSize = 12.sp)
                }
            }

            if (editable) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, "Editar sucursal", tint = Gray) }
                    if (active) {
                        IconButton(onClick = onDeactivate) {
                            Icon(Icons.Outlined.Delete, "Dar de baja la sucursal", tint = Color(0xFFB91C1C))
                        }
                    } else {
                        IconButton(onClick = onReactivate) {
                            Icon(Icons.Outlined.Restore, "Reactivar sucursal", tint = Green)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun BranchDialog(
    branch: Branch,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, address: String, phone: String) -> Unit
) {
    var name by remember(branch) { mutableStateOf(branch.name) }
    var address by remember(branch) { mutableStateOf(branch.address) }
    var phone by remember(branch) { mutableStateOf(branch.phone) }
    val valid = name.isNotBlank() && address.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (branch.id == 0) "Nueva sucursal" else "Editar sucursal",
                color = Navy,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nombre de la sucursal") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(address, { address = it }, label = { Text("Dirección") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    phone,
                    { value -> phone = value.filter { it.isDigit() || it in " +-()" }.take(20) },
                    label = { Text("Teléfono (opcional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, address, phone) }, enabled = valid && !saving) {
                Text(if (saving) "Guardando…" else "Guardar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Cancelar") } }
    )
}

@Composable
internal fun BranchDeactivateDialog(
    branch: Branch,
    saving: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Dar de baja la sucursal") },
        text = {
            Text(
                "\"${branch.name}\" ya no podrá elegirse en pedidos nuevos. " +
                        "Los pedidos anteriores se conservan y podrás reactivarla cuando quieras."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !saving) {
                Text("Dar de baja", color = Color(0xFFB91C1C))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Cancelar") } }
    )
}