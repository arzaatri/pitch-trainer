package com.example.tonetrainer

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TuneScreen(vm: TuneViewModel) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    if (isLandscape) TuneScreenLandscape(vm) else TuneScreenPortrait(vm)
}

@Composable
private fun TuneScreenPortrait(vm: TuneViewModel) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
    ) {
        Text(
            text = vm.targetNoteName,
            fontSize = 80.sp,
            fontWeight = FontWeight.ExtraBold,
            color = AccentGold,
        )

        Text(
            text = vm.feedback,
            fontSize = 18.sp,
            color = tuneFeedbackColor(vm.feedback),
        )

        Spacer(modifier = Modifier.height(60.dp))

        if (!vm.isComplete) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                OutlinedButton(
                    onClick = { vm.adjustPitch(-1) },
                    modifier = Modifier.size(110.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(2.dp, AccentGold),
                ) {
                    Text("-${vm.difficulty.moveStepCents}¢", fontSize = 22.sp, color = AccentGold)
                }

                OutlinedButton(
                    onClick = { vm.adjustPitch(1) },
                    modifier = Modifier.size(110.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(2.dp, AccentGold),
                ) {
                    Text("+${vm.difficulty.moveStepCents}¢", fontSize = 22.sp, color = AccentGold)
                }
            }

            Spacer(modifier = Modifier.height(60.dp))

            Button(
                onClick = { vm.submit() },
                enabled = !vm.isPaused,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceGray),
            ) {
                Text("CHECK PITCH", fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(12.dp))

            PauseButton(isPaused = vm.isPaused, onToggle = { vm.togglePause() })
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Now playing perfect reference...",
                    color = Color.LightGray,
                    fontSize = 14.sp,
                )

                Spacer(modifier = Modifier.height(24.dp))

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
}

/**
 * Landscape has far less vertical room than horizontal, so a plain vertical stack (the portrait
 * layout) pushes the lower buttons off the bottom of the screen. Instead the screen splits into
 * two columns: note name/feedback/pitch controls on the left, and the primary action button
 * (CHECK PITCH / CONTINUE) stacked above Pause and the post-submission correct/guess toggle on
 * the right. Both columns are scrollable as a safety net so nothing is ever unreachable even on
 * a very short landscape screen.
 */
@Composable
private fun TuneScreenLandscape(vm: TuneViewModel) {
    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = vm.targetNoteName,
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                color = AccentGold,
            )

            Text(
                text = vm.feedback,
                fontSize = 14.sp,
                color = tuneFeedbackColor(vm.feedback),
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (!vm.isComplete) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { vm.adjustPitch(-1) },
                        modifier = Modifier.width(84.dp).height(64.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(2.dp, AccentGold),
                    ) {
                        Text("-${vm.difficulty.moveStepCents}¢", fontSize = 15.sp, color = AccentGold, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = { vm.adjustPitch(1) },
                        modifier = Modifier.width(84.dp).height(64.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(2.dp, AccentGold),
                    ) {
                        Text("+${vm.difficulty.moveStepCents}¢", fontSize = 15.sp, color = AccentGold, maxLines = 1)
                    }
                }
            } else {
                Text("Now playing perfect reference...", color = Color.LightGray, fontSize = 12.sp)
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
                    Text("CHECK PITCH", fontWeight = FontWeight.Bold, color = Color.White)
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

private fun tuneFeedbackColor(feedback: String): Color = when {
    feedback == "Perfect Match!" -> CorrectGreen
    feedback.startsWith("Close!") -> CloseYellow
    else -> Color.LightGray
}
