package com.example.tonetrainer

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Oval pause/resume control shown below a panel's submit/continue button. */
@Composable
fun PauseButton(isPaused: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onToggle,
        modifier = modifier.fillMaxWidth(0.5f).height(40.dp),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(containerColor = SurfaceGray),
    ) {
        if (isPaused) {
            PlayIcon(tint = Color.White, modifier = Modifier.size(16.dp))
        } else {
            PauseIcon(tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}
