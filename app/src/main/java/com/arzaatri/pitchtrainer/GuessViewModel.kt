package com.arzaatri.pitchtrainer

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class GuessFeedback { NONE, CORRECT, CLOSE, WRONG }

class GuessViewModel(app: Application) : AndroidViewModel(app) {
    private val engine = PitchEngine()
    private var isPanelVisible = false
    private var isAppInForeground = true

    val settings = mutableStateListOf<Boolean>()

    var isPaused by mutableStateOf(false)
        private set

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
    var guessedFreq by mutableStateOf(440.0)
        private set
    var isShowingCorrectTone by mutableStateOf(true)
        private set

    val targetNoteName: String get() = noteLabel(targetToneIndex, targetOctave)

    var isEasyMode by mutableStateOf(false)
        private set

    /** Easy mode's letter wheel is restricted to whichever half of the chromatic scale
     * (relative to A) the target tone falls in, so guessing still takes some listening. */
    val easyToneRange: IntRange
        get() {
            val half = TONES.size / 2
            return if (targetToneIndex < half) 0 until half else half until TONES.size
        }

    var instrument by mutableStateOf(Instrument.SINE)
        private set

    init {
        settings.addAll(SettingsStore.load(app, PREF_KEY_GUESS_SETTINGS).toList())
        isEasyMode = SettingsStore.isGuessEasyModeEnabled(app)
        val storedInstrument = SettingsStore.getString(app, PREF_KEY_INSTRUMENT, Instrument.SINE.name)
        instrument = Instrument.entries.find { it.name == storedInstrument } ?: Instrument.SINE
        if (instrument != Instrument.SINE) loadInstrument(instrument)
        generateNewTask()
    }

    fun selectInstrument(newInstrument: Instrument) {
        if (newInstrument == instrument) return
        instrument = newInstrument
        SettingsStore.putString(getApplication(), PREF_KEY_INSTRUMENT, newInstrument.name)
        loadInstrument(newInstrument)
    }

    /** Parsing the soundfont asset is only needed off the Sine default, and takes a few hundred
     * ms, so it's kept off the main thread rather than blocking init/setInstrument. */
    private fun loadInstrument(instrument: Instrument) {
        val app = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            engine.setInstrument(SoundFontBank.zonesFor(app, instrument))
        }
    }

    fun onVisible() {
        isPanelVisible = true
        refreshAudio()
        engine.updateFrequency(if (isComplete && !isShowingCorrectTone) guessedFreq else targetFreq)
    }

    fun onHidden() {
        isPanelVisible = false
        refreshAudio()
    }

    fun onAppForeground() {
        isAppInForeground = true
        refreshAudio()
    }

    fun onAppBackground() {
        isAppInForeground = false
        refreshAudio()
    }

    fun togglePause() {
        isPaused = !isPaused
        refreshAudio()
    }

    private fun refreshAudio() {
        if (isPanelVisible && isAppInForeground && !isPaused) engine.start() else engine.stop()
    }

    fun isAudible(): Boolean = engine.isAudible()

    override fun onCleared() {
        engine.release()
    }

    fun generateNewTask() {
        isComplete = false
        feedback = GuessFeedback.NONE
        isShowingCorrectTone = true

        val activeSlots = settings.indices.filter { settings[it] }
        val slot = if (activeSlots.isNotEmpty()) activeSlots.random() else noteSlot(TONES.indexOf("A"), 4)
        targetToneIndex = toneIndexOf(slot)
        targetOctave = octaveOf(slot)
        targetFreq = frequencyOf(slot)
        applyEasyModeConstraints()
        engine.updateFrequency(targetFreq)
    }

    /** In easy mode the octave wheel is locked to the target's octave, and the letter wheel is
     * limited to whichever half of the chromatic scale the target falls in. */
    private fun applyEasyModeConstraints() {
        if (!isEasyMode) return
        guessOctave = targetOctave
        if (guessToneIndex !in easyToneRange) guessToneIndex = easyToneRange.first
    }

    fun selectGuessTone(toneIndex: Int) {
        guessToneIndex = toneIndex
    }

    fun selectGuessOctave(octave: Int) {
        guessOctave = octave
    }

    fun submit() {
        isComplete = true
        guessedFreq = frequencyOf(noteSlot(guessToneIndex, guessOctave))
        isShowingCorrectTone = true
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

    /** Post-submission only: switches the playing reference tone between the correct note and
     * the note the user guessed. */
    fun toggleCorrectTone() {
        isShowingCorrectTone = !isShowingCorrectTone
        engine.updateFrequency(if (isShowingCorrectTone) targetFreq else guessedFreq)
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
        isEasyMode = !isEasyMode
        SettingsStore.setGuessEasyModeEnabled(getApplication(), isEasyMode)
        // Easy mode doesn't change which notes are reachable, just how the guess wheels behave,
        // so the in-progress target and its audio stay put; only the wheels need re-clamping.
        applyEasyModeConstraints()
    }

    private fun persistSettings() {
        SettingsStore.save(getApplication(), PREF_KEY_GUESS_SETTINGS, settings.toBooleanArray())
    }
}
