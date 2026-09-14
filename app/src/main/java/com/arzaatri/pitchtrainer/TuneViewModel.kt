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
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt

enum class Difficulty(val label: String, val startStepCents: Int, val maxStartSteps: Int, val moveStepCents: Int) {
    EASY("Easy", 30, 5, 30),
    MEDIUM("Medium", 20, 5, 20),
    HARD("Hard", 10, 10, 10),
}

class TuneViewModel(app: Application) : AndroidViewModel(app) {
    private val engine = PitchEngine()
    private var isPanelVisible = false
    private var isAppInForeground = true

    val settings = mutableStateListOf<Boolean>()

    var isPaused by mutableStateOf(false)
        private set

    var difficulty by mutableStateOf(Difficulty.HARD)
        private set
    var targetToneIndex by mutableStateOf(TONES.indexOf("A"))
        private set
    var targetOctave by mutableStateOf(4)
        private set
    var targetFreq by mutableStateOf(440.0)
        private set
    var userFreq by mutableStateOf(440.0)
        private set
    var feedback by mutableStateOf("Adjust the pitch")
        private set
    var isComplete by mutableStateOf(false)
        private set
    var isShowingCorrectTone by mutableStateOf(true)
        private set

    val targetNoteName: String get() = noteLabel(targetToneIndex, targetOctave)

    var instrument by mutableStateOf(Instrument.SINE)
        private set
    var isVibratoEnabled by mutableStateOf(false)
        private set

    init {
        settings.addAll(SettingsStore.load(app, PREF_KEY_TUNE_SETTINGS).toList())
        val storedDifficulty = SettingsStore.getString(app, PREF_KEY_TUNE_DIFFICULTY, Difficulty.HARD.name)
        difficulty = Difficulty.entries.find { it.name == storedDifficulty } ?: Difficulty.HARD
        val storedInstrument = SettingsStore.getString(app, PREF_KEY_INSTRUMENT, Instrument.SINE.name)
        instrument = Instrument.entries.find { it.name == storedInstrument } ?: Instrument.SINE
        if (instrument != Instrument.SINE) loadInstrument(instrument)
        isVibratoEnabled = SettingsStore.getBoolean(app, PREF_KEY_VIBRATO, false)
        engine.setVibratoEnabled(isVibratoEnabled)
        generateNewTask()
    }

    fun selectInstrument(newInstrument: Instrument) {
        if (newInstrument == instrument) return
        instrument = newInstrument
        SettingsStore.putString(getApplication(), PREF_KEY_INSTRUMENT, newInstrument.name)
        loadInstrument(newInstrument)
    }

    fun toggleVibrato() {
        isVibratoEnabled = !isVibratoEnabled
        SettingsStore.putBoolean(getApplication(), PREF_KEY_VIBRATO, isVibratoEnabled)
        engine.setVibratoEnabled(isVibratoEnabled)
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
        engine.updateFrequency(if (isComplete && isShowingCorrectTone) targetFreq else userFreq)
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

    fun selectDifficulty(d: Difficulty) {
        if (d == difficulty) return
        difficulty = d
        SettingsStore.putString(getApplication(), PREF_KEY_TUNE_DIFFICULTY, d.name)
        // The in-progress offset was generated for the old step size and may no longer be
        // reachable (e.g. 80c isn't a multiple of a 30c step), so start a fresh round. Since
        // the abandoned round was never submitted, it was never recorded to stats either.
        generateNewTask()
    }

    fun generateNewTask() {
        isComplete = false
        feedback = "Adjust the pitch"
        isShowingCorrectTone = true

        val activeSlots = settings.indices.filter { settings[it] }
        val slot = if (activeSlots.isNotEmpty()) activeSlots.random() else noteSlot(TONES.indexOf("A"), 4)
        targetToneIndex = toneIndexOf(slot)
        targetOctave = octaveOf(slot)
        targetFreq = frequencyOf(slot)

        val steps = (1..difficulty.maxStartSteps).random()
        val sign = if (listOf(true, false).random()) 1 else -1
        val offsetCents = steps * difficulty.startStepCents * sign
        userFreq = targetFreq * 2.0.pow(offsetCents / 1200.0)
        engine.updateFrequency(userFreq)
    }

    fun adjustPitch(direction: Int) {
        if (isComplete) return
        userFreq *= 2.0.pow((direction * difficulty.moveStepCents) / 1200.0)
        engine.updateFrequency(userFreq)
    }

    /** Post-submission only: switches the playing reference tone between the correct note and
     * the note the user actually tuned to. */
    fun toggleCorrectTone() {
        isShowingCorrectTone = !isShowingCorrectTone
        engine.updateFrequency(if (isShowingCorrectTone) targetFreq else userFreq)
    }

    fun submit() {
        isComplete = true
        engine.updateFrequency(targetFreq)

        val diffCents = (1200 * log2(userFreq / targetFreq)).roundToInt()
        feedback = when {
            diffCents == 0 -> "Perfect Match!"
            kotlin.math.abs(diffCents) == difficulty.moveStepCents -> "Close! You were off by $diffCents cents"
            else -> "You were off by $diffCents cents"
        }
        StatsStore.recordTune(getApplication(), targetToneIndex, targetOctave, diffCents)
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

    private fun persistSettings() {
        SettingsStore.save(getApplication(), PREF_KEY_TUNE_SETTINGS, settings.toBooleanArray())
    }
}
