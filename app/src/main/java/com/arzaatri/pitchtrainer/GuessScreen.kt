package com.arzaatri.pitchtrainer

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val OCTAVE_LABELS = OCTAVE_RANGE.map { it.toString() }

@Composable
fun GuessScreen(vm: GuessViewModel) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    if (isLandscape) GuessScreenLandscape(vm) else GuessScreenPortrait(vm)
}

@Composable
private fun GuessScreenPortrait(vm: GuessViewModel) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
    ) {
        if (!vm.isComplete) {
            Text("Listen, then guess the note", fontSize = 20.sp, color = Color.LightGray, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                WheelPicker(
                    items = TONES.map { displayName(it) },
                    selectedIndex = vm.guessToneIndex,
                    onSelectedIndexChange = { vm.selectGuessTone(it) },
                    accentColor = AccentGold,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                )
                WheelPicker(
                    items = OCTAVE_LABELS,
                    selectedIndex = vm.guessOctave - OCTAVE_RANGE.first,
                    onSelectedIndexChange = { vm.selectGuessOctave(OCTAVE_RANGE.first + it) },
                    accentColor = AccentGold,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    scrollEnabled = !vm.isEasyMode,
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = { vm.submit() },
                enabled = !vm.isPaused,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceGray),
            ) {
                Text("SUBMIT GUESS", fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(12.dp))

            PauseButton(isPaused = vm.isPaused, onToggle = { vm.togglePause() })
        } else {
            val (message, color) = guessFeedbackDisplay(vm.feedback)

            Text(message, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = color)

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "It was ${vm.targetNoteName}",
                fontSize = 20.sp,
                color = Color.LightGray,
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { vm.nextNote() },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
            ) {
                Text("CONTINUE", fontWeight = FontWeight.ExtraBold, color = DarkGray)
            }

            Spacer(modifier = Modifier.height(12.dp))

            PauseButton(isPaused = vm.isPaused, onToggle = { vm.togglePause() })

            Spacer(modifier = Modifier.height(12.dp))

            GuessToggleButton(isShowingCorrect = vm.isShowingCorrectTone, onToggle = { vm.toggleCorrectTone() })
        }
    }
}

/** See TuneScreenLandscape's doc comment - same rationale: the wheel pickers (or the revealed
 * answer) sit on the left, the primary action button stacks above Pause and the post-submission
 * correct/guessed toggle on the right. The wheel pickers also shrink from 5 to 3 visible rows
 * here since landscape has much less height to spare. */
@Composable
private fun GuessScreenLandscape(vm: GuessViewModel) {
    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (!vm.isComplete) {
                Text("Listen, then guess the note", fontSize = 15.sp, color = Color.LightGray, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    WheelPicker(
                        items = TONES.map { displayName(it) },
                        selectedIndex = vm.guessToneIndex,
                        onSelectedIndexChange = { vm.selectGuessTone(it) },
                        accentColor = AccentGold,
                        modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
                        visibleRows = 3,
                    )
                    WheelPicker(
                        items = OCTAVE_LABELS,
                        selectedIndex = vm.guessOctave - OCTAVE_RANGE.first,
                        onSelectedIndexChange = { vm.selectGuessOctave(OCTAVE_RANGE.first + it) },
                        accentColor = AccentGold,
                        modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
                        scrollEnabled = !vm.isEasyMode,
                        visibleRows = 3,
                    )
                }
            } else {
                val (message, color) = guessFeedbackDisplay(vm.feedback)

                Text(message, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = color)

                Spacer(modifier = Modifier.height(8.dp))

                Text("It was ${vm.targetNoteName}", fontSize = 15.sp, color = Color.LightGray)
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (!vm.isComplete) {
                Button(
                    onClick = { vm.submit() },
                    enabled = !vm.isPaused,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceGray),
                ) {
                    Text("SUBMIT GUESS", fontWeight = FontWeight.Bold, color = Color.White)
                }
            } else {
                Button(
                    onClick = { vm.nextNote() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                ) {
                    Text("CONTINUE", fontWeight = FontWeight.ExtraBold, color = DarkGray)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            PauseButton(isPaused = vm.isPaused, onToggle = { vm.togglePause() })

            if (vm.isComplete) {
                Spacer(modifier = Modifier.height(10.dp))
                GuessToggleButton(isShowingCorrect = vm.isShowingCorrectTone, onToggle = { vm.toggleCorrectTone() })
            }
        }
    }
}

private fun guessFeedbackDisplay(feedback: GuessFeedback): Pair<String, Color> = when (feedback) {
    GuessFeedback.CORRECT -> "Correct!" to CorrectGreen
    GuessFeedback.CLOSE -> "Close!" to CloseYellow
    GuessFeedback.WRONG -> "Wrong" to WrongRed
    GuessFeedback.NONE -> "" to Color.White
}
