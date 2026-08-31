package com.tkriek.scrollless.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.tkriek.scrollless.data.entities.AppOpenEvent
import com.tkriek.scrollless.data.entities.DailyStat
import kotlinx.coroutines.flow.Flow

@Dao
interface AppOpenEventDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(event: AppOpenEvent): Long

    @Query("SELECT COUNT(*) FROM app_open_event WHERE date = :date AND outcome = :outcome")
    suspend fun countByOutcome(date: String, outcome: String): Int

    @Query("SELECT * FROM app_open_event WHERE date = :date ORDER BY timestamp DESC")
    fun observeForDate(date: String): Flow<List<AppOpenEvent>>

    @Query("SELECT * FROM app_open_event ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<AppOpenEvent>>

    @Query("DELETE FROM app_open_event WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long): Int
}

@Dao
interface DailyStatDao {

    @Upsert
    suspend fun upsert(stat: DailyStat)

    @Query("SELECT * FROM daily_stat WHERE date = :date")
    suspend fun get(date: String): DailyStat?

    @Query("SELECT * FROM daily_stat WHERE date = :date")
    fun observe(date: String): Flow<DailyStat?>

    @Query("SELECT * FROM daily_stat WHERE date BETWEEN :from AND :to ORDER BY date ASC")
    fun observeRange(from: String, to: String): Flow<List<DailyStat>>

    @Query("SELECT * FROM daily_stat WHERE date <= :date ORDER BY date DESC LIMIT :limit")
    suspend fun historyBefore(date: String, limit: Int): List<DailyStat>

    @Query("SELECT COALESCE(SUM(points), 0) FROM daily_stat")
    fun observeTotalPoints(): Flow<Int>
}
