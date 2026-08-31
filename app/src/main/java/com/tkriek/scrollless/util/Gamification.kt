package com.tkriek.scrollless.util

import com.tkriek.scrollless.data.entities.DailyStat

/**
 * Punten en streaks. Bewust simpel en herberekenbaar: alles is af te leiden uit
 * de dagcijfers, dus er is geen aparte tabel nodig.
 */
object Gamification {

    const val POINTS_PER_ABORT = 10
    const val POINTS_GOOD_DAY = 5

    val streakBonuses: Map<Int, Int> = mapOf(3 to 25, 7 to 75, 30 to 300)

    /** Een dag is "goed" als je niet vaker dan je limiet alsnog doorging. */
    fun isGoodDay(stat: DailyStat, dailyOpenLimit: Int): Boolean =
        stat.opensPassed <= dailyOpenLimit

    /**
     * Streak tot en met [todayKey]. [history] bevat de dagrijen inclusief die van
     * vandaag. Een dag zonder rij telt als goed — dan opende je de app niet eens —
     * maar alleen vanaf de eerste dag die ooit gemeten is; anders zou een verse
     * installatie meteen met een streak van een jaar beginnen.
     */
    fun streakFor(
        todayKey: String,
        history: List<DailyStat>,
        dailyOpenLimit: Int
    ): Int {
        if (history.isEmpty()) return 0
        val byDate = history.associateBy { it.date }
        val earliest = history.minOf { DateUtils.parse(it.date) }
        var date = DateUtils.parse(todayKey)
        var streak = 0
        while (!date.isBefore(earliest)) {
            val stat = byDate[DateUtils.key(date)]
            val good = stat == null || isGoodDay(stat, dailyOpenLimit)
            if (!good) return streak
            streak++
            date = date.minusDays(1)
        }
        return streak
    }

    fun pointsFor(stat: DailyStat, dailyOpenLimit: Int, streakDays: Int): Int {
        var points = stat.opensBlocked * POINTS_PER_ABORT
        if (isGoodDay(stat, dailyOpenLimit) && stat.totalOpens > 0) {
            points += POINTS_GOOD_DAY
        }
        streakBonuses[streakDays]?.let { points += it }
        return points
    }

    /** Volgende mijlpaal om naartoe te werken, of null als je er al voorbij bent. */
    fun nextMilestone(streakDays: Int): Int? =
        streakBonuses.keys.sorted().firstOrNull { it > streakDays }
}
