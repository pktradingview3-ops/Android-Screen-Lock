package com.timewall.app.domain

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Text pieces the renderer draws. Always two-digit hour and minute, so layouts stay aligned. */
data class TimeParts(
    val hour: String,
    val minute: String,
    val date: String,
)

object TimeFormatter {

    private val dateFormat = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", Locale.ENGLISH)

    fun format(time: LocalDateTime, use24h: Boolean): TimeParts {
        val hour = if (use24h) {
            time.hour
        } else {
            // 00:xx -> 12, 13:xx -> 01, 12:xx -> 12
            val h = time.hour % 12
            if (h == 0) 12 else h
        }
        return TimeParts(
            hour = "%02d".format(hour),
            minute = "%02d".format(time.minute),
            date = time.format(dateFormat).uppercase(Locale.ENGLISH),
        )
    }
}
