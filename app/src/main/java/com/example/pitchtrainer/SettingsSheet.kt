package com.example.pitchtrainer

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LabelCellWidth = 64.dp
private val GridCellSize = 32.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    title: String,
    active: List<Boolean>,
    onToggleCell: (toneIndex: Int, octave: Int) -> Unit,
    onToggleRow: (toneIndex: Int) -> Unit,
    onToggleColumn: (octave: Int) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState,
    context: Context,
) {
    var warningDismissed by remember { mutableStateOf(SettingsStore.isOctave6WarningDismissed(context)) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun runGuarded(wouldEnableCautionOctave: Boolean, action: () -> Unit) {
        if (wouldEnableCautionOctave && !warningDismissed) {
            pendingAction = action
        } else {
            action()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceGray,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.85f)
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold)
                Button(onClick = onReset) { Text("Reset") }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 12.dp),
            ) {
                // Octave header row
                Row {
                    Box(Modifier.width(LabelCellWidth).height(GridCellSize))
                    OCTAVE_RANGE.forEach { octave ->
                        GridHeaderCell(
                            text = "$octave",
                            onClick = {
                                runGuarded(wouldColumnEnableCautionOctave(octave, active)) { onToggleColumn(octave) }
                            },
                        )
                    }
                }

                // One row per tone
                TONES.indices.forEach { toneIndex ->
                    Row {
                        Box(
                            modifier = Modifier
                                .width(LabelCellWidth)
                                .height(GridCellSize)
                                .clickable {
                                    runGuarded(wouldRowEnableCautionOctave(toneIndex, active)) { onToggleRow(toneIndex) }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(displayName(TONES[toneIndex]), color = Color.White, fontSize = 12.sp)
                        }
                        OCTAVE_RANGE.forEach { octave ->
                            val slot = noteSlot(toneIndex, octave)
                            val isActive = active.getOrElse(slot) { false }
                            val isCaution = isCautionSlot(slot)
                            Box(
                                modifier = Modifier
                                    .size(GridCellSize)
                                    .padding(1.dp)
                                    .background(if (isActive) AccentGold else DarkGray)
                                    .clickable {
                                        runGuarded(isCaution && !isActive) { onToggleCell(toneIndex, octave) }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isCaution) {
                                    Text(
                                        "!",
                                        color = WrongRed,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (pendingAction != null) {
        Octave6WarningDialog(
            onConfirm = { dontShowAgain ->
                if (dontShowAgain) {
                    SettingsStore.setOctave6WarningDismissed(context, true)
                    warningDismissed = true
                }
                pendingAction?.invoke()
                pendingAction = null
            },
            onCancel = { pendingAction = null },
        )
    }
}

private fun wouldRowEnableCautionOctave(toneIndex: Int, active: List<Boolean>): Boolean {
    val slots = OCTAVE_RANGE.map { noteSlot(toneIndex, it) }
    val allOn = slots.all { active.getOrElse(it) { false } }
    if (allOn) return false // this tap would turn the row OFF, not on
    return slots.any { isCautionSlot(it) && !active.getOrElse(it) { false } }
}

private fun wouldColumnEnableCautionOctave(octave: Int, active: List<Boolean>): Boolean {
    val slots = TONES.indices.map { noteSlot(it, octave) }
    val allOn = slots.all { active.getOrElse(it) { false } }
    if (allOn) return false // this tap would turn the column OFF, not on
    return slots.any { isCautionSlot(it) && !active.getOrElse(it) { false } }
}

@Composable
private fun Octave6WarningDialog(onConfirm: (dontShowAgain: Boolean) -> Unit, onCancel: () -> Unit) {
    var dontShowAgain by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Heads up") },
        text = {
            Column {
                Text("Notes from F5 upward are very high-pitched and can be unpleasant to listen to.")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .clickable { dontShowAgain = !dontShowAgain },
                ) {
                    Checkbox(checked = dontShowAgain, onCheckedChange = { dontShowAgain = it })
                    Text("Don't show this again")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(dontShowAgain) }) { Text("Enable anyway") }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancel") }
        },
    )
}

@Composable
private fun GridHeaderCell(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(GridCellSize)
            .height(GridCellSize)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
