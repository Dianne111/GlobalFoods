package com.globalfoods.project

import kotlin.test.Test
import kotlin.test.assertEquals

class QuantityInputTest {
    private fun type(text: String, maximum: Double = 100.0, initial: String = ""): String {
        var field = initial
        text.forEach { character -> field = normalizeQuantity(field + character, maximum) }
        return field
    }

    @Test
    fun typingTenAndAHalfDoesNotBecomeOneHundredAndFive() {
        assertEquals("10.", type("10."))
        assertEquals("10.5", type("10.5"))
        assertEquals("10.5", type("10.5", initial = "0"))
    }

    @Test
    fun supportsFractionsBelowOneKilogramAndStartingWithTheSeparator() {
        assertEquals("0.", type("0."))
        assertEquals("0.5", type("0.5"))
        assertEquals("0.25", type(".25"))
    }

    @Test
    fun commaKeyboardsProduceTheSameNumericQuantity() {
        assertEquals("10.5", type("10,5"))
        assertEquals("0.25", type("0,25"))
    }

    @Test
    fun keepsDecimalStockLimitsAndDoesNotRoundDownTheQuantity() {
        assertEquals("10.5", normalizeQuantity("10.75", 10.5))
        assertEquals("0.5", type("0.75", maximum = 0.5))
        assertEquals("10.125", type("10.125"))
    }

    @Test
    fun allowsClearingAndDeletingTheFraction() {
        assertEquals("", normalizeQuantity("", 100.0))
        assertEquals("10.", normalizeQuantity("10.", 100.0))
        assertEquals("10", normalizeQuantity("10", 100.0))
    }

    @Test
    fun aSecondSeparatorIsIgnoredWithoutChangingTheExistingQuantity() {
        assertEquals("10.5", normalizeQuantity("10.5.", 100.0))
        assertEquals("10.5", normalizeQuantity("10.5.9", 100.0))
    }
}
