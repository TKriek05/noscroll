package com.tkriek.scrollless.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tkriek.scrollless.data.TrackedApp
import com.tkriek.scrollless.util.DateUtils
import com.tkriek.scrollless.util.Gamification

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onOpenUsageAccess: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onDailyLimitChange: (Int) -> Unit,
    onWaitSecondsChange: (Int) -> Unit,
    onGuardEnabledChange: (Boolean) -> Unit
) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Header(state) }

            if (!state.permissions.allGranted) {
                item {
                    SetupCard(
                        state = state,
                        onOpenUsageAccess = onOpenUsageAccess,
                        onOpenAccessibility = onOpenAccessibility,
                        onOpenBatterySettings = onOpenBatterySettings,
                        onOpenOverlaySettings = onOpenOverlaySettings
                    )
                }
            }

            item { StreakCard(state) }
            item { TodayCard(state) }
            item { WeekCard(state) }
            item {
                SettingsCard(
                    state = state,
                    onDailyLimitChange = onDailyLimitChange,
                    onWaitSecondsChange = onWaitSecondsChange,
                    onGuardEnabledChange = onGuardEnabledChange
                )
            }
        }
    }
}

@Composable
private fun Header(state: DashboardUiState) {
    Column {
        Text(
            text = "ScrollLess",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = if (state.guardEnabled) "Bewaker staat aan" else "Bewaker staat uit",
            style = MaterialTheme.typography.labelMedium,
            color = if (state.guardEnabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            }
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SetupCard(
    state: DashboardUiState,
    onOpenUsageAccess: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit
) {
    SectionCard(title = "Eerst instellen") {
        SetupRow(
            done = state.permissions.usageAccess,
            title = "Toegang tot gebruiksgegevens",
            detail = "Nodig om schermtijd te kunnen lezen.",
            onClick = onOpenUsageAccess
        )
        SetupRow(
            done = state.permissions.accessibility,
            title = "Toegankelijkheidsdienst",
            detail = "Nodig om te merken dat je Instagram of YouTube opent.",
            onClick = onOpenAccessibility
        )
        SetupRow(
            done = state.permissions.overlay,
            title = "Over andere apps tonen",
            detail = "Zorgt dat het tussenscherm ook echt bovenop komt.",
            onClick = onOpenOverlaySettings
        )
        SetupRow(
            done = state.permissions.batteryUnrestricted,
            title = "Batterij onbeperkt",
            detail = "Anders pauzeert Android de bewaker na een tijd.",
            onClick = onOpenBatterySettings
        )
    }
}

@Composable
private fun SetupRow(
    done: Boolean,
    title: String,
    detail: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (done) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (!done) {
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onClick) { Text("Open") }
        }
    }
}

@Composable
private fun StreakCard(state: DashboardUiState) {
    val streak = state.today.streakDays
    SectionCard(title = "Streak") {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "$streak",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (streak == 1) "goede dag" else "goede dagen",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        val milestone = state.nextMilestone
        Text(
            text = if (milestone != null) {
                "Nog ${milestone - streak} dag(en) tot de mijlpaal van $milestone " +
                    "(+${Gamification.streakBonuses[milestone]} punten)."
            } else {
                "Alle mijlpalen gehaald. Doorgaan dus."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Row {
            Metric(label = "Punten vandaag", value = "${state.today.points}")
            Spacer(Modifier.width(24.dp))
            Metric(label = "Punten totaal", value = "${state.totalPoints}")
        }
    }
}

@Composable
private fun TodayCard(state: DashboardUiState) {
    val today = state.today
    SectionCard(title = "Vandaag") {
        Text(
            text = DateUtils.formatDuration(today.totalMs),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${TrackedApp.INSTAGRAM.label} ${DateUtils.formatDuration(today.instagramMs)} · " +
                "${TrackedApp.YOUTUBE.label} ${DateUtils.formatDuration(today.youtubeMs)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Row {
            Metric(label = "Afgebroken", value = "${today.opensBlocked}")
            Spacer(Modifier.width(24.dp))
            Metric(
                label = "Doorgegaan",
                value = "${today.opensPassed}/${state.dailyOpenLimit}",
                highlight = today.opensPassed > state.dailyOpenLimit
            )
        }
    }
}

@Composable
private fun WeekCard(state: DashboardUiState) {
    val max = (state.week.maxOfOrNull { it.millis } ?: 0L).coerceAtLeast(1L)
    SectionCard(title = "Deze week") {
        Text(
            text = DateUtils.formatDuration(state.weekTotalMs),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            state.week.forEach { day ->
                val fraction by animateFloatAsState(
                    targetValue = (day.millis.toFloat() / max).coerceIn(0f, 1f),
                    label = "bar-${day.label}"
                )
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((96.dp * fraction).coerceAtLeast(4.dp))
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (day.isToday) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                }
                            )
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = day.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    state: DashboardUiState,
    onDailyLimitChange: (Int) -> Unit,
    onWaitSecondsChange: (Int) -> Unit,
    onGuardEnabledChange: (Boolean) -> Unit
) {
    SectionCard(title = "Instellingen") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Bewaker",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Toon het tussenscherm bij Instagram en YouTube.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = state.guardEnabled, onCheckedChange = onGuardEnabledChange)
        }
        Spacer(Modifier.height(8.dp))
        Stepper(
            label = "Wachttijd",
            value = "${state.waitSeconds}s",
            onDecrease = { onWaitSecondsChange((state.waitSeconds - 2).coerceAtLeast(0)) },
            onIncrease = { onWaitSecondsChange((state.waitSeconds + 2).coerceAtMost(60)) }
        )
        Stepper(
            label = "Doorgegane opens per dag",
            value = "${state.dailyOpenLimit}",
            onDecrease = { onDailyLimitChange((state.dailyOpenLimit - 1).coerceAtLeast(0)) },
            onIncrease = { onDailyLimitChange((state.dailyOpenLimit + 1).coerceAtMost(20)) }
        )
    }
}

@Composable
private fun Stepper(
    label: String,
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        OutlinedButton(onClick = onDecrease, shape = CircleShape) { Text("−") }
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        OutlinedButton(onClick = onIncrease, shape = CircleShape) { Text("+") }
    }
}

@Composable
private fun Metric(label: String, value: String, highlight: Boolean = false) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = if (highlight) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
