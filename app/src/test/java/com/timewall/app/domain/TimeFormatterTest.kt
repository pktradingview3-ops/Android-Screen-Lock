package com.timewall.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class TimeFormatterTest {

    @Test
    fun twentyFourHour_eveningUsesHourAsIs() {
        val p = TimeFormatter.format(LocalDateTime.of(2026, 10, 8, 20, 7), use24h = true)
        assertEquals("20", p.hour)
        assertEquals("07", p.minute)
    }

    @Test
    fun twelveHour_eveningConvertsToPm() {
        val p = TimeFormatter.format(LocalDateTime.of(2026, 10, 8, 20, 7), use24h = false)
        assertEquals("08", p.hour)
        assertEquals("07", p.minute)
    }

    @Test
    fun twelveHour_midnightIsTwelve() {
        val p = TimeFormatter.format(LocalDateTime.of(2026, 10, 8, 0, 5), use24h = false)
        assertEquals("12", p.hour)
        assertEquals("05", p.minute)
    }

    @Test
    fun twelveHour_noonIsTwelve() {
        val p = TimeFormatter.format(LocalDateTime.of(2026, 10, 8, 12, 30), use24h = false)
        assertEquals("12", p.hour)
        assertEquals("30", p.minute)
    }

    @Test
    fun twentyFourHour_midnightIsZero() {
        val p = TimeFormatter.format(LocalDateTime.of(2026, 10, 8, 0, 0), use24h = true)
        assertEquals("00", p.hour)
        assertEquals("00", p.minute)
    }

    @Test
    fun date_isUppercaseWithWeekday() {
        val p = TimeFormatter.format(LocalDateTime.of(2026, 10, 8, 20, 7), use24h = true)
        assertEquals("THU, 08 OCT 2026", p.date)
    }

    @Test
    fun date_leapDay() {
        val p = TimeFormatter.format(LocalDateTime.of(2028, 2, 29, 9, 0), use24h = true)
        assertEquals("TUE, 29 FEB 2028", p.date)
    }
}
