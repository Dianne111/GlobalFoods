package com.globalfoods.project

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TelegramAssistantTest {
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
