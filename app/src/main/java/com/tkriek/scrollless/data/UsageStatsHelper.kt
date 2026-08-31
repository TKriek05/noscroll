package com.tkriek.scrollless.data

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.util.Log
import com.tkriek.scrollless.util.DateUtils
import java.time.LocalDate

/**
 * Leest schermtijd per app uit [UsageStatsManager].
 *
 * PACKAGE_USAGE_STATS is een "special access"-permissie: er is geen runtime-dialog,
 * de gebruiker moet hem zelf aanzetten in Instellingen. [permissionIntent] brengt
 * je naar het juiste scherm, [hasPermission] controleert of het gelukt is.
 */
object UsageStatsHelper {

    private const val TAG = "UsageStatsHelper"

    fun hasPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return if (mode == AppOpsManager.MODE_DEFAULT) {
            context.checkPermission(
                android.Manifest.permission.PACKAGE_USAGE_STATS,
                Process.myPid(),
                Process.myUid()
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            mode == AppOpsManager.MODE_ALLOWED
        }
    }

    fun permissionIntent(context: Context): Intent =
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            // Sommige toestellen springen hiermee direct naar ScrollLess in de lijst.
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    /**
     * Voorgrondtijd per package voor [date], in milliseconden.
     *
     * Gebaseerd op losse events (RESUMED/PAUSED) in plaats van [UsageStatsManager.queryUsageStats],
     * omdat de gebucketteerde variant op veel toestellen dagen aan elkaar plakt en dan
     * te hoge cijfers geeft. Sessies die over middernacht heen lopen worden op de
     * daggrens afgekapt.
     */
    fun foregroundMillisPerApp(
        context: Context,
        date: LocalDate,
        packages: Set<String> = TrackedApp.packageNames
    ): Map<String, Long> {
        if (!hasPermission(context)) return emptyMap()

        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyMap()

        val start = DateUtils.startOfDayMillis(date)
        val end = DateUtils.endOfDayMillis(date)
        if (end <= start) return emptyMap()

        val totals = mutableMapOf<String, Long>()
        val resumedAt = mutableMapOf<String, Long>()

        try {
            // Iets ruimer beginnen zodat we een sessie meepakken die vóór middernacht
            // begon; de optelling zelf blijft binnen [start, end).
            val events = manager.queryEvents(start - LOOKBEHIND_MS, end)
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val pkg = event.packageName ?: continue
                if (pkg !in packages) continue

                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED ->
                        resumedAt[pkg] = event.timeStamp

                    UsageEvents.Event.ACTIVITY_PAUSED,
                    UsageEvents.Event.ACTIVITY_STOPPED -> {
                        val from = resumedAt.remove(pkg) ?: continue
                        totals.addSession(pkg, from, event.timeStamp, start, end)
                    }
                }
            }
            // Nog open sessies (app staat nu op de voorgrond) tellen tot "nu".
            resumedAt.forEach { (pkg, from) -> totals.addSession(pkg, from, end, start, end) }
        } catch (e: SecurityException) {
            Log.w(TAG, "Geen toegang tot usage stats", e)
            return emptyMap()
        }

        return packages.associateWith { totals[it] ?: 0L }
    }

    fun foregroundMillis(context: Context, date: LocalDate, app: TrackedApp): Long =
        foregroundMillisPerApp(context, date, setOf(app.packageName))[app.packageName] ?: 0L

    /** Handig voor Fase 0: log de tijd van vandaag naar logcat. */
    fun logToday(context: Context) {
        val perApp = foregroundMillisPerApp(context, DateUtils.today())
        TrackedApp.entries.forEach { app ->
            val ms = perApp[app.packageName] ?: 0L
            Log.i(TAG, "${app.label}: ${DateUtils.formatDuration(ms)} (${ms} ms)")
        }
    }

    private fun MutableMap<String, Long>.addSession(
        pkg: String,
        from: Long,
        to: Long,
        windowStart: Long,
        windowEnd: Long
    ) {
        val clampedFrom = maxOf(from, windowStart)
        val clampedTo = minOf(to, windowEnd)
        val duration = clampedTo - clampedFrom
        if (duration > 0) {
            this[pkg] = (this[pkg] ?: 0L) + duration
        }
    }

    private const val LOOKBEHIND_MS = 12 * 60 * 60 * 1000L
}
