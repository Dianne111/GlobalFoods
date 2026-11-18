package com.globalfoods.project

import com.globalfoods.project.sharedlogic.network.orders.OrderResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DeliveryDateTest {
    @Test
    fun sendsYearDayMonthWithZeroPaddedDaysAndMonths() {
        assertEquals(20262411, deliveryDateToApi("2026/24/11"))
        assertEquals(20260811, deliveryDateToApi("2026/08/11"))
        assertEquals(20260502, deliveryDateToApi("2026/05/02"))
        assertEquals("2026/24/11", formatOrderDate(20262411))
        assertEquals("2026/08/11", formatOrderDate(20260811))
    }

    @Test
    fun theCalendarSelectionAndEditedOrderUseTheSameDate() {
        val november24 = 1795478400000L

        assertEquals("2026/24/11", millisToDate(november24))
        assertEquals(november24, dateToMillis("2026/24/11"))
        val form = formFromOrder(OrderResponse(id = 1, date = 20262411))
        assertEquals("2026/24/11", form.deliveryDate)
        assertEquals(20262411, deliveryDateToApi(form.deliveryDate))
    }

    @Test
    fun rejectsInvalidDatesAndThePreviousInputFormat() {
        assertNull(deliveryDateToApi("24/11/2026"))
        assertNull(deliveryDateToApi("2026/31/04"))
        assertNull(deliveryDateToApi("2026/29/02"))
        assertEquals(20242902, deliveryDateToApi("2024/29/02"))
        assertNull(deliveryDateToApi("2026/5/2"))
        assertNull(formatOrderDate(20263104))
        assertNull(formatOrderDate(0))
        assertEquals("", formFromOrder(OrderResponse(id = 2, date = 0)).deliveryDate)
    }
}
