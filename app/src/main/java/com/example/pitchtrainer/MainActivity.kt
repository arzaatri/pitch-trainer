package com.example.pitchtrainer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.Composable

import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.Arrangement

// For FontWeight (Bold, ExtraBold, etc.)
import androidx.compose.ui.text.font.FontWeight

// For Icons (The Refresh icon)
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh

// For RoundedCornerShape (Making the button corners soft)
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.filled.Settings

import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.CornerRadius

// Define the Night Colors
private val DarkGray = Color(0xFF121212)
private val SurfaceGray = Color(0xFF1E1E1E)
private val AccentGold = Color(0xFFB58F00)
private val SoftCyan = Color(0xFF03DAC5)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = AccentGold,
                    background = DarkGray,
                    surface = SurfaceGray
                )
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PitchApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PitchApp(vm: PitchViewModel = viewModel()) {
    val sheetState = rememberModalBottomSheetState()
    var showSettings by remember { mutableStateOf(false) }
    // Wrap everything in a Column with a dark background
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(DarkGray) // Deep dark background
            .padding(24.dp)
    ) {
        IconButton(
            onClick = { showSettings = true },
            modifier = Modifier.align(Alignment.End)
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
        }
        // Target Note with a soft glow color
        Text(
            text = vm.targetNoteName,
            fontSize = 72.sp,
            fontWeight = FontWeight.ExtraBold,
            color = AccentGold
        )

        Text(
            text = vm.feedback,
            fontSize = 18.sp,
            color = Color.LightGray.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(50.dp))

        // The Dial
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()        // Spans the width of the screen
                .height(150.dp)        // Fixed height for the "ribbon"
                .padding(horizontal = 16.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        // Adjusting sensitivity: higher number = slower pitch change
                        vm.adjustPitch(dragAmount.x.toDouble() / 2.0)
                        change.consume()
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // 1. Draw the background of the rectangle
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.05f),
                    cornerRadius = CornerRadius(12f, 12f)
                )

                // 2. Draw the outline (The "Border")
                drawRoundRect(
                    color = AccentGold,
                    style = Stroke(width = 4f),
                    cornerRadius = CornerRadius(12f, 12f)
                )
            }

            // 3. Optional: Add a visual "Center Line"
            // This helps the user see where the "neutral" point is
            /*
            Divider(
                color = AccentGold.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxHeight().width(1.dp)
            )
            */

            Text(
                "SLIDE LEFT OR RIGHT TO ADJUST PITCH",
                color = AccentGold.copy(alpha = 0.6f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(60.dp))

        // Submit Button with Dark Theme Styling
        Button(
            onClick = { vm.submit() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SurfaceGray,
                contentColor = AccentGold
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("SUBMIT PITCH", fontWeight = FontWeight.Bold)
        }
        if (showSettings) {
            ModalBottomSheet(
                onDismissRequest = { showSettings = false },
                sheetState = sheetState,
                containerColor = Color(0xFF1E1E1E)
            ) {
                Column(modifier = Modifier.fillMaxHeight(0.8f).padding(16.dp)) {
                    Text("Practice Settings", fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold)

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Button(onClick = { vm.selectAll() }) { Text("Select All") }
                        vm.octaves.forEach { o ->
                            // Check if the octave is currently "Active"
                            val isActive = vm.enabledNotes.any { it.endsWith(o.toString()) }

                            Button(
                                onClick = { vm.selectOctave(o) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isActive) AccentGold else SurfaceGray,
                                    contentColor = if (isActive) DarkGray else Color.White
                                )
                            ) {
                                Text("Oct $o")
                            }
                        }
                    }

                    LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.weight(1f)) {
                        // We check octaves 3, 4, 5, and 6 for the checkboxes
                        val displayOctaves = listOf(3, 4, 5, 6)

                        items(vm.allNotes.size * displayOctaves.size) { index ->
                            val n = vm.allNotes[index % 12]
                            val o = displayOctaves[index / 12]
                            val noteKey = "$n$o"

                            // ONLY show the checkbox if it's within your G3-G#6 range
                            if (vm.isWithinRange(n, o)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = vm.enabledNotes.contains(noteKey),
                                        onCheckedChange = { vm.toggleNote(noteKey) }
                                    )
                                    Text(noteKey, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
