package com.arzaatri.pitchtrainer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import kotlin.math.roundToInt

private val BarHeight = 22.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TuneAnalyticsSheet(
    context: Context,
    onDismiss: () -> Unit,
    sheetState: SheetState,
) {
    var mode by remember { mutableStateOf(BreakdownMode.OVERALL) }
    val rows = remember(mode) { StatsStore.tuneRows(context, mode) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = SurfaceGray) {
        Column(modifier = Modifier.fillMaxHeight(0.85f).padding(16.dp)) {
            Text("Adjust Analytics", fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold)
            Legend(listOf("Flat" to FlatPurple, "Correct" to CorrectWhite, "Sharp" to SharpOrange))
            BreakdownSelector(mode) { mode = it }
            Spacer(Modifier.height(12.dp))
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(rows) { row ->
                    val total = row.flat + row.correct + row.sharp
                    val fractions = if (total > 0) {
                        listOf(row.flat.toFloat() / total, row.correct.toFloat() / total, row.sharp.toFloat() / total)
                    } else listOf(0f, 0f, 0f)
                    val percentTexts = listOf(
                        pctLabel(row.flat, total),
                        pctLabel(row.correct, total),
                        pctLabel(row.sharp, total),
                    )
                    val belowTexts = listOf(
                        if (row.flat > 0) "Avg: ${row.avgFlatCents.roundToInt()}c" else null,
                        null,
                        if (row.sharp > 0) "Avg: ${row.avgSharpCents.roundToInt()}c" else null,
                    )
                    StatRow(
                        label = row.label,
                        fractions = fractions,
                        colors = listOf(FlatPurple, CorrectWhite, SharpOrange),
                        percentTexts = percentTexts,
                        belowTexts = belowTexts,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuessAnalyticsSheet(
    context: Context,
    onDismiss: () -> Unit,
    sheetState: SheetState,
) {
    var mode by remember { mutableStateOf(BreakdownMode.OVERALL) }
    val rows = remember(mode) { StatsStore.guessRows(context, mode) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = SurfaceGray) {
        Column(modifier = Modifier.fillMaxHeight(0.85f).padding(16.dp)) {
            Text("Guess Analytics", fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold)
            Legend(listOf("Correct" to CorrectGreen, "Close" to CloseYellow, "Wrong" to WrongRed))
            BreakdownSelector(mode) { mode = it }
            Spacer(Modifier.height(12.dp))
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(rows) { row ->
                    val total = row.correct + row.close + row.wrong
                    val fractions = if (total > 0) {
                        listOf(row.correct.toFloat() / total, row.close.toFloat() / total, row.wrong.toFloat() / total)
                    } else listOf(0f, 0f, 0f)
                    val percentTexts = listOf(
                        pctLabel(row.correct, total),
                        pctLabel(row.close, total),
                        pctLabel(row.wrong, total),
                    )
                    StatRow(
                        label = row.label,
                        fractions = fractions,
                        colors = listOf(CorrectGreen, CloseYellow, WrongRed),
                        percentTexts = percentTexts,
                        belowTexts = null,
                    )
                }
            }
        }
    }
}

@Composable
private fun Legend(entries: List<Pair<String, Color>>) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        entries.forEach { (label, color) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).background(color, RoundedCornerShape(2.dp)))
                Spacer(Modifier.width(4.dp))
                Text(label, color = Color.LightGray, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun BreakdownSelector(mode: BreakdownMode, onModeChange: (BreakdownMode) -> Unit) {
    val options = listOf(
        BreakdownMode.OVERALL to "Overall",
        BreakdownMode.BY_TONE to "By Tone",
        BreakdownMode.BY_OCTAVE to "By Octave",
        BreakdownMode.BY_TONE_OCTAVE to "By Tone+Octave",
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        options.forEach { (value, label) ->
            val selected = mode == value
            Box(
                modifier = Modifier
                    .background(if (selected) AccentGold else DarkGray, RoundedCornerShape(8.dp))
                    .clickable { onModeChange(value) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(label, color = if (selected) DarkGray else Color.White, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    fractions: List<Float>,
    colors: List<Color>,
    percentTexts: List<String?>,
    belowTexts: List<String?>?,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        MidpointLabels(fractions = fractions, texts = percentTexts, colors = colors)
        Row(modifier = Modifier.fillMaxWidth().height(BarHeight)) {
            fractions.forEachIndexed { i, f ->
                if (f > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(f)
                            .fillMaxHeight()
                            .background(colors[i]),
                    )
                }
            }
        }
        if (belowTexts != null) {
            MidpointLabels(fractions = fractions, texts = belowTexts, colors = colors, fontSize = 10.sp)
        }
    }
}

/**
 * Places each non-null label centered over its segment's horizontal midpoint, but clamps the
 * label's x-offset to stay within the bar's own bounds so a thin segment's text can never spill
 * past the screen edge.
 */
@Composable
private fun MidpointLabels(
    fractions: List<Float>,
    texts: List<String?>,
    colors: List<Color>,
    fontSize: TextUnit = 12.sp,
) {
    Layout(
        content = {
            texts.forEachIndexed { i, text ->
                if (text != null) {
                    Text(text, color = colors[i], fontSize = fontSize, fontWeight = FontWeight.Bold)
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val placeables = measurables.map { it.measure(Constraints()) }
        val height = placeables.maxOfOrNull { it.height } ?: 0

        val midpoints = mutableListOf<Int>()
        var cumulative = 0f
        for (i in fractions.indices) {
            val segMid = cumulative + fractions[i] / 2f
            if (texts[i] != null) {
                midpoints.add((segMid * width).roundToInt())
            }
            cumulative += fractions[i]
        }

        layout(width, height) {
            placeables.forEachIndexed { idx, placeable ->
                val midX = midpoints[idx]
                val maxX = (width - placeable.width).coerceAtLeast(0)
                val x = (midX - placeable.width / 2).coerceIn(0, maxX)
                placeable.placeRelative(x, 0)
            }
        }
    }
}

private fun pctLabel(count: Int, total: Int): String? {
    if (total <= 0 || count <= 0) return null
    return "${((count.toFloat() / total) * 100).roundToInt()}%"
}
