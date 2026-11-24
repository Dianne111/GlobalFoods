package com.globalfoods.project

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CreationDateTest {
    private fun allowed(date: String, today: String): Boolean =
        isSelectableCreationDate(dateToMillis(date), requireNotNull(dateToMillis(today)))

    @Test
    fun allowsTodayAndTomorrowButNotYesterday() {
        assertFalse(allowed("20/11/2026", "21/11/2026"))
        assertTrue(allowed("21/11/2026", "21/11/2026"))
        assertTrue(allowed("22/11/2026", "21/11/2026"))
    }

    @Test
    fun comparesCompleteDatesAcrossMonthsAndYears() {
        assertFalse(allowed("31/12/2025", "01/01/2026"))
        assertFalse(allowed("31/10/2026", "01/11/2026"))
        assertTrue(allowed("01/01/2027", "31/12/2026"))
        assertTrue(allowed("31/12/2100", "21/11/2026"))
    }

    @Test
    fun aDateThatBecamePastWhileTheFormWasOpenIsRejected() {
        assertTrue(allowed("21/11/2026", "21/11/2026"))
        assertFalse(allowed("21/11/2026", "22/11/2026"))
    }

    @Test
    fun invalidOrMissingDatesCannotBeSubmitted() {
        for (date in listOf("", "31/11/2026", "29/02/2027", "2026/11/21", "00/12/2026")) {
            assertFalse(allowed(date, "21/11/2026"))
        }
    }

    @Test
    fun validLeapDaysCanBeSelectedAsTodayOrFuture() {
        assertTrue(allowed("29/02/2028", "28/02/2028"))
        assertTrue(allowed("29/02/2028", "29/02/2028"))
        assertFalse(allowed("28/02/2028", "29/02/2028"))
    }
}
