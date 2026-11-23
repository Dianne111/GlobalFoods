package com.globalfoods.project

import java.util.Calendar

internal actual fun currentPaymentDate(): Int {
    val calendar = Calendar.getInstance()
    return calendar.get(Calendar.YEAR) * 10000 +
        (calendar.get(Calendar.MONTH) + 1) * 100 + calendar.get(Calendar.DAY_OF_MONTH)
}
