package com.tkriek.scrollless.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tkriek.scrollless.data.UsageStatsHelper
import com.tkriek.scrollless.service.ScrollGuardAccessibilityService
import com.tkriek.scrollless.ui.theme.ScrollLessTheme

class MainActivity : ComponentActivity() {

    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ScrollLessTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                DashboardScreen(
                    state = state,
                    onOpenUsageAccess = { safeStart(UsageStatsHelper.permissionIntent(this)) },
                    onOpenAccessibility = { safeStart(ScrollGuardAccessibilityService.settingsIntent()) },
                    onOpenBatterySettings = { safeStart(batteryOptimizationIntent()) },
                    onOpenOverlaySettings = { safeStart(overlayIntent()) },
                    onDailyLimitChange = viewModel::setDailyOpenLimit,
                    onWaitSecondsChange = viewModel::setWaitSeconds,
                    onGuardEnabledChange = viewModel::setGuardEnabled
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Special-access permissies worden buiten de app toegekend, dus opnieuw checken.
        viewModel.refresh()
    }

    private fun batteryOptimizationIntent(): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Bewust het lijstscherm en niet het directe verzoek-dialoog: de instelling
            // die de service in leven houdt ("Onbeperkt") staat bij de app zelf.
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        } else {
            appDetailsIntent()
        }

    private fun overlayIntent(): Intent =
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.fromParts("package", packageName, null)
        )

    private fun appDetailsIntent(): Intent =
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        )

    private fun safeStart(intent: Intent) {
        runCatching { startActivity(intent) }
            .onFailure { runCatching { startActivity(appDetailsIntent()) } }
    }
}
