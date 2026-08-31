package com.tkriek.scrollless.ui

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.provider.Settings as AndroidSettings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tkriek.scrollless.data.ScrollLessRepository
import com.tkriek.scrollless.data.Settings
import com.tkriek.scrollless.data.UsageStatsHelper
import com.tkriek.scrollless.data.entities.DailyStat
import com.tkriek.scrollless.service.ScrollGuardAccessibilityService
import com.tkriek.scrollless.util.DateUtils
import com.tkriek.scrollless.util.Gamification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class PermissionState(
    val usageAccess: Boolean = false,
    val accessibility: Boolean = false,
    val batteryUnrestricted: Boolean = false,
    val overlay: Boolean = false
) {
    /** Zonder deze twee doet de app niets zinnigs. */
    val essentialsGranted: Boolean get() = usageAccess && accessibility
    val allGranted: Boolean get() = essentialsGranted && batteryUnrestricted && overlay
}

data class DayBar(
    val date: LocalDate,
    val label: String,
    val millis: Long,
    val isToday: Boolean
)

data class DashboardUiState(
    val today: DailyStat = DailyStat(date = DateUtils.todayKey()),
    val week: List<DayBar> = emptyList(),
    val weekTotalMs: Long = 0L,
    val totalPoints: Int = 0,
    val permissions: PermissionState = PermissionState(),
    val dailyOpenLimit: Int = Settings.DEFAULT_DAILY_LIMIT,
    val waitSeconds: Int = Settings.DEFAULT_WAIT_SECONDS,
    val guardEnabled: Boolean = true,
    val nextMilestone: Int? = null
)

class DashboardViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = ScrollLessRepository.get(app)
    private val settings = Settings(app)
    private val permissions = MutableStateFlow(readPermissions())

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.observeToday(),
        repository.observeWeek(),
        repository.observeTotalPoints(),
        permissions,
        settings.changes()
    ) { today, week, points, perms, prefs ->
        val todayStat = today ?: DailyStat(date = DateUtils.todayKey())
        val byDate = week.associateBy { it.date }
        val bars = DateUtils.weekEnding().map { date ->
            val key = DateUtils.key(date)
            DayBar(
                date = date,
                label = DateUtils.shortDayLabel(date),
                millis = byDate[key]?.totalMs ?: 0L,
                isToday = key == DateUtils.todayKey()
            )
        }
        DashboardUiState(
            today = todayStat,
            week = bars,
            weekTotalMs = bars.sumOf { it.millis },
            totalPoints = points,
            permissions = perms,
            dailyOpenLimit = prefs.dailyOpenLimit,
            waitSeconds = prefs.waitSeconds,
            guardEnabled = prefs.guardEnabled,
            nextMilestone = Gamification.nextMilestone(todayStat.streakDays)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState()
    )

    /** Bij elke terugkeer naar de app: permissies opnieuw checken en cijfers verversen. */
    fun refresh() {
        permissions.value = readPermissions()
        viewModelScope.launch {
            runCatching { repository.refreshRecentDays() }
        }
    }

    fun setDailyOpenLimit(value: Int) {
        settings.dailyOpenLimit = value
        refresh()
    }

    fun setWaitSeconds(value: Int) {
        settings.waitSeconds = value
    }

    fun setGuardEnabled(enabled: Boolean) {
        settings.guardEnabled = enabled
        if (enabled) settings.endGracePeriod()
    }

    private fun readPermissions(): PermissionState {
        val context: Context = getApplication()
        return PermissionState(
            usageAccess = UsageStatsHelper.hasPermission(context),
            accessibility = ScrollGuardAccessibilityService.isEnabled(context),
            batteryUnrestricted = isIgnoringBatteryOptimizations(context),
            overlay = AndroidSettings.canDrawOverlays(context)
        )
    }

    private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return power.isIgnoringBatteryOptimizations(context.packageName)
    }
}
