package com.tkriek.scrollless.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

/**
 * Instellingen van de gebruiker. Bewust SharedPreferences en niet DataStore:
 * de AccessibilityService moet deze waarden synchroon kunnen lezen op het moment
 * dat er een event binnenkomt, zonder coroutine.
 */
class Settings(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("scrollless_settings", Context.MODE_PRIVATE)

    /** Hoe lang je moet wachten voor "Toch doorgaan" aanklikbaar wordt. */
    var waitSeconds: Int
        get() = prefs.getInt(KEY_WAIT_SECONDS, DEFAULT_WAIT_SECONDS)
        set(value) = prefs.edit().putInt(KEY_WAIT_SECONDS, value.coerceIn(0, 60)).apply()

    /** Hoe vaak je per dag mag doorgaan en de dag toch als "goed" telt. */
    var dailyOpenLimit: Int
        get() = prefs.getInt(KEY_DAILY_LIMIT, DEFAULT_DAILY_LIMIT)
        set(value) = prefs.edit().putInt(KEY_DAILY_LIMIT, value.coerceIn(0, 20)).apply()

    /** Na "Toch doorgaan" laten we je zolang met rust, anders krijg je het scherm bij elke schermwissel. */
    var graceMinutes: Int
        get() = prefs.getInt(KEY_GRACE_MINUTES, DEFAULT_GRACE_MINUTES)
        set(value) = prefs.edit().putInt(KEY_GRACE_MINUTES, value.coerceIn(1, 120)).apply()

    /** Tijdstip tot wanneer de bewaker zwijgt (millis, epoch). */
    var quietUntil: Long
        get() = prefs.getLong(KEY_QUIET_UNTIL, 0L)
        set(value) = prefs.edit().putLong(KEY_QUIET_UNTIL, value).apply()

    var guardEnabled: Boolean
        get() = prefs.getBoolean(KEY_GUARD_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_GUARD_ENABLED, value).apply()

    fun startGracePeriod(now: Long = System.currentTimeMillis()) {
        quietUntil = now + graceMinutes * 60_000L
    }

    fun endGracePeriod() {
        quietUntil = 0L
    }

    fun isQuiet(now: Long = System.currentTimeMillis()): Boolean = now < quietUntil

    fun changes(): Flow<Settings> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            trySend(this@Settings)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(this@Settings)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.conflate()

    companion object {
        const val DEFAULT_WAIT_SECONDS = 8
        const val DEFAULT_DAILY_LIMIT = 2
        const val DEFAULT_GRACE_MINUTES = 5

        private const val KEY_WAIT_SECONDS = "wait_seconds"
        private const val KEY_DAILY_LIMIT = "daily_limit"
        private const val KEY_GRACE_MINUTES = "grace_minutes"
        private const val KEY_QUIET_UNTIL = "quiet_until"
        private const val KEY_GUARD_ENABLED = "guard_enabled"
    }
}
