package com.tkriek.scrollless.util

import com.tkriek.scrollless.data.entities.DailyStat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GamificationTest {

    private val today = LocalDate.of(2026, 8, 31)
    private val todayKey = DateUtils.key(today)

    private fun day(daysAgo: Long) = DateUtils.key(today.minusDays(daysAgo))

    private fun stat(date: String, passed: Int, blocked: Int = 0) =
        DailyStat(date = date, opensPassed = passed, opensBlocked = blocked)

    @Test
    fun `dag boven de limiet breekt de streak`() {
        val streak = Gamification.streakFor(todayKey, listOf(stat(todayKey, passed = 5)), 2)
        assertEquals(0, streak)
    }

    @Test
    fun `precies op de limiet telt nog als goede dag`() {
        val streak = Gamification.streakFor(todayKey, listOf(stat(todayKey, passed = 2)), 2)
        assertEquals(1, streak)
    }

    @Test
    fun `streak stopt bij de eerste slechte dag`() {
        val history = listOf(
            stat(todayKey, passed = 1),
            stat(day(1), passed = 0),
            stat(day(2), passed = 2),
            stat(day(3), passed = 9)
        )
        assertEquals(3, Gamification.streakFor(todayKey, history, 2))
    }

    @Test
    fun `zonder gemeten dagen is er geen streak`() {
        assertEquals(0, Gamification.streakFor(todayKey, emptyList(), 2))
    }

    @Test
    fun `een dag zonder rij telt als goede dag`() {
        val history = listOf(
            stat(todayKey, passed = 0),
            // dag 1 ontbreekt: die dag opende je Instagram of YouTube niet
            stat(day(2), passed = 1),
            stat(day(3), passed = 7)
        )
        assertEquals(3, Gamification.streakFor(todayKey, history, 2))
    }

    @Test
    fun `vandaag telt mee ook voor de worker de dagrij schreef`() {
        assertEquals(2, Gamification.streakFor(todayKey, listOf(stat(day(1), passed = 0)), 2))
    }

    @Test
    fun `punten tellen afgebroken opens plus dagbonus`() {
        val stat = stat(todayKey, passed = 1, blocked = 3)
        val points = Gamification.pointsFor(stat, dailyOpenLimit = 2, streakDays = 1)
        assertEquals(3 * Gamification.POINTS_PER_ABORT + Gamification.POINTS_GOOD_DAY, points)
    }

    @Test
    fun `mijlpaal geeft eenmalig een bonus`() {
        val stat = stat(todayKey, passed = 0, blocked = 0)
        val atMilestone = Gamification.pointsFor(stat, dailyOpenLimit = 2, streakDays = 7)
        val justBefore = Gamification.pointsFor(stat, dailyOpenLimit = 2, streakDays = 6)
        assertEquals(75, atMilestone - justBefore)
    }

    @Test
    fun `slechte dag levert geen dagbonus op`() {
        val stat = stat(todayKey, passed = 5)
        assertFalse(Gamification.isGoodDay(stat, dailyOpenLimit = 2))
        assertEquals(0, Gamification.pointsFor(stat, dailyOpenLimit = 2, streakDays = 0))
    }

    @Test
    fun `volgende mijlpaal loopt op`() {
        assertEquals(3, Gamification.nextMilestone(0))
        assertEquals(7, Gamification.nextMilestone(3))
        assertEquals(30, Gamification.nextMilestone(7))
        assertTrue(Gamification.nextMilestone(30) == null)
    }
}
