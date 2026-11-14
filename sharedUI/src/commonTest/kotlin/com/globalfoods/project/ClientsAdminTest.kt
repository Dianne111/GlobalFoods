package com.globalfoods.project

import com.globalfoods.project.sharedlogic.network.clients.AdminClient
import kotlin.test.Test
import kotlin.test.assertEquals

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
}
