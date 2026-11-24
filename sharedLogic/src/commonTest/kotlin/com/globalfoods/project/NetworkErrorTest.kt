package com.globalfoods.project

import com.globalfoods.project.sharedlogic.network.core.NO_CONNECTION_MESSAGE
import com.globalfoods.project.sharedlogic.network.core.TIMEOUT_MESSAGE
import com.globalfoods.project.sharedlogic.network.core.networkErrorMessage
import com.globalfoods.project.sharedlogic.network.orders.OrderRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NetworkErrorTest {
    @Test
    fun timeoutsAndMissingConnectionGetAClearMessage() {
        assertEquals(TIMEOUT_MESSAGE, networkErrorMessage(HttpRequestTimeoutException("https://api", 20_000)))
        assertEquals(TIMEOUT_MESSAGE, networkErrorMessage(ConnectTimeoutException("connect timeout")))
        assertEquals(NO_CONNECTION_MESSAGE, networkErrorMessage(IOException("Unable to resolve host")))
        // Los repositorios envuelven la excepción: se revisa la causa.
        assertEquals(NO_CONNECTION_MESSAGE, networkErrorMessage(Exception("envuelta", IOException("sin red"))))
        assertNull(networkErrorMessage(IllegalStateException("La API rechazó el pedido (HTTP 400)")))
    }

    @Test
    fun aRepositoryReportsTheMissingConnectionInSpanish() = runBlocking {
        val client = HttpClient(MockEngine { throw IOException("Unable to resolve host api.globalfoodsmexico.com") })
        val result = OrderRepository(client).getOrders()
        assertEquals(NO_CONNECTION_MESSAGE, result.exceptionOrNull()?.message)
    }
}
