package com.example.tonetrainer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

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

/** A tiny hand-drawn pause glyph - Icons.Default.Pause isn't in the core icon set we depend on. */
@Composable
fun PauseIcon(tint: Color, modifier: Modifier = Modifier.size(24.dp)) {
    Canvas(modifier = modifier) {
        val barWidth = size.width * 0.3f
        val gap = size.width * 0.15f
        val startX = (size.width - 2 * barWidth - gap) / 2f
        listOf(0, 1).forEach { i ->
            drawRect(
                color = tint,
                topLeft = Offset(startX + i * (barWidth + gap), 0f),
                size = Size(barWidth, size.height),
            )
        }
    }
}

/** A tiny hand-drawn play glyph, to match PauseIcon without pulling in material-icons-extended. */
@Composable
fun PlayIcon(tint: Color, modifier: Modifier = Modifier.size(24.dp)) {
    Canvas(modifier = modifier) {
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(size.width * 0.22f, 0f)
            lineTo(size.width * 0.22f, size.height)
            lineTo(size.width * 0.82f, size.height / 2f)
            close()
        }
        drawPath(path, color = tint)
    }
}

/** Draws a small filled triangle at the endpoint of a circular arc, tangent to the circle so it
 * reads as an arrowhead continuing the arc's direction of travel. */
private fun DrawScope.drawArrowAtArcEnd(center: Offset, radius: Float, endAngleDeg: Float, tint: Color, arrowLen: Float, arrowWidth: Float) {
    val endAngleRad = Math.toRadians(endAngleDeg.toDouble())
    val tangentAngleRad = endAngleRad + Math.PI / 2.0
    val point = Offset(center.x + radius * cos(endAngleRad).toFloat(), center.y + radius * sin(endAngleRad).toFloat())
    val tangent = Offset(cos(tangentAngleRad).toFloat(), sin(tangentAngleRad).toFloat())
    val normal = Offset(-tangent.y, tangent.x)
    val tip = Offset(point.x + tangent.x * arrowLen * 0.5f, point.y + tangent.y * arrowLen * 0.5f)
    val base = Offset(point.x - tangent.x * arrowLen * 0.5f, point.y - tangent.y * arrowLen * 0.5f)
    val path = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(base.x + normal.x * arrowWidth, base.y + normal.y * arrowWidth)
        lineTo(base.x - normal.x * arrowWidth, base.y - normal.y * arrowWidth)
        close()
    }
    drawPath(path, color = tint)
}

/** A tiny hand-drawn cycle glyph - two arcs forming a broken circle, each ending in an arrowhead,
 * used for the toggle between two values, to match the other hand-drawn icons here without
 * pulling in material-icons-extended. */
@Composable
fun CycleIcon(tint: Color, modifier: Modifier = Modifier.size(24.dp)) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.13f
        val radius = size.minDimension / 2f - strokeWidth
        val center = Offset(size.width / 2f, size.height / 2f)
        val arrowLen = size.minDimension * 0.26f
        val arrowWidth = size.minDimension * 0.15f

        listOf(-135f, 45f).forEach { startAngle ->
            val sweepAngle = 120f
            drawArc(
                color = tint,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            drawArrowAtArcEnd(center, radius, startAngle + sweepAngle, tint, arrowLen, arrowWidth)
        }
    }
}
