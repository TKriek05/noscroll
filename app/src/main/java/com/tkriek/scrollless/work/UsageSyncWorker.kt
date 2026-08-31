package com.tkriek.scrollless.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.tkriek.scrollless.data.ScrollLessRepository
import java.util.concurrent.TimeUnit

/**
 * Schrijft elke 15 minuten de schermtijd van vandaag (en gisteren) weg naar
 * DailyStat, zodat het dashboard ook klopt als je de app dagen niet opent.
 * 15 minuten is het minimum dat WorkManager toestaat.
 */
class UsageSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        val repository = ScrollLessRepository.get(applicationContext)
        repository.refreshRecentDays()
        repository.pruneOldEvents()
        Result.success()
    } catch (e: Exception) {
        Result.retry()
    }

    companion object {
        private const val UNIQUE_NAME = "usage-sync"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<UsageSyncWorker>(15, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
