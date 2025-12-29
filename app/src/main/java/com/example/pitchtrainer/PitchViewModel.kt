package com.example.pitchtrainer

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*

class PitchViewModel : ViewModel() {
    val allNotes = listOf("A", "A#", "B", "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#")
    val octaves = listOf(4, 5, 6)

    // State to track which Note+Octave combinations are enabled
    var enabledNotes = mutableStateListOf<String>()

    var targetNoteName by mutableStateOf("")
    var targetFreq by mutableStateOf(0.0)
    var userFreq by mutableStateOf(0.0)
    var feedback by mutableStateOf("Adjust the dial!")

    private val engine = PitchEngine()

    init {
        // Start with Octave 4 enabled by default
        selectOctave(4)
        engine.start()
        generateNewTask()
    }

    fun toggleNote(note: String) {
        if (enabledNotes.contains(note)) enabledNotes.remove(note)
        else enabledNotes.add(note)
    }

    fun selectAll() {
        enabledNotes.clear()
        // Check every possible note from Octave 3 through 6
        for (o in 3..6) {
            for (n in allNotes) {
                if (isWithinRange(n, o)) enabledNotes.add("$n$o")
            }
        }
    }

    fun selectOctave(octave: Int) {
        val notesInOctave = allNotes.map { "$it$octave" }.filter { n ->
            val noteName = n.filter { !it.isDigit() }
            isWithinRange(noteName, octave)
        }

        // Check if ALL valid notes for this octave are already enabled
        val isAlreadyFull = enabledNotes.containsAll(notesInOctave)

        if (isAlreadyFull) {
            // Toggle OFF: Remove only the notes belonging to this octave
            enabledNotes.removeAll(notesInOctave)
        } else {
            // Toggle ON: Add missing notes
            notesInOctave.forEach { if (!enabledNotes.contains(it)) enabledNotes.add(it) }
        }
    }

    fun isWithinRange(noteName: String, octave: Int): Boolean {
        val noteIdx = allNotes.indexOf(noteName)

        // G3 is the absolute floor
        if (octave == 3 && noteIdx < 10) return false

        // Allow all of octaves 4 and 5
        if (octave in 4..6) return true

        return true
    }

    fun generateNewTask() {
        if (enabledNotes.isEmpty()) {
            feedback = "Please select at least one note!"
            return
        }
        val randomNote = enabledNotes.random()
        targetNoteName = randomNote

        // Calculate frequency for the chosen note
        val noteName = randomNote.filter { !it.isDigit() }
        val octave = randomNote.filter { it.isDigit() }.toInt()
        val n = allNotes.indexOf(noteName) - 9 + (octave - 4) * 12
        targetFreq = 440.0 * 2.0.pow(n / 12.0)

        userFreq = targetFreq * 2.0.pow((-200..200).random() / 1200.0)
        engine.updateFrequency(userFreq)
    }

    fun adjustPitch(deltaCents: Double) {
        userFreq *= 2.0.pow(deltaCents / 1200.0)
        engine.updateFrequency(userFreq)
    }

    fun submit() {
        val centsOff = (1200 * log2(userFreq / targetFreq)).toInt()
        val result = if (abs(centsOff) < 10) "Perfect! " else "Off by $centsOff cents"
        feedback = result

        viewModelScope.launch {
            delay(1500)
            generateNewTask()
        }
    }

    override fun onCleared() {
        engine.stop()
        super.onCleared()
    }
}