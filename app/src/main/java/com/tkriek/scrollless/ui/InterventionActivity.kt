package com.tkriek.scrollless.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tkriek.scrollless.ScrollLessApp
import com.tkriek.scrollless.data.ScrollLessRepository
import com.tkriek.scrollless.data.Settings
import com.tkriek.scrollless.data.TrackedApp
import com.tkriek.scrollless.data.entities.AppOpenEvent
import com.tkriek.scrollless.ui.theme.ScrollLessTheme
import com.tkriek.scrollless.util.Alternative
import com.tkriek.scrollless.util.Alternatives
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Het bewuste moment: verschijnt over Instagram/YouTube heen, laat je een paar
 * seconden wachten en biedt een concrete vervanger aan.
 */
class InterventionActivity : ComponentActivity() {

    private lateinit var settings: Settings
    private lateinit var repository: ScrollLessRepository
    private var handled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        showOverLockScreen()

        settings = Settings(this)
        repository = ScrollLessRepository.get(this)

        val app = TrackedApp.fromPackage(intent.getStringExtra(EXTRA_PACKAGE)) ?: run {
            finish()
            return
        }

        setContent {
            ScrollLessTheme(darkTheme = true) {
                InterventionScreen(
                    app = app,
                    waitSeconds = settings.waitSeconds,
                    onProceed = { onProceed(app) },
                    onAbort = { onAbort(app) }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    /**
     * Wegzwiepen naar het startscherm is ook een keuze: dan ging je niet door.
     * De activity heeft noHistory=true, dus dit is echt het einde van dit scherm.
     */
    override fun onStop() {
        super.onStop()
        if (handled || isChangingConfigurations) return
        val app = TrackedApp.fromPackage(intent.getStringExtra(EXTRA_PACKAGE)) ?: return
        handled = true
        log(app, AppOpenEvent.OUTCOME_ABORTED)
    }

    private fun onProceed(app: TrackedApp) {
        if (handled) return
        handled = true
        settings.startGracePeriod()
        log(app, AppOpenEvent.OUTCOME_PASSED)
        // Terug naar de app die je opende; die staat nog in de vorige task.
        finish()
    }

    private fun onAbort(app: TrackedApp) {
        if (handled) return
        handled = true
        settings.endGracePeriod()
        log(app, AppOpenEvent.OUTCOME_ABORTED)
        goHome()
        finish()
    }

    private fun log(app: TrackedApp, outcome: String) {
        // Bewust niet lifecycleScope: dit scherm sluit meteen na de keuze en de
        // schrijfactie mag daar niet mee gecanceld worden.
        ScrollLessApp.applicationScope.launch {
            runCatching { repository.logOutcome(app.packageName, outcome) }
        }
    }

    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
    }

    companion object {
        private const val EXTRA_PACKAGE = "extra_package"

        fun intentFor(context: Context, app: TrackedApp): Intent =
            Intent(context, InterventionActivity::class.java)
                .putExtra(EXTRA_PACKAGE, app.packageName)
    }
}

@Composable
private fun InterventionScreen(
    app: TrackedApp,
    waitSeconds: Int,
    onProceed: () -> Unit,
    onAbort: () -> Unit
) {
    var secondsLeft by remember { mutableIntStateOf(waitSeconds) }
    var alternative by remember { mutableStateOf<Alternative>(Alternatives.random()) }

    LaunchedEffect(waitSeconds) {
        while (secondsLeft > 0) {
            delay(1_000)
            secondsLeft--
        }
    }

    val progress by animateFloatAsState(
        targetValue = if (waitSeconds == 0) 1f else 1f - (secondsLeft.toFloat() / waitSeconds),
        label = "wait"
    )

    // Terug = je laat het erbij; dat telt als afgebroken.
    BackHandler { onAbort() }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Even wachten",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Je opende ${app.label}",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Wat wilde je hier eigenlijk doen?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            text = "In plaats daarvan",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = alternative.title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = alternative.detail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "~${alternative.minutes} min",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { alternative = Alternatives.randomOtherThan(alternative) }) {
                    Text("Andere suggestie")
                }
            }

            Column {
                Button(
                    onClick = onAbort,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Dit ga ik doen", style = MaterialTheme.typography.titleMedium)
                }

                Spacer(Modifier.height(12.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = onProceed,
                        enabled = secondsLeft <= 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(
                            text = if (secondsLeft > 0) {
                                "Toch doorgaan ($secondsLeft)"
                            } else {
                                "Toch doorgaan naar ${app.label}"
                            },
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(MaterialTheme.colorScheme.surface)
                )
            }
        }
    }
}
