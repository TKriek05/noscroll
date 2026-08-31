package com.tkriek.scrollless.data

import android.content.Context
import com.tkriek.scrollless.data.entities.AppOpenEvent
import com.tkriek.scrollless.data.entities.DailyStat
import com.tkriek.scrollless.util.DateUtils
import com.tkriek.scrollless.util.Gamification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Enige plek waar events, dagcijfers en usage stats samenkomen.
 */
class ScrollLessRepository(private val context: Context) {

    private val db = AppDatabase.get(context)
    private val eventDao = db.appOpenEventDao()
    private val statDao = db.dailyStatDao()
    private val settings = Settings(context)

    fun observeToday(): Flow<DailyStat?> = statDao.observe(DateUtils.todayKey())

    fun observeWeek(): Flow<List<DailyStat>> {
        val week = DateUtils.weekEnding()
        return statDao.observeRange(DateUtils.key(week.first()), DateUtils.key(week.last()))
    }

    fun observeRecentEvents(limit: Int = 20): Flow<List<AppOpenEvent>> =
        eventDao.observeRecent(limit)

    fun observeTotalPoints(): Flow<Int> = statDao.observeTotalPoints()

    /** Legt een keuze op het interventiescherm vast en herberekent de dag. */
    suspend fun logOutcome(packageName: String, outcome: String) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        eventDao.insert(
            AppOpenEvent(
                packageName = packageName,
                timestamp = now,
                outcome = outcome,
                date = DateUtils.todayKey()
            )
        )
        refreshDay(DateUtils.today())
    }

    /**
     * Herberekent de dagrij: schermtijd uit UsageStats, tellingen uit de events,
     * plus streak en punten. Idempotent, dus de worker mag dit zo vaak draaien
     * als hij wil.
     */
    suspend fun refreshDay(date: LocalDate): DailyStat = withContext(Dispatchers.IO) {
        val dateKey = DateUtils.key(date)
        val usage = UsageStatsHelper.foregroundMillisPerApp(context, date)
        val blocked = eventDao.countByOutcome(dateKey, AppOpenEvent.OUTCOME_ABORTED)
        val passed = eventDao.countByOutcome(dateKey, AppOpenEvent.OUTCOME_PASSED)

        val base = DailyStat(
            date = dateKey,
            instagramMs = usage[TrackedApp.INSTAGRAM.packageName] ?: 0L,
            youtubeMs = usage[TrackedApp.YOUTUBE.packageName] ?: 0L,
            opensBlocked = blocked,
            opensPassed = passed
        )

        val limit = settings.dailyOpenLimit
        // De dag zelf zit ook in de history, maar met de oude tellingen; vervang 'm
        // door de zojuist berekende versie zodat de streak klopt.
        val history = statDao.historyBefore(dateKey, HISTORY_DAYS)
            .filterNot { it.date == dateKey } + base
        val streak = Gamification.streakFor(dateKey, history.sortedByDescending { it.date }, limit)
        val stat = base.copy(
            streakDays = streak,
            points = Gamification.pointsFor(base, limit, streak)
        )

        statDao.upsert(stat)
        stat
    }

    /** Zowel vandaag als gisteren, zodat een dag netjes wordt afgesloten na middernacht. */
    suspend fun refreshRecentDays() {
        val today = DateUtils.today()
        refreshDay(today.minusDays(1))
        refreshDay(today)
    }

    /** Ruwe events ouder dan 90 dagen hebben we niet meer nodig; de dagcijfers blijven. */
    suspend fun pruneOldEvents() = withContext(Dispatchers.IO) {
        val cutoff = System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000
        eventDao.deleteOlderThan(cutoff)
    }

    companion object {
        private const val HISTORY_DAYS = 400

        @Volatile
        private var instance: ScrollLessRepository? = null

        fun get(context: Context): ScrollLessRepository =
            instance ?: synchronized(this) {
                instance ?: ScrollLessRepository(context.applicationContext).also { instance = it }
            }
    }
}
