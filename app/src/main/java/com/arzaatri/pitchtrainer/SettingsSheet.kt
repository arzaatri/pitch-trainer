package com.arzaatri.pitchtrainer

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
import androidx.compose.material3.ButtonDefaults
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

private enum class CautionKind { NONE, HIGH, LOW }

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
    instrument: Instrument,
    onSelectInstrument: (Instrument) -> Unit,
) {
    var highWarningDismissed by remember { mutableStateOf(SettingsStore.isHighPitchWarningDismissed(context)) }
    var lowWarningDismissed by remember { mutableStateOf(SettingsStore.isLowPitchWarningDismissed(context)) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pendingKind by remember { mutableStateOf(CautionKind.NONE) }

    fun runGuarded(kind: CautionKind, action: () -> Unit) {
        val dismissed = when (kind) {
            CautionKind.HIGH -> highWarningDismissed
            CautionKind.LOW -> lowWarningDismissed
            CautionKind.NONE -> true
        }
        if (kind != CautionKind.NONE && !dismissed) {
            pendingAction = action
            pendingKind = kind
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Instrument.entries.forEach { inst ->
                    val selected = inst == instrument
                    Button(
                        onClick = { onSelectInstrument(inst) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) AccentGold else DarkGray,
                            contentColor = if (selected) DarkGray else Color.White,
                        ),
                    ) { Text(inst.label) }
                }
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
                                runGuarded(wouldColumnEnableCaution(octave, active)) { onToggleColumn(octave) }
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
                                    runGuarded(wouldRowEnableCaution(toneIndex, active)) { onToggleRow(toneIndex) }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(displayName(TONES[toneIndex]), color = Color.White, fontSize = 12.sp)
                        }
                        OCTAVE_RANGE.forEach { octave ->
                            val slot = noteSlot(toneIndex, octave)
                            val isActive = active.getOrElse(slot) { false }
                            val isHighCaution = isHighCautionSlot(slot)
                            val isLowCaution = isLowCautionSlot(slot)
                            val kind = if (isHighCaution) CautionKind.HIGH else if (isLowCaution) CautionKind.LOW else CautionKind.NONE
                            Box(
                                modifier = Modifier
                                    .size(GridCellSize)
                                    .padding(1.dp)
                                    .background(if (isActive) AccentGold else DarkGray)
                                    .clickable {
                                        runGuarded(if (!isActive) kind else CautionKind.NONE) { onToggleCell(toneIndex, octave) }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isHighCaution) {
                                    Text(
                                        "!",
                                        color = WrongRed,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                    )
                                } else if (isLowCaution) {
                                    Text(
                                        "*",
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
        CautionWarningDialog(
            kind = pendingKind,
            onConfirm = { dontShowAgain ->
                if (dontShowAgain) {
                    when (pendingKind) {
                        CautionKind.HIGH -> {
                            SettingsStore.setHighPitchWarningDismissed(context, true)
                            highWarningDismissed = true
                        }
                        CautionKind.LOW -> {
                            SettingsStore.setLowPitchWarningDismissed(context, true)
                            lowWarningDismissed = true
                        }
                        CautionKind.NONE -> {}
                    }
                }
                pendingAction?.invoke()
                pendingAction = null
            },
            onCancel = { pendingAction = null },
        )
    }
}

/** Which caution warning (if any) newly turning this row on would trigger. */
private fun wouldRowEnableCaution(toneIndex: Int, active: List<Boolean>): CautionKind {
    val slots = OCTAVE_RANGE.map { noteSlot(toneIndex, it) }
    val allOn = slots.all { active.getOrElse(it) { false } }
    if (allOn) return CautionKind.NONE // this tap would turn the row OFF, not on
    val newlyActive = slots.filter { !active.getOrElse(it) { false } }
    return when {
        newlyActive.any { isHighCautionSlot(it) } -> CautionKind.HIGH
        newlyActive.any { isLowCautionSlot(it) } -> CautionKind.LOW
        else -> CautionKind.NONE
    }
}

/** Which caution warning (if any) newly turning this column on would trigger. */
private fun wouldColumnEnableCaution(octave: Int, active: List<Boolean>): CautionKind {
    val slots = TONES.indices.map { noteSlot(it, octave) }
    val allOn = slots.all { active.getOrElse(it) { false } }
    if (allOn) return CautionKind.NONE // this tap would turn the column OFF, not on
    val newlyActive = slots.filter { !active.getOrElse(it) { false } }
    return when {
        newlyActive.any { isHighCautionSlot(it) } -> CautionKind.HIGH
        newlyActive.any { isLowCautionSlot(it) } -> CautionKind.LOW
        else -> CautionKind.NONE
    }
}

@Composable
private fun CautionWarningDialog(kind: CautionKind, onConfirm: (dontShowAgain: Boolean) -> Unit, onCancel: () -> Unit) {
    var dontShowAgain by remember { mutableStateOf(false) }
    val message = when (kind) {
        CautionKind.HIGH -> "Notes from F5 upward are very high-pitched and can be unpleasant to listen to."
        CautionKind.LOW -> "Notes below G3 may be difficult to hear clearly on some device speakers."
        CautionKind.NONE -> ""
    }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Heads up") },
        text = {
            Column {
                Text(message)
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
