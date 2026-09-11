package com.example.tonetrainer

import android.content.Context

private const val PREF_KEY_TUNE_STATS = "tune_stats"
private const val PREF_KEY_GUESS_STATS = "guess_stats"

private const val TUNE_FIELDS = 5 // flat, correct, sharp, sumAbsFlatCents, sumAbsSharpCents
private const val GUESS_FIELDS = 3 // correct, close, wrong

enum class BreakdownMode { OVERALL, BY_TONE, BY_OCTAVE, BY_TONE_OCTAVE }

data class TuneStatsRow(
    val label: String,
    val flat: Int,
    val correct: Int,
    val sharp: Int,
    val avgFlatCents: Double,
    val avgSharpCents: Double,
)

data class GuessStatsRow(
    val label: String,
    val correct: Int,
    val close: Int,
    val wrong: Int,
)

enum class GuessOutcome { CORRECT, CLOSE, WRONG }

/**
 * Aggregated (not per-event log) attempt counters, keyed by note slot (0..107).
 * Only touched slots are stored, so footprint stays tiny no matter how many attempts happen.
 */
object StatsStore {

    fun recordTune(context: Context, toneIndex: Int, octave: Int, diffCents: Int) {
        val slot = noteSlot(toneIndex, octave)
        val stats = loadMap(context, PREF_KEY_TUNE_STATS, TUNE_FIELDS)
        val row = stats.getOrPut(slot) { IntArray(TUNE_FIELDS) }
        when {
            diffCents < 0 -> {
                row[0] += 1
                row[3] += -diffCents
            }
            diffCents > 0 -> {
                row[2] += 1
                row[4] += diffCents
            }
            else -> row[1] += 1
        }
        saveMap(context, PREF_KEY_TUNE_STATS, stats)
    }

    fun recordGuess(context: Context, toneIndex: Int, octave: Int, outcome: GuessOutcome) {
        val slot = noteSlot(toneIndex, octave)
        val stats = loadMap(context, PREF_KEY_GUESS_STATS, GUESS_FIELDS)
        val row = stats.getOrPut(slot) { IntArray(GUESS_FIELDS) }
        when (outcome) {
            GuessOutcome.CORRECT -> row[0] += 1
            GuessOutcome.CLOSE -> row[1] += 1
            GuessOutcome.WRONG -> row[2] += 1
        }
        saveMap(context, PREF_KEY_GUESS_STATS, stats)
    }

    fun tuneRows(context: Context, mode: BreakdownMode): List<TuneStatsRow> {
        val stats = loadMap(context, PREF_KEY_TUNE_STATS, TUNE_FIELDS)
        return when (mode) {
            BreakdownMode.OVERALL -> listOf(buildTuneRow("Overall", stats.values))
            BreakdownMode.BY_TONE -> TONES.indices.map { toneIndex ->
                val entries = stats.filterKeys { toneIndexOf(it) == toneIndex }.values
                buildTuneRow(displayName(TONES[toneIndex]), entries)
            }
            BreakdownMode.BY_OCTAVE -> OCTAVE_RANGE.map { octave ->
                val entries = stats.filterKeys { octaveOf(it) == octave }.values
                buildTuneRow("Octave $octave", entries)
            }
            BreakdownMode.BY_TONE_OCTAVE -> stats.keys
                .sortedWith(compareBy({ octaveOf(it) }, { toneIndexOf(it) }))
                .map { slot -> buildTuneRow(noteLabel(toneIndexOf(slot), octaveOf(slot)), listOf(stats.getValue(slot))) }
        }
    }

    fun guessRows(context: Context, mode: BreakdownMode): List<GuessStatsRow> {
        val stats = loadMap(context, PREF_KEY_GUESS_STATS, GUESS_FIELDS)
        return when (mode) {
            BreakdownMode.OVERALL -> listOf(buildGuessRow("Overall", stats.values))
            BreakdownMode.BY_TONE -> TONES.indices.map { toneIndex ->
                val entries = stats.filterKeys { toneIndexOf(it) == toneIndex }.values
                buildGuessRow(displayName(TONES[toneIndex]), entries)
            }
            BreakdownMode.BY_OCTAVE -> OCTAVE_RANGE.map { octave ->
                val entries = stats.filterKeys { octaveOf(it) == octave }.values
                buildGuessRow("Octave $octave", entries)
            }
            BreakdownMode.BY_TONE_OCTAVE -> stats.keys
                .sortedWith(compareBy({ octaveOf(it) }, { toneIndexOf(it) }))
                .map { slot -> buildGuessRow(noteLabel(toneIndexOf(slot), octaveOf(slot)), listOf(stats.getValue(slot))) }
        }
    }

    private fun buildTuneRow(label: String, entries: Collection<IntArray>): TuneStatsRow {
        var flat = 0; var correct = 0; var sharp = 0; var sumFlat = 0; var sumSharp = 0
        for (e in entries) {
            flat += e[0]; correct += e[1]; sharp += e[2]; sumFlat += e[3]; sumSharp += e[4]
        }
        return TuneStatsRow(
            label = label,
            flat = flat,
            correct = correct,
            sharp = sharp,
            avgFlatCents = if (flat > 0) sumFlat.toDouble() / flat else 0.0,
            avgSharpCents = if (sharp > 0) sumSharp.toDouble() / sharp else 0.0,
        )
    }

    private fun buildGuessRow(label: String, entries: Collection<IntArray>): GuessStatsRow {
        var correct = 0; var close = 0; var wrong = 0
        for (e in entries) {
            correct += e[0]; close += e[1]; wrong += e[2]
        }
        return GuessStatsRow(label, correct, close, wrong)
    }

    private fun loadMap(context: Context, key: String, fields: Int): MutableMap<Int, IntArray> {
        val prefs = context.getSharedPreferences("pitch_trainer_prefs", Context.MODE_PRIVATE)
        val raw = prefs.getString(key, null) ?: return mutableMapOf()
        val map = mutableMapOf<Int, IntArray>()
        raw.split(';').forEach { entry ->
            if (entry.isBlank()) return@forEach
            val (slotStr, valuesStr) = entry.split(':', limit = 2)
            val slot = slotStr.toIntOrNull() ?: return@forEach
            val values = valuesStr.split(',').map { it.toIntOrNull() ?: 0 }
            if (values.size == fields) map[slot] = values.toIntArray()
        }
        return map
    }

    private fun saveMap(context: Context, key: String, map: Map<Int, IntArray>) {
        val prefs = context.getSharedPreferences("pitch_trainer_prefs", Context.MODE_PRIVATE)
        val encoded = map.entries.joinToString(";") { (slot, values) ->
            "$slot:${values.joinToString(",")}"
        }
        prefs.edit().putString(key, encoded).apply()
    }
}
