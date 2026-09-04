package com.example.pitchtrainer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

enum class Panel { TUNE, GUESS }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = AccentGold,
                    background = DarkGray,
                    surface = SurfaceGray,
                ),
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PitchApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PitchApp(
    tuneVm: TuneViewModel = viewModel(),
    guessVm: GuessViewModel = viewModel(),
) {
    var panel by remember { mutableStateOf(Panel.TUNE) }
    var showSettings by remember { mutableStateOf(false) }
    var showAnalytics by remember { mutableStateOf(false) }
    val settingsSheetState = rememberModalBottomSheetState()
    val analyticsSheetState = rememberModalBottomSheetState()
    val context = LocalContext.current

    LaunchedEffect(panel) {
        when (panel) {
            Panel.TUNE -> {
                guessVm.onHidden()
                tuneVm.onVisible()
            }
            Panel.GUESS -> {
                tuneVm.onHidden()
                guessVm.onVisible()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            tuneVm.onHidden()
            guessVm.onHidden()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkGray)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PanelButton("Tune", panel == Panel.TUNE, Modifier.weight(1f)) { panel = Panel.TUNE }
                PanelButton("Guess", panel == Panel.GUESS, Modifier.weight(1f)) { panel = Panel.GUESS }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (panel) {
                Panel.TUNE -> {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Difficulty.entries.forEach { d ->
                            ModeButton(d.label, tuneVm.difficulty == d, Modifier.weight(1f)) { tuneVm.selectDifficulty(d) }
                        }
                    }
                }
                Panel.GUESS -> {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ModeButton("Normal", !guessVm.isEasyMode, Modifier.weight(1f)) {
                            if (guessVm.isEasyMode) guessVm.toggleEasyMode()
                        }
                        ModeButton("Easy", guessVm.isEasyMode, Modifier.weight(1f)) {
                            if (!guessVm.isEasyMode) guessVm.toggleEasyMode()
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = { showAnalytics = true }) {
                    BarChartIcon(tint = Color.White, modifier = Modifier.size(22.dp))
                }
                IconButton(onClick = { showSettings = true }) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (panel) {
                    Panel.TUNE -> TuneScreen(tuneVm)
                    Panel.GUESS -> GuessScreen(guessVm)
                }
            }
        }

        if (showSettings) {
            when (panel) {
                Panel.TUNE -> SettingsSheet(
                    title = "Tune Settings",
                    active = tuneVm.settings,
                    onToggleCell = tuneVm::toggleCell,
                    onToggleRow = tuneVm::toggleRow,
                    onToggleColumn = tuneVm::toggleColumn,
                    onReset = tuneVm::resetSettings,
                    onDismiss = { showSettings = false },
                    sheetState = settingsSheetState,
                    context = context,
                )
                Panel.GUESS -> SettingsSheet(
                    title = "Guess Settings",
                    active = guessVm.settings,
                    onToggleCell = guessVm::toggleCell,
                    onToggleRow = guessVm::toggleRow,
                    onToggleColumn = guessVm::toggleColumn,
                    onReset = guessVm::resetSettings,
                    onDismiss = { showSettings = false },
                    sheetState = settingsSheetState,
                    context = context,
                )
            }
        }

        if (showAnalytics) {
            when (panel) {
                Panel.TUNE -> TuneAnalyticsSheet(
                    context = context,
                    onDismiss = { showAnalytics = false },
                    sheetState = analyticsSheetState,
                )
                Panel.GUESS -> GuessAnalyticsSheet(
                    context = context,
                    onDismiss = { showAnalytics = false },
                    sheetState = analyticsSheetState,
                )
            }
        }
    }
}

@Composable
private fun PanelButton(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) AccentGold else SurfaceGray,
            contentColor = if (selected) DarkGray else Color.White,
        ),
    ) {
        Text(label, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ModeButton(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) AccentGold.copy(alpha = 0.15f) else Color.Transparent,
            contentColor = if (selected) AccentGold else Color.LightGray,
        ),
    ) {
        Text(label, fontSize = 13.sp)
    }
}
