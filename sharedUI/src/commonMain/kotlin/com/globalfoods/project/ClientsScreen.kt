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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.globalfoods.project.sharedlogic.network.core.ApiConflictException
import com.globalfoods.project.sharedlogic.network.orders.Branch
import com.globalfoods.project.sharedlogic.network.orders.OrderRepository
import com.globalfoods.project.sharedlogic.network.orders.OrderResponse
import com.globalfoods.project.sharedlogic.network.orders.orderStatus
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

/**
 * Pedidos del cliente [clientId] que todavía no se entregan (Creado, Surtido o En reparto).
 * Mientras tenga alguno no se le puede dar de baja.
 */
internal fun pendingDeliveryOrders(orders: List<OrderResponse>, clientId: Int): List<OrderResponse> =
    orders.filter { (it.clientId ?: it.client?.id) == clientId && it.orderStatus?.isInProcess == true }

/** Pedidos dirigidos a la sucursal [branchId] que todavía no se entregan; impiden darla de baja. */
internal fun pendingBranchOrders(orders: List<OrderResponse>, branchId: Int): List<OrderResponse> =
    orders.filter { it.branchId == branchId && it.orderStatus?.isInProcess == true }

/**
 * "tiene 2 pedidos en proceso (#10, #11). Podrá darse de baja cuando esos pedidos se entreguen
 * o se cancelen.", con hasta 5 números. Es el aviso del flujo A1 de CU-18 y CU-22: solo informa,
 * no pide al usuario entregar ni cancelar (el cliente no puede hacerlo, CU-06 y CU-07).
 */
private fun pendingOrdersSummary(pending: List<OrderResponse>): String {
    val count = pending.size
    val numbers = pending.sortedBy { it.id }.take(5).joinToString(", ") { "#${it.id}" } +
        if (count > 5) " y ${count - 5} más" else ""
    val orders = if (count == 1) "1 pedido en proceso" else "$count pedidos en proceso"
    val those = if (count == 1) "ese pedido se entregue o se cancele" else "esos pedidos se entreguen o se cancelen"
    return "tiene $orders ($numbers). Podrá darse de baja cuando $those."
}

/** Texto del aviso que impide dar de baja a un cliente con pedidos en proceso (CU-18, A1). */
internal fun pendingOrdersMessage(clientName: String, pending: List<OrderResponse>): String =
    "\"${clientName.ifBlank { "Este cliente" }}\" ${pendingOrdersSummary(pending)}"

/** Texto del aviso que impide dar de baja a una sucursal con pedidos en proceso (CU-22, A1). */
internal fun pendingBranchOrdersMessage(branchName: String, pending: List<OrderResponse>): String =
    "La sucursal \"${branchName.ifBlank { "sin nombre" }}\" ${pendingOrdersSummary(pending)}"

private const val IN_PROCESS_SUFFIX = "tiene pedidos en proceso. Podrá darse de baja cuando esos pedidos se entreguen o se cancelen."

/**
 * Aviso cuando la API rechaza la baja del cliente (409) sin decir qué pedidos son, por ejemplo
 * porque entró un pedido después de que la pantalla cargó la lista.
 */
internal fun clientInProcessMessage(clientName: String): String =
    "\"${clientName.ifBlank { "Este cliente" }}\" $IN_PROCESS_SUFFIX"

/** Igual que [clientInProcessMessage], para la baja de una sucursal. */
internal fun branchInProcessMessage(branchName: String): String =
    "La sucursal \"${branchName.ifBlank { "sin nombre" }}\" $IN_PROCESS_SUFFIX"

/** La baja no se hizo porque hay pedidos pendientes; su mensaje es el texto del aviso. */
internal class PendingOrdersException(message: String) : Exception(message)

/**
 * Da de baja la sucursal. La API revisa los pedidos pendientes y, si los hay, responde 409:
 * en ese caso falla con [PendingOrdersException] y el texto del aviso (CU-22, A1).
 */
internal suspend fun deactivateBranchIfNoPendingOrders(repository: OrderRepository, branch: Branch): Result<Unit> =
    repository.setBranchActive(branch, active = false).recoverCatching { error ->
        throw if (error is ApiConflictException) PendingOrdersException(branchInProcessMessage(branch.name)) else error
    }

@Composable
fun ClientsScreen(
    refreshKey: Int = 0,
    onClientSelected: (AdminClient) -> Unit,
    onTabSelected: (MainTab) -> Unit,
    /** Lo guarda App para conservar la posición al volver del detalle de un cliente. */
    listState: LazyListState = rememberLazyListState(),
    /** Últimos clientes cargados: se muestran al volver mientras se actualizan. */
    cache: ListsCache = remember { ListsCache() }
) {
    val repository = remember { ClientRepository() }
    val scope = rememberCoroutineScope()
    var clients by remember { mutableStateOf(cache.clients.orEmpty()) }
    var isLoading by remember { mutableStateOf(cache.clients == null) }
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
            onSuccess = { loaded -> clients = sortClients(loaded).also { cache.clients = it } },
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
        Column(Modifier.fillMaxSize().padding(insets)) {
            // Alta, búsqueda y pestañas fijas; solo la lista se desplaza.
            PinnedTop(listState) {
                PrimaryAction("+  Nuevo cliente") { creating = true }
                SearchField(query, { query = it }, placeholder = "Buscar cliente")
                SegmentedTabs(
                    options = ClientsView.entries,
                    selected = view,
                    label = { option -> "${option.label} · ${searched.count { it.isActive == (option == ClientsView.Active) }}" },
                    onSelected = { view = it }
                )
            }
            Box(Modifier.fillMaxWidth().weight(1f)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 520.dp)
                        .align(Alignment.TopCenter)
                        .padding(horizontal = DashboardHorizontalPadding),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item { Spacer(Modifier.height(2.dp)) }
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
 *
 * Valida con las reglas de RF-CLI-02/03. Los errores se marcan en rojo bajo cada campo después
 * del primer intento de guardar, y desaparecen en cuanto el dato se corrige.
 */
@Composable
internal fun ClientDialog(
    client: AdminClient?,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, user: String, password: String) -> Unit
) {
    var name by remember(client) { mutableStateOf(client?.name.orEmpty()) }
    var phone by remember(client) { mutableStateOf(phoneInput(client?.phone.orEmpty())) }
    var user by remember(client) { mutableStateOf(client?.user.orEmpty()) }
    var password by remember(client) { mutableStateOf("") }
    var showPassword by remember(client) { mutableStateOf(false) }
    var submitted by remember(client) { mutableStateOf(false) }
    val isNew = client == null
    val errors = validateClientForm(name, phone, user, password, originalUser = client?.user)
    val shown = if (submitted) errors else ClientFormErrors()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "Nuevo cliente" else "Editar cliente", color = Navy, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ValidatedTextField(
                    name,
                    { name = it.take(MAX_CLIENT_NAME) },
                    label = "Nombre o razón social",
                    error = shown.name,
                    modifier = Modifier.fillMaxWidth()
                )
                ValidatedTextField(
                    phone,
                    { phone = phoneInput(it) },
                    label = "Celular (10 dígitos)",
                    error = shown.phone,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                ValidatedTextField(
                    user,
                    { value -> user = value.filterNot(Char::isWhitespace).take(MAX_USER) },
                    label = "Usuario para iniciar sesión",
                    error = shown.user,
                    hint = "De 6 a 20 caracteres, sin espacios.".takeIf { isNew },
                    modifier = Modifier.fillMaxWidth()
                )
                ValidatedTextField(
                    password,
                    { password = it },
                    label = if (isNew) "Contraseña (mínimo 8 caracteres)" else "Nueva contraseña (opcional)",
                    error = shown.password,
                    hint = if (isNew) null else "Déjala vacía para conservar la actual.",
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
            TextButton(
                onClick = {
                    submitted = true
                    if (errors.isValid) onSave(name, phone, user, password)
                },
                enabled = !saving
            ) {
                Text(if (saving) "Guardando…" else "Guardar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Cancelar") } }
    )
}
