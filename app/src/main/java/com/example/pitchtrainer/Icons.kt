package com.example.pitchtrainer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A tiny hand-drawn bar-chart glyph, used instead of pulling in the (large)
 * material-icons-extended artifact for a single icon.
 */
@Composable
fun BarChartIcon(tint: Color, modifier: Modifier = Modifier.size(24.dp)) {
    Canvas(modifier = modifier) {
        val barWidth = size.width / 5f
        val gap = barWidth / 2f
        val heightFractions = listOf(0.4f, 0.7f, 1.0f)
        heightFractions.forEachIndexed { i, fraction ->
            val barHeight = size.height * fraction
            drawRect(
                color = tint,
                topLeft = Offset(i * (barWidth + gap), size.height - barHeight),
                size = Size(barWidth, barHeight),
            )
        }
    }
}
