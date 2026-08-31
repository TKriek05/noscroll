package com.tkriek.scrollless.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.tkriek.scrollless.data.dao.AppOpenEventDao
import com.tkriek.scrollless.data.dao.DailyStatDao
import com.tkriek.scrollless.data.entities.AppOpenEvent
import com.tkriek.scrollless.data.entities.DailyStat

@Database(
    entities = [AppOpenEvent::class, DailyStat::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appOpenEventDao(): AppOpenEventDao
    abstract fun dailyStatDao(): DailyStatDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "scrollless.db"
                ).build().also { instance = it }
            }
    }
}
