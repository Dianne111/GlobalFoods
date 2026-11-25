package com.globalfoods.project

import com.globalfoods.project.sharedlogic.network.orders.DeliveryPayment
import com.globalfoods.project.sharedlogic.network.orders.PaymentType
import com.globalfoods.project.sharedlogic.network.orders.amountInput
import com.globalfoods.project.sharedlogic.network.orders.checkDeliveryPayment
import com.globalfoods.project.sharedlogic.network.orders.paymentMethodLabel
import com.globalfoods.project.sharedlogic.network.orders.remainingForCard
import com.globalfoods.project.sharedlogic.network.orders.sanitizeAmountInput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DeliveryPaymentTest {
    @Test
    fun paymentTypeFollowsTheApiCodes() {
        assertEquals(listOf(0, 1, 2, 3), PaymentType.entries.map { it.code })
        assertEquals(PaymentType.Cash, DeliveryPayment(cash = 500.0, card = 0.0).type)
        assertEquals(PaymentType.Card, DeliveryPayment(cash = 0.0, card = 500.0).type)
        assertEquals(PaymentType.Mixed, DeliveryPayment(cash = 300.0, card = 200.0).type)
        assertEquals(PaymentType.Pending, DeliveryPayment(cash = 0.0, card = 0.0).type)
        assertEquals("Mixto", paymentMethodLabel(3))
    }

    @Test
    fun theRestOfTheTotalGoesToCard() {
        assertEquals("478.00", remainingForCard(1178.0, "700"))
        assertEquals("0.00", remainingForCard(1178.0, "1178"))
        assertEquals("0.00", remainingForCard(1178.0, "2000"))
        assertEquals("1178.00", remainingForCard(1178.0, ""))
        assertEquals("1234.50", amountInput(1234.5))
    }

    @Test
    fun amountsAcceptOnlyNumbersWithTwoDecimals() {
        assertEquals("1200.50", sanitizeAmountInput("$1,200.505"))
        assertEquals("0.5", sanitizeAmountInput(".5"))
        assertEquals("12.34", sanitizeAmountInput("12.3.4"))
        assertEquals("", sanitizeAmountInput("abc-"))
    }

    @Test
    fun aPaymentThatMatchesTheTotalIsReady() {
        val cash = checkDeliveryPayment(1178.0, "1178.00", "0.00")
        assertEquals(DeliveryPayment(1178.0, 0.0), cash.payment)
        assertNull(cash.error)
        val mixed = checkDeliveryPayment(1178.0, "700", "478")
        assertEquals(PaymentType.Mixed, mixed.payment?.type)
        // Centavos que no se pierden por el redondeo de Double.
        assertEquals(PaymentType.Mixed, checkDeliveryPayment(0.3, "0.1", "0.2").payment?.type)
    }

    @Test
    fun explainsWhatIsWrongWithTheAmounts() {
        assertEquals(
            "Faltan $78.00 por asignar a un método de pago.",
            checkDeliveryPayment(1178.0, "700", "400").error
        )
        assertEquals(
            "Los montos suman $1,000.00 más que el total.",
            checkDeliveryPayment(1178.0, "1178", "1000").error
        )
        assertEquals(
            "Escribe cuánto se cobró en efectivo o con tarjeta.",
            checkDeliveryPayment(1178.0, "", "").error
        )
        assertEquals("Escribe un monto válido en Efectivo.", checkDeliveryPayment(10.0, ".", "0").error)
        assertNull(checkDeliveryPayment(10.0, "5", "4").payment)
    }
}
