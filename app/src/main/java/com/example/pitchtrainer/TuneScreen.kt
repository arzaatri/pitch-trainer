package com.example.pitchtrainer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TuneScreen(vm: TuneViewModel) {
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
            color = when (vm.feedback) {
                "Perfect Match!" -> CorrectGreen
                "Close!" -> CloseYellow
                else -> Color.LightGray
            },
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
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceGray),
            ) {
                Text("CHECK PITCH", fontWeight = FontWeight.Bold, color = Color.White)
            }
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
            }
        }
    }
}
