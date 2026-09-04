package com.example.pitchtrainer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val OCTAVE_LABELS = OCTAVE_RANGE.map { it.toString() }

@Composable
fun GuessScreen(vm: GuessViewModel) {
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
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceGray),
            ) {
                Text("SUBMIT GUESS", fontWeight = FontWeight.Bold, color = Color.White)
            }
        } else {
            val (message, color) = when (vm.feedback) {
                GuessFeedback.CORRECT -> "Correct!" to CorrectGreen
                GuessFeedback.CLOSE -> "Close!" to CloseYellow
                GuessFeedback.WRONG -> "Wrong" to WrongRed
                GuessFeedback.NONE -> "" to Color.White
            }

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
        }
    }
}
