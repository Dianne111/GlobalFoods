package com.globalfoods.project

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.globalfoods.project.sharedlogic.network.clients.AdminClient
import com.globalfoods.project.sharedlogic.network.clients.ClientRepository
import com.globalfoods.project.sharedlogic.network.core.ApiConflictException
import com.globalfoods.project.sharedlogic.network.orders.Branch
import com.globalfoods.project.sharedlogic.network.orders.BranchRequest
import com.globalfoods.project.sharedlogic.network.orders.OrderRepository
import kotlinx.coroutines.launch

private const val VISIBLE_CLIENT_ORDERS = 5

@Composable
fun ClientDetailScreen(
    client: AdminClient,
    onBack: () -> Unit,
    onChanged: () -> Unit,
    /** Se dio de baja al cliente (baja lógica); se regresa a la lista. */
    onDeactivated: () -> Unit,
    onOrderSelected: (Order) -> Unit
) {
    val clientRepository = remember { ClientRepository() }
    val orderRepository = remember { OrderRepository() }
    val scope = rememberCoroutineScope()
    var current by remember(client.id) { mutableStateOf(client) }
    var branches by remember(client.id) { mutableStateOf<List<Branch>>(emptyList()) }
    var orders by remember(client.id) { mutableStateOf<List<Order>>(emptyList()) }
    var isLoading by remember(client.id) { mutableStateOf(true) }
    var loadError by remember(client.id) { mutableStateOf<String?>(null) }
    var reloadKey by remember(client.id) { mutableStateOf(0) }
    var showAllOrders by remember(client.id) { mutableStateOf(false) }

    var editingClient by remember { mutableStateOf(false) }
    var confirmDeactivate by remember { mutableStateOf(false) }
    /** Aviso (título y mensaje) de que la baja no se puede hacer por pedidos pendientes. */
    var blockedNotice by remember { mutableStateOf<Pair<String, String>?>(null) }
    val clientBlockedTitle = "No se puede dar de baja al cliente"
    val branchBlockedTitle = "No se puede dar de baja la sucursal"
    var editingBranch by remember { mutableStateOf<Branch?>(null) }
    var branchToDeactivate by remember { mutableStateOf<Branch?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(client.id, reloadKey) {
        isLoading = true
        loadError = null
        val problems = mutableListOf<String>()
        clientRepository.getClients().fold(
            onSuccess = { list -> list.firstOrNull { it.id == client.id }?.let { current = it } },
            onFailure = { problems += "los datos del cliente" }
        )
        orderRepository.getBranches(client.id).fold(
            onSuccess = { branches = sortBranches(it, client.id) },
            onFailure = { problems += "las sucursales" }
        )
        orderRepository.getOrders().fold(
            onSuccess = { all ->
                orders = sortOrders(all.filter { (it.clientId ?: it.client?.id) == client.id }.map { toOrder(it) })
            },
            onFailure = { problems += "los pedidos" }
        )
        if (problems.isNotEmpty()) loadError = "No se pudieron cargar ${problems.joinToString(", ")}."
        isLoading = false
    }

    fun runAction(onDone: () -> Unit = {}, action: suspend () -> Result<Unit>) {
        isSaving = true
        scope.launch {
            action().fold(
                onSuccess = {
                    onDone()
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
            ScreenHeader(eyebrow = "DETALLE DEL CLIENTE", title = current.name.ifBlank { "Cliente #${current.id}" }, onBack = onBack) {
                HeaderIconAction(Icons.Outlined.Refresh, "Actualizar", enabled = !isLoading) { reloadKey++ }
            }
        }
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
                if (isLoading) {
                    item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = Green) }
                }
                loadError?.let { message ->
                    item {
                        Column {
                            Text(message, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                            TextButton(onClick = { reloadKey++ }) { Text("Reintentar") }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            DetailRow("Usuario", current.user.ifBlank { null })
                            DetailRow("Celular", current.phone?.ifBlank { null })
                            DetailRow("Estado", if (current.isActive) "Activo" else "Inactivo")
                            if (!current.isActive) {
                                Text(
                                    "Este cliente está dado de baja: no puede iniciar sesión ni aparece al registrar pedidos.",
                                    color = Gray,
                                    fontSize = 12.sp
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { editingClient = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Navy),
                                    modifier = Modifier.weight(1f)
                                ) { Text("Editar datos") }
                                if (current.isActive) {
                                    TextButton(
                                        onClick = {
                                            val pending = pendingDeliveryOrders(orders.mapNotNull { it.response }, current.id)
                                            if (pending.isEmpty()) confirmDeactivate = true
                                            else blockedNotice = clientBlockedTitle to pendingOrdersMessage(current.name, pending)
                                        },
                                        enabled = !isSaving,
                                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFB91C1C))
                                    ) { Text("Dar de baja") }
                                } else {
                                    TextButton(
                                        onClick = {
                                            runAction(onDone = onChanged) { clientRepository.setActive(current, active = true) }
                                        },
                                        enabled = !isSaving,
                                        colors = ButtonDefaults.textButtonColors(contentColor = Green)
                                    ) { Text("Reactivar") }
                                }
                            }
                        }
                    }
                }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionTitle("SUCURSALES (${branches.size})", Modifier.weight(1f))
                        TextButton(onClick = { editingBranch = Branch(id = 0, clientId = current.id, name = "") }) {
                            Icon(Icons.Outlined.Add, contentDescription = null)
                            Text("Agregar")
                        }
                    }
                }
                if (branches.isEmpty() && !isLoading) {
                    item { EmptyText("Este cliente no tiene sucursales registradas.") }
                }
                items(branches, key = { "s${it.id}" }) { branch ->
                    BranchCard(
                        branch,
                        onEdit = { editingBranch = branch },
                        onDeactivate = {
                            val pending = pendingBranchOrders(orders.mapNotNull { it.response }, branch.id)
                            if (pending.isEmpty()) branchToDeactivate = branch
                            else blockedNotice = branchBlockedTitle to pendingBranchOrdersMessage(branch.name, pending)
                        },
                        onReactivate = { runAction { orderRepository.setBranchActive(branch, active = true) } }
                    )
                }

                item { SectionTitle("PEDIDOS (${orders.size})", Modifier.padding(top = 8.dp)) }
                if (orders.isEmpty() && !isLoading) {
                    item { EmptyText("Este cliente todavía no tiene pedidos.") }
                }
                val shownOrders = if (showAllOrders) orders else orders.take(VISIBLE_CLIENT_ORDERS)
                items(shownOrders, key = { "p${it.id}" }) { order ->
                    OrderCard(order, onClick = { onOrderSelected(order) })
                }
                if (orders.size > VISIBLE_CLIENT_ORDERS) {
                    item {
                        TextButton(onClick = { showAllOrders = !showAllOrders }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (showAllOrders) "Ver menos" else "Ver los ${orders.size} pedidos")
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }

    if (editingClient) {
        ClientDialog(
            client = current,
            saving = isSaving,
            onDismiss = { if (!isSaving) editingClient = false },
            onSave = { name, phone, user, password ->
                runAction(onDone = { editingClient = false; onChanged() }) {
                    clientRepository.updateClient(current, name, phone, user, password, active = current.isActive)
                }
            }
        )
    }

    if (confirmDeactivate) {
        AlertDialog(
            onDismissRequest = { if (!isSaving) confirmDeactivate = false },
            title = { Text("Dar de baja al cliente") },
            text = {
                Text(
                    "\"${current.name}\" ya no podrá iniciar sesión ni aparecerá al registrar pedidos. " +
                        "Sus datos, sucursales y pedidos se conservan, y podrás reactivarlo desde la pestaña Inactivos."
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !isSaving,
                    onClick = {
                        isSaving = true
                        scope.launch {
                            // La API revisa los pedidos pendientes; si entró uno nuevo responde 409.
                            clientRepository.setActive(current, active = false).fold(
                                onSuccess = { confirmDeactivate = false; onDeactivated() },
                                onFailure = { e ->
                                    confirmDeactivate = false
                                    if (e is ApiConflictException) {
                                        blockedNotice = clientBlockedTitle to clientInProcessMessage(current.name)
                                        reloadKey++
                                    } else {
                                        actionError = e.message
                                    }
                                }
                            )
                            isSaving = false
                        }
                    }
                ) { Text("Dar de baja", color = Color(0xFFB91C1C)) }
            },
            dismissButton = { TextButton(onClick = { confirmDeactivate = false }, enabled = !isSaving) { Text("Cancelar") } }
        )
    }

    blockedNotice?.let { (title, message) ->
        AlertDialog(
            onDismissRequest = { blockedNotice = null },
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { blockedNotice = null }) { Text("Aceptar") } }
        )
    }

    editingBranch?.let { branch ->
        AdminBranchDialog(
            branch = branch,
            otherActiveNames = branches.filter { it.status == 1 && it.id != branch.id }.map { it.name },
            saving = isSaving,
            onDismiss = { if (!isSaving) editingBranch = null },
            onSave = { name, address, phone ->
                val request = BranchRequest(
                    clientId = current.id,
                    name = name.trim(),
                    address = address.trim(),
                    phone = phone.trim(),
                    status = if (branch.id == 0) 1 else branch.status
                )
                runAction(onDone = { editingBranch = null }) {
                    if (branch.id == 0) orderRepository.createBranch(request)
                    else orderRepository.updateBranch(branch.id, request)
                }
            }
        )
    }

    branchToDeactivate?.let { branch ->
        BranchDeactivateDialog(
            branch = branch,
            saving = isSaving,
            onConfirm = {
                isSaving = true
                scope.launch {
                    deactivateBranchIfNoPendingOrders(orderRepository, branch).fold(
                        onSuccess = { branchToDeactivate = null },
                        onFailure = { e ->
                            branchToDeactivate = null
                            if (e is PendingOrdersException) blockedNotice = branchBlockedTitle to e.message.orEmpty()
                            else actionError = e.message
                        }
                    )
                    reloadKey++
                    isSaving = false
                }
            },
            onDismiss = { branchToDeactivate = null }
        )
    }

    actionError?.let { message ->
        AlertDialog(
            onDismissRequest = { actionError = null },
            title = { Text("No se pudo completar la acción") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { actionError = null }) { Text("Aceptar") } }
        )
    }
}

/**
 * Alta o edición de una sucursal desde la vista del vendedor/administrador, con las reglas de
 * RF-SUC-01/03. La pestaña Sucursales del cliente usa [BranchDialog], que no cambia.
 */
@Composable
private fun AdminBranchDialog(
    branch: Branch,
    otherActiveNames: List<String>,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, address: String, phone: String) -> Unit
) {
    var name by remember(branch) { mutableStateOf(branch.name) }
    var address by remember(branch) { mutableStateOf(branch.address) }
    var phone by remember(branch) { mutableStateOf(phoneInput(branch.phone)) }
    var submitted by remember(branch) { mutableStateOf(false) }
    val errors = validateBranchForm(name, address, phone, otherActiveNames)
    val shown = if (submitted) errors else BranchFormErrors()

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
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ValidatedTextField(
                    name,
                    { name = it.take(MAX_BRANCH_NAME) },
                    label = "Nombre de la sucursal",
                    error = shown.name,
                    modifier = Modifier.fillMaxWidth()
                )
                ValidatedTextField(
                    address,
                    { address = it },
                    label = "Dirección",
                    error = shown.address,
                    hint = "Calle y número, colonia y ciudad.",
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth()
                )
                ValidatedTextField(
                    phone,
                    { phone = phoneInput(it) },
                    label = "Teléfono (10 dígitos)",
                    error = shown.phone,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    submitted = true
                    if (errors.isValid) onSave(name, address, phone)
                },
                enabled = !saving
            ) {
                Text(if (saving) "Guardando…" else "Guardar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Cancelar") } }
    )
}

@Composable
private fun DetailRow(label: String, value: String?) {
    Column {
        Text(label, color = Gray, fontSize = 12.sp)
        Text(value ?: "Sin dato registrado", color = Navy, fontSize = 14.sp)
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, color = Navy, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = modifier)
}

@Composable
private fun EmptyText(text: String) {
    Text(text, color = Gray, fontSize = 13.sp, modifier = Modifier.padding(vertical = 4.dp))
}
