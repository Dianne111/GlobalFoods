package com.globalfoods.project

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.globalfoods.project.sharedlogic.network.clients.AdminClient
import com.globalfoods.project.sharedlogic.network.dashboard.ArticuloInventario

/**
 * Últimos datos que cargaron Inicio, Pedidos y Clientes. Al volver de un detalle se muestran de
 * inmediato (mientras se actualizan por detrás) para que la lista tenga sus elementos y conserve
 * la posición en la que estaba. null = todavía no se ha cargado. Lo guarda App y se reinicia al
 * cerrar sesión.
 */
@Stable
class ListsCache {
    var inventory by mutableStateOf<List<ArticuloInventario>?>(null)
    var dashboardOrders by mutableStateOf<List<Order>?>(null)
    var orders by mutableStateOf<List<Order>?>(null)
    var clients by mutableStateOf<List<AdminClient>?>(null)
}
