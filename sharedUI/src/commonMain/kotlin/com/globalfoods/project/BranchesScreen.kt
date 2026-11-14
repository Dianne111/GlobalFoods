package com.globalfoods.project

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Restore
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
    // null = cerrado; Branch con Id 0 = sucursal nueva.
    var editing by remember { mutableStateOf<Branch?>(null) }
    var toDeactivate by remember { mutableStateOf<Branch?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(clientId, reloadKey) {
        if (clientId == null) return@LaunchedEffect
        isLoading = true
        loadError = null
        repository.getBranches(clientId).fold(
            onSuccess = { branches = sortBranches(it, clientId) },
            onFailure = { loadError = "No se pudieron cargar tus sucursales." }
        )
        isLoading = false
    }

    fun runAction(action: suspend () -> Result<Unit>) {
        isSaving = true
        scope.launch {
            action().fold(
                onSuccess = {
                    editing = null
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
            ScreenHeader(eyebrow = "MI CUENTA", title = "SUCURSALES") {
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { Spacer(Modifier.height(4.dp)) }
                if (clientId == null) {
                    item {
                        Notice(
                            "Tu usuario aún no está vinculado a un cliente. " +
                                "Comunícate con tu asesor de ventas para administrar tus sucursales."
                        )
                    }
                } else {
                    item {
                        PrimaryAction("+  Agregar sucursal") {
                            editing = Branch(id = 0, clientId = clientId, name = "")
                        }
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
                        Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Green)
                        }
                    }
                    clientId != null && branches.isEmpty() && loadError == null -> item {
                        Text(
                            "Aún no tienes sucursales registradas. Agrega la primera para recibir tus pedidos.",
                            color = Gray,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)
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
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
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
            onConfirm = { runAction { repository.setBranchActive(branch, active = false) } },
            onDismiss = { toDeactivate = null }
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

@Composable
internal fun BranchCard(
    branch: Branch,
    onEdit: () -> Unit,
    onDeactivate: () -> Unit,
    onReactivate: () -> Unit
) {
    val active = branch.status == 1
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
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

/** Confirmación de la baja lógica de una sucursal (se conserva y se puede reactivar). */
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
