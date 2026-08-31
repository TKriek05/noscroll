package com.tkriek.scrollless.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.tkriek.scrollless.data.Settings as ScrollLessSettings
import com.tkriek.scrollless.data.TrackedApp
import com.tkriek.scrollless.ui.InterventionActivity

/**
 * Kijkt mee met venster-wissels en start het interventiescherm zodra Instagram of
 * YouTube naar de voorgrond komt.
 *
 * De service leest géén scherminhoud (canRetrieveWindowContent=false) en krijgt
 * dankzij android:packageNames in accessibility_service_config.xml alleen events
 * van de twee gevolgde apps.
 *
 * Let op: zet ScrollLess op "Onbeperkt" bij batterijgebruik, anders pauzeert
 * Android deze service na verloop van tijd.
 */
class ScrollGuardAccessibilityService : AccessibilityService() {

    private lateinit var settings: ScrollLessSettings

    /** Laatste keer dat we voor deze package een scherm toonden. */
    private var lastTriggerAt = 0L
    private var lastTriggerPackage: String? = null

    override fun onCreate() {
        super.onCreate()
        settings = ScrollLessSettings(this)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "ScrollGuard actief voor ${TrackedApp.packageNames}")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return
        val app = TrackedApp.fromPackage(packageName) ?: return

        if (!settings.guardEnabled) return

        val now = System.currentTimeMillis()

        // Net "toch doorgaan" gekozen: even niet zeuren.
        if (settings.isQuiet(now)) return

        // Binnen een app vuren schermwissels vaak achter elkaar; één scherm is genoeg.
        if (packageName == lastTriggerPackage && now - lastTriggerAt < RETRIGGER_COOLDOWN_MS) return

        lastTriggerPackage = packageName
        lastTriggerAt = now

        startActivity(
            InterventionActivity.intentFor(this, app).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION
                )
            }
        )
    }

    override fun onInterrupt() = Unit

    companion object {
        private const val TAG = "ScrollGuard"

        /**
         * Korte cooldown tegen dubbele events van dezelfde app-opening. Het echte
         * "laat me even met rust" gaat via Settings.gracePeriod.
         */
        private const val RETRIGGER_COOLDOWN_MS = 3_000L

        fun isEnabled(context: Context): Boolean {
            val expected = ComponentName(context, ScrollGuardAccessibilityService::class.java)
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val splitter = TextUtils.SimpleStringSplitter(':')
            splitter.setString(enabledServices)
            while (splitter.hasNext()) {
                val component = ComponentName.unflattenFromString(splitter.next())
                if (component != null && component == expected) return true
            }
            return false
        }

        fun settingsIntent(): Intent =
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
