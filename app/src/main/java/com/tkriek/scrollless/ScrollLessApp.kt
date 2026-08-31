package com.tkriek.scrollless

import android.app.Application
import com.tkriek.scrollless.work.UsageSyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class ScrollLessApp : Application() {

    override fun onCreate() {
        super.onCreate()
        UsageSyncWorker.schedule(this)
    }

    companion object {
        /**
         * Voor schrijfacties die af moeten maken ook als de activity al weg is —
         * het interventiescherm sluit zichzelf direct na je keuze.
         */
        val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
