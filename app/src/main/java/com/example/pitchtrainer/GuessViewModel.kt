package com.example.pitchtrainer

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import kotlin.math.abs

enum class GuessFeedback { NONE, CORRECT, CLOSE, WRONG }

class GuessViewModel(app: Application) : AndroidViewModel(app) {
    private val engine = PitchEngine()
    private var engineStarted = false

    val settings = mutableStateListOf<Boolean>()

    var targetToneIndex by mutableStateOf(TONES.indexOf("A"))
        private set
    var targetOctave by mutableStateOf(4)
        private set
    var targetFreq by mutableStateOf(440.0)
        private set

    var guessToneIndex by mutableStateOf(TONES.indexOf("C"))
    var guessOctave by mutableStateOf(4)

    var isComplete by mutableStateOf(false)
        private set
    var feedback by mutableStateOf(GuessFeedback.NONE)
        private set

    val targetNoteName: String get() = noteLabel(targetToneIndex, targetOctave)

    val isEasyMode: Boolean
        get() = settings.toBooleanArray().contentEquals(SettingsStore.easyPresetOctave4())

    init {
        settings.addAll(SettingsStore.load(app, PREF_KEY_GUESS_SETTINGS).toList())
        generateNewTask()
    }

    fun onVisible() {
        if (!engineStarted) {
            engine.start()
            engineStarted = true
        }
        engine.updateFrequency(targetFreq)
    }

    fun onHidden() {
        engine.stop()
        engineStarted = false
    }

    override fun onCleared() {
        engine.stop()
    }

    fun generateNewTask() {
        isComplete = false
        feedback = GuessFeedback.NONE

        val activeSlots = settings.indices.filter { settings[it] }
        val slot = if (activeSlots.isNotEmpty()) activeSlots.random() else noteSlot(TONES.indexOf("A"), 4)
        targetToneIndex = toneIndexOf(slot)
        targetOctave = octaveOf(slot)
        targetFreq = frequencyOf(slot)
        // Easy mode only has one possible octave, so the locked wheel should already show it.
        if (isEasyMode) guessOctave = targetOctave
        engine.updateFrequency(targetFreq)
    }

    fun selectGuessTone(toneIndex: Int) {
        guessToneIndex = toneIndex
    }

    fun selectGuessOctave(octave: Int) {
        guessOctave = octave
    }

    fun submit() {
        isComplete = true
        val distance = abs(noteSlot(targetToneIndex, targetOctave) - noteSlot(guessToneIndex, guessOctave))
        feedback = when (distance) {
            0 -> GuessFeedback.CORRECT
            1 -> GuessFeedback.CLOSE
            else -> GuessFeedback.WRONG
        }
        val outcome = when (feedback) {
            GuessFeedback.CORRECT -> GuessOutcome.CORRECT
            GuessFeedback.CLOSE -> GuessOutcome.CLOSE
            else -> GuessOutcome.WRONG
        }
        StatsStore.recordGuess(getApplication(), targetToneIndex, targetOctave, outcome)
        engine.updateFrequency(targetFreq)
    }

    fun nextNote() = generateNewTask()

    fun toggleCell(toneIndex: Int, octave: Int) {
        val slot = noteSlot(toneIndex, octave)
        settings[slot] = !settings[slot]
        persistSettings()
    }

    fun toggleRow(toneIndex: Int) {
        val slots = OCTAVE_RANGE.map { noteSlot(toneIndex, it) }
        val allOn = slots.all { settings[it] }
        slots.forEach { settings[it] = !allOn }
        persistSettings()
    }

    fun toggleColumn(octave: Int) {
        val slots = TONES.indices.map { noteSlot(it, octave) }
        val allOn = slots.all { settings[it] }
        slots.forEach { settings[it] = !allOn }
        persistSettings()
    }

    fun resetSettings() {
        val defaults = SettingsStore.defaultSettings()
        for (i in settings.indices) settings[i] = defaults[i]
        persistSettings()
    }

    fun toggleEasyMode() {
        val newSettings = if (isEasyMode) SettingsStore.defaultSettings() else SettingsStore.easyPresetOctave4()
        for (i in settings.indices) settings[i] = newSettings[i]
        persistSettings()
        // The in-progress target may no longer be reachable under the new settings (e.g. Easy
        // only allows octave 4), so start a fresh round. The abandoned round was never
        // submitted, so it was never recorded to stats either.
        generateNewTask()
    }

    private fun persistSettings() {
        SettingsStore.save(getApplication(), PREF_KEY_GUESS_SETTINGS, settings.toBooleanArray())
    }
}
