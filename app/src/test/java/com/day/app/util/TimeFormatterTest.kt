package com.day.app.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.Locale

class TimeFormatterTest {

    @Test
    fun testFormatUserTime_hourAndMinuteBoundaryCases() {
        // Midnight 00:00 -> 12:00 AM
        assertEquals("12:00 AM", TimeFormatter.formatUserTime(0, 0))

        // 00:05 -> 12:05 AM
        assertEquals("12:05 AM", TimeFormatter.formatUserTime(0, 5))

        // 01:00 -> 1:00 AM
        assertEquals("1:00 AM", TimeFormatter.formatUserTime(1, 0))

        // 08:30 -> 8:30 AM
        assertEquals("8:30 AM", TimeFormatter.formatUserTime(8, 30))

        // 09:05 -> 9:05 AM
        assertEquals("9:05 AM", TimeFormatter.formatUserTime(9, 5))

        // 11:59 -> 11:59 AM
        assertEquals("11:59 AM", TimeFormatter.formatUserTime(11, 59))

        // Noon 12:00 -> 12:00 PM
        assertEquals("12:00 PM", TimeFormatter.formatUserTime(12, 0))

        // 12:01 -> 12:01 PM
        assertEquals("12:01 PM", TimeFormatter.formatUserTime(12, 1))

        // 13:00 -> 1:00 PM
        assertEquals("1:00 PM", TimeFormatter.formatUserTime(13, 0))

        // 15:45 -> 3:45 PM
        assertEquals("3:45 PM", TimeFormatter.formatUserTime(15, 45))

        // 18:30 -> 6:30 PM
        assertEquals("6:30 PM", TimeFormatter.formatUserTime(18, 30))

        // 23:59 -> 11:59 PM
        assertEquals("11:59 PM", TimeFormatter.formatUserTime(23, 59))
    }

    @Test
    fun testFormatUserTime_epochTimestamp() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 18)
            set(Calendar.MINUTE, 42)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val formatted = TimeFormatter.formatUserTime(cal.timeInMillis)
        // Must contain 6:42 and PM (no leading zero on 6)
        assertEquals("6:42 PM", formatted)
    }

    @Test
    fun testGetClockComponents() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 5)
        }
        val components = TimeFormatter.getClockComponents(cal.time)
        assertEquals("9:05", components.first)
        assertEquals("AM", components.second)
    }

    @Test
    fun testFormatDateTime_and_Deadline() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.SEPTEMBER)
            set(Calendar.DAY_OF_MONTH, 28)
            set(Calendar.HOUR_OF_DAY, 18)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dt = TimeFormatter.formatDateTime(cal.timeInMillis)
        assertEquals("SEP 28, 6:30 PM", dt)

        val deadline = TimeFormatter.formatDeadline(cal.timeInMillis)
        assertEquals("28 SEP, 6:30 PM", deadline)

        val withSec = TimeFormatter.formatUserTimeWithSeconds(cal.timeInMillis)
        assertEquals("6:30:00 PM", withSec)
    }
}
