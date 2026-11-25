package com.globalfoods.project

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TelegramAssistantTest {
    @Test
    fun includesTheSelectedOrdersIdInTheNativeBotStartParameter() {
        for (id in listOf(25, 184)) {
            val attempts = mutableListOf<String>()
            assertTrue(TelegramAssistant.open(id) { uri -> attempts += uri; true })
            assertEquals(listOf("tg://resolve?domain=GlobalFoodsMexicobot&start=pedido_$id"), attempts)
        }
    }

    @Test
    fun preservesTheOrderReferenceWhenOpeningTheBotInTheBrowser() {
        val attempts = mutableListOf<String>()
        assertTrue(TelegramAssistant.open(25) { uri ->
            attempts += uri
            uri.startsWith("https://")
        })
        assertEquals(listOf(
            "tg://resolve?domain=GlobalFoodsMexicobot&start=pedido_25",
            "https://t.me/GlobalFoodsMexicobot?start=pedido_25"
        ), attempts)
    }

    @Test
    fun anInvalidOrderDoesNotOpenAnUnrelatedGenericConversation() {
        for (id in listOf(0, -1)) {
            val attempts = mutableListOf<String>()
            assertFalse(TelegramAssistant.open(id) { uri -> attempts += uri; true })
            assertTrue(attempts.isEmpty())
        }
    }

    @Test
    fun reportsFailureIfNeitherHandlerCanOpenTheSpecificOrder() {
        val attempts = mutableListOf<String>()
        assertFalse(TelegramAssistant.open(25) { uri -> attempts += uri; false })
        assertEquals(2, attempts.size)
        assertTrue(attempts.all { it.endsWith("start=pedido_25") })
    }

    @Test
    fun opensTheRequestedBotInTelegramWhenAvailable() {
        val attempts = mutableListOf<String>()
        assertTrue(TelegramAssistant.open { uri -> attempts += uri; true })
        assertEquals(listOf("tg://resolve?domain=GlobalFoodsMexicobot"), attempts)
    }

    @Test
    fun usesTheSameBotsWebLinkWhenTelegramIsNotInstalled() {
        val attempts = mutableListOf<String>()
        assertTrue(TelegramAssistant.open { uri ->
            attempts += uri
            uri == "https://t.me/GlobalFoodsMexicobot"
        })
        assertEquals(listOf("tg://resolve?domain=GlobalFoodsMexicobot", "https://t.me/GlobalFoodsMexicobot"), attempts)
    }

    @Test
    fun reportsWhenNeitherTelegramNorABrowserCanOpenTheLink() {
        val attempts = mutableListOf<String>()
        assertFalse(TelegramAssistant.open { uri -> attempts += uri; false })
        assertEquals(2, attempts.size)
    }
}
