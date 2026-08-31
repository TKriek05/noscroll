package com.tkriek.scrollless.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Eén poging om een gevolgde app te openen, plus wat je daarna deed.
 */
@Entity(tableName = "app_open_event")
data class AppOpenEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val packageName: String,
    val timestamp: Long,
    /** [OUTCOME_PASSED] of [OUTCOME_ABORTED]. */
    val outcome: String,
    /** Datum als "yyyy-MM-dd", zodat we per dag kunnen tellen zonder tijdzone-gedoe in SQL. */
    val date: String
) {
    companion object {
        const val OUTCOME_PASSED = "doorgegaan"
        const val OUTCOME_ABORTED = "afgebroken"
    }
}
