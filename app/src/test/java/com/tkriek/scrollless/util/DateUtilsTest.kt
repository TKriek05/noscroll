package com.tkriek.scrollless.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DateUtilsTest {

    @Test
    fun `sleutel is een sorteerbare iso-datum`() {
        assertEquals("2026-08-31", DateUtils.key(LocalDate.of(2026, 8, 31)))
    }

    @Test
    fun `week eindigt op de gegeven dag en bevat zeven dagen`() {
        val end = LocalDate.of(2026, 8, 31)
        val week = DateUtils.weekEnding(end)
        assertEquals(7, week.size)
        assertEquals(LocalDate.of(2026, 8, 25), week.first())
        assertEquals(end, week.last())
    }

    @Test
    fun `duur wordt kort en leesbaar weergegeven`() {
        assertEquals("2u 5m", DateUtils.formatDuration((2 * 60 + 5) * 60_000L))
        assertEquals("42m", DateUtils.formatDuration(42 * 60_000L))
        assertEquals("30s", DateUtils.formatDuration(30_000L))
        assertEquals("0s", DateUtils.formatDuration(-1L))
    }
}
