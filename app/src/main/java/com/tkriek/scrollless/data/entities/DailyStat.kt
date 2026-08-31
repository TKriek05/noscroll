package com.tkriek.scrollless.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Samenvatting per dag. Wordt elke 15 minuten door de UsageSyncWorker herschreven
 * en direct bijgewerkt zodra je een keuze maakt op het interventiescherm.
 */
@Entity(tableName = "daily_stat")
data class DailyStat(
    @PrimaryKey val date: String,
    val instagramMs: Long = 0L,
    val youtubeMs: Long = 0L,
    /** Aantal keer dat je op "Vervanger doen" drukte. */
    val opensBlocked: Int = 0,
    /** Aantal keer dat je alsnog doorging. */
    val opensPassed: Int = 0,
    val points: Int = 0,
    val streakDays: Int = 0
) {
    val totalMs: Long get() = instagramMs + youtubeMs
    val totalOpens: Int get() = opensBlocked + opensPassed
}
