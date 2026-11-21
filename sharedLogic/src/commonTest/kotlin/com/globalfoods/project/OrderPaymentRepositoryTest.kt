package com.globalfoods.project

import com.globalfoods.project.sharedlogic.network.orders.OrderLineResponse
import com.globalfoods.project.sharedlogic.network.orders.OrderPaymentMethod
import com.globalfoods.project.sharedlogic.network.orders.OrderRepository
import com.globalfoods.project.sharedlogic.network.orders.OrderRequest
import com.globalfoods.project.sharedlogic.network.orders.OrderResponse
import com.globalfoods.project.sharedlogic.network.orders.toRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OrderPaymentRepositoryTest {
    private val original = OrderResponse(
        id = 7, clientId = 12, branchId = 3, date = 20261120, paymentMethod = 0, status = 3,
        totalKg = 5.0, total = 1250.0, notes = "No cambiar estas observaciones",
        lines = listOf(OrderLineResponse(id = 40, productCode = 101, size = "Mediano", quantity = 5.0, price = 250.0, subtotal = 1250.0))
    )
    private val headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    private fun client(engine: MockEngine) = HttpClient(engine) { install(ContentNegotiation) { json() } }

    @Test
    fun creatingAnOrderStillSendsZeroEvenIfAnotherMethodWasProvided() = runBlocking {
        var sent: OrderRequest? = null
        val client = client(MockEngine { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("/api/pedidos", request.url.encodedPath)
            sent = Json.decodeFromString<OrderRequest>((request.body as TextContent).text)
            respond(Json.encodeToString(OrderResponse.serializer(), original.copy(status = 1)), headers = headers)
        })
        try {
            OrderRepository(client).createOrder(original.toRequest().copy(status = 1, paymentMethod = 2)).getOrThrow()
            assertEquals(0, sent?.paymentMethod)
        } finally { client.close() }
    }

    @Test
    fun sendsTheSelectedMethodAndDateUsingPutThenConfirmsWithGet() = runBlocking {
        for (method in OrderPaymentMethod.entries) {
            var stored = original
            val calls = mutableListOf<HttpMethod>()
            val client = client(MockEngine { request ->
                calls += request.method
                assertEquals("/api/pedidos/7", request.url.encodedPath)
                if (request.method == HttpMethod.Put) {
                    val body = Json.decodeFromString<OrderRequest>((request.body as TextContent).text)
                    assertEquals(original.toRequest().copy(date = 20261121, paymentMethod = method.code), body)
                    stored = original.copy(date = body.date, paymentMethod = body.paymentMethod)
                    respond("", HttpStatusCode.NoContent)
                } else respond(Json.encodeToString(OrderResponse.serializer(), stored), headers = headers)
            })
            try {
                val result = OrderRepository(client).registerPayment(7, method, 20261121).getOrThrow()
                assertEquals(method.code, result.paymentMethod)
                assertEquals(20261121, result.date)
                assertEquals(3, result.status)
                assertEquals(listOf(HttpMethod.Get, HttpMethod.Put, HttpMethod.Get), calls)
            } finally { client.close() }
        }
    }

    @Test
    fun reportsFailureIfTheApiAcceptsThePutButDoesNotPersistTheFields() = runBlocking {
        val client = client(MockEngine { request ->
            if (request.method == HttpMethod.Put) respond("", HttpStatusCode.NoContent)
            else respond(Json.encodeToString(OrderResponse.serializer(), original), headers = headers)
        })
        try {
            assertTrue(OrderRepository(client).registerPayment(7, OrderPaymentMethod.Cash, 20261121).isFailure)
        } finally { client.close() }
    }

    @Test
    fun aLostConfirmationCanBeRetriedWithoutSendingTheSamePutTwice() = runBlocking {
        var stored = original
        var puts = 0
        var loseConfirmation = true
        val client = client(MockEngine { request ->
            when {
                request.method == HttpMethod.Put -> {
                    puts++
                    stored = original.copy(date = 20261121, paymentMethod = 1)
                    respond("", HttpStatusCode.NoContent)
                }
                puts > 0 && loseConfirmation -> {
                    loseConfirmation = false
                    error("Conexión interrumpida después de guardar")
                }
                else -> respond(Json.encodeToString(OrderResponse.serializer(), stored), headers = headers)
            }
        })
        try {
            val repository = OrderRepository(client)
            assertTrue(repository.registerPayment(7, OrderPaymentMethod.Cash, 20261121).isFailure)
            assertEquals(stored, repository.registerPayment(7, OrderPaymentMethod.Cash, 20261121).getOrThrow())
            assertEquals(1, puts)
        } finally { client.close() }
    }

    @Test
    fun explainsWhenTheApiDoesNotReturnMetodoPagoAfterAnAcceptedUpdate() = runBlocking {
        var put: OrderRequest? = null
        val client = client(MockEngine { request ->
            if (request.method == HttpMethod.Put) {
                put = Json.decodeFromString<OrderRequest>((request.body as TextContent).text)
                respond("", HttpStatusCode.NoContent)
            } else respond(Json.encodeToString(OrderResponse.serializer(), original.copy(paymentMethod = null)), headers = headers)
        })
        try {
            val result = OrderRepository(client).registerPayment(7, OrderPaymentMethod.CardOrTransfer, 20261121)
            assertEquals(2, put?.paymentMethod)
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("no devolvió MetodoPago"))
        } finally { client.close() }
    }

    @Test
    fun rechecksTheCurrentStateBeforeSendingAnUpdate() = runBlocking {
        val calls = mutableListOf<HttpMethod>()
        val client = client(MockEngine { request ->
            calls += request.method
            respond(Json.encodeToString(OrderResponse.serializer(), original.copy(status = 4)), headers = headers)
        })
        try {
            assertTrue(OrderRepository(client).registerPayment(7, OrderPaymentMethod.Cash, 20261121).isFailure)
            assertEquals(listOf(HttpMethod.Get), calls)
        } finally { client.close() }
    }
}
