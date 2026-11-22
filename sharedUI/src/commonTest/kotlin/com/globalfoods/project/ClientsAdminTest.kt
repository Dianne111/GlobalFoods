package com.globalfoods.project

import com.globalfoods.project.sharedlogic.network.clients.AdminClient
import com.globalfoods.project.sharedlogic.network.orders.Client
import com.globalfoods.project.sharedlogic.network.orders.OrderResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClientsAdminTest {
    private val clients = listOf(
        AdminClient(id = 1, user = "palo_seco", name = "Marisquería Palo Seco", phone = "33 1234 5678"),
        AdminClient(id = 2, user = "luisf", name = "Luis Fernando Olivarria", phone = "(331) 999-0000", status = 0),
        AdminClient(id = 3, user = "elfaro", name = "restaurante El Faro", phone = null)
    )

    @Test
    fun searchesByNameUserOrPhoneDigits() {
        assertEquals(listOf(1), searchClients(clients, "palo").map { it.id })
        assertEquals(listOf(3), searchClients(clients, "ELFARO").map { it.id })
        assertEquals(listOf(1), searchClients(clients, "1234 56").map { it.id })
        assertEquals(listOf(2), searchClients(clients, "331999").map { it.id })
        assertEquals(clients, searchClients(clients, "  "))
    }

    @Test
    fun sortsAlphabeticallyIgnoringCase() {
        assertEquals(listOf(2, 1, 3), sortClients(clients).map { it.id })
    }

    @Test
    fun onlyUndeliveredOrdersOfTheClientBlockDeactivation() {
        val orders = listOf(
            OrderResponse(id = 10, clientId = 1, status = 1), // Creado
            OrderResponse(id = 11, clientId = 1, status = 2), // Surtido
            OrderResponse(id = 12, clientId = 1, status = 3), // En reparto
            OrderResponse(id = 13, clientId = 1, status = 4), // Entregado
            OrderResponse(id = 14, clientId = 1, status = 5), // Cancelado
            OrderResponse(id = 15, clientId = 2, status = 1), // Otro cliente
            OrderResponse(id = 16, client = Client(id = 1, name = "Palo Seco"), status = 2) // Id solo en Cliente
        )
        assertEquals(listOf(10, 11, 12, 16), pendingDeliveryOrders(orders, clientId = 1).map { it.id })
        assertTrue(pendingDeliveryOrders(orders.filter { it.status >= 4 }, clientId = 1).isEmpty())
    }

    @Test
    fun onlyUndeliveredOrdersToTheBranchBlockItsDeactivation() {
        val orders = listOf(
            OrderResponse(id = 20, clientId = 1, branchId = 5, status = 1), // Creado
            OrderResponse(id = 21, clientId = 1, branchId = 5, status = 3), // En reparto
            OrderResponse(id = 22, clientId = 1, branchId = 5, status = 4), // Entregado
            OrderResponse(id = 23, clientId = 1, branchId = 5, status = 5), // Cancelado
            OrderResponse(id = 24, clientId = 1, branchId = 6, status = 2), // Otra sucursal
            OrderResponse(id = 25, clientId = 1, branchId = null, status = 1) // Recolección
        )
        assertEquals(listOf(20, 21), pendingBranchOrders(orders, branchId = 5).map { it.id })
        assertTrue(pendingBranchOrders(orders, branchId = 7).isEmpty())

        val message = pendingBranchOrdersMessage("Centro", pendingBranchOrders(orders, branchId = 5))
        assertEquals(
            "La sucursal \"Centro\" tiene 2 pedidos en proceso (#20, #21). " +
                "Podrá darse de baja cuando esos pedidos se entreguen o se cancelen.",
            message
        )
    }

    @Test
    fun pendingOrdersMessageListsTheOrders() {
        val one = pendingOrdersMessage("El Faro", listOf(OrderResponse(id = 7, clientId = 3)))
        assertEquals(
            "\"El Faro\" tiene 1 pedido en proceso (#7). Podrá darse de baja cuando ese pedido se entregue o se cancele.",
            one
        )

        val many = pendingOrdersMessage("El Faro", (1..7).map { OrderResponse(id = it, clientId = 3) })
        assertTrue("7 pedidos en proceso" in many && "#5" in many && "#6" !in many && "y 2 más" in many)
    }
}
