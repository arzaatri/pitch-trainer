package com.example.tonetrainer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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

/**
 * Oval toggle shown below the pause button on Tune's post-submission screen, for switching the
 * reference tone that's playing between the correct note and the note the user actually tuned to.
 */
@Composable
fun GuessToggleButton(isShowingCorrect: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onToggle,
        modifier = modifier.fillMaxWidth(0.5f).height(40.dp),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isShowingCorrect) AccentGold else SurfaceGray,
        ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            CycleIcon(
                tint = if (isShowingCorrect) DarkGray else Color.White,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                if (isShowingCorrect) "Correct" else "Your guess",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isShowingCorrect) DarkGray else Color.White,
            )
        }
    }
}
