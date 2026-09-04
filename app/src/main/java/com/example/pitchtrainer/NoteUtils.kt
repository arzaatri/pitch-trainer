package com.example.pitchtrainer

import kotlin.math.pow

// Internal tone ordering is preserved from the original app so existing frequency math
// (A4 = 440Hz reference, octave boundary at A) doesn't shift.
val TONES = listOf("A", "A#", "B", "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#")

// Restricted to 3-6: outside this range the synthesized tone is unpleasant to listen to.
val OCTAVE_RANGE = 3..6
const val OCTAVE_COUNT = 4 // OCTAVE_RANGE.count()
const val TOTAL_NOTES = 12 * OCTAVE_COUNT // 48

private val DISPLAY_NAMES = mapOf(
    "A#" to "A#/Bb",
    "C#" to "C#/Db",
    "D#" to "D#/Eb",
    "F#" to "F#/Gb",
    "G#" to "G#/Ab",
)

fun displayName(tone: String): String = DISPLAY_NAMES[tone] ?: tone

/**
 * Flat index 0..(TOTAL_NOTES-1) encoding a (tone, octave) pair, relative to OCTAVE_RANGE's
 * floor so the storage array stays densely packed regardless of where the range starts.
 */
fun noteSlot(toneIndex: Int, octave: Int): Int = (octave - OCTAVE_RANGE.first) * 12 + toneIndex

fun toneIndexOf(slot: Int): Int = slot % 12
fun octaveOf(slot: Int): Int = slot / 12 + OCTAVE_RANGE.first

fun frequencyOf(toneIndex: Int, octave: Int): Double {
    val n = toneIndex - 9 + (octave - 4) * 12
    return 440.0 * 2.0.pow(n / 12.0)
}

fun frequencyOf(slot: Int): Double = frequencyOf(toneIndexOf(slot), octaveOf(slot))

fun noteLabel(toneIndex: Int, octave: Int): String = "${displayName(TONES[toneIndex])}$octave"

/** F5 and everything above it is high-pitched enough to be unpleasant to listen to. */
fun isCautionNote(toneIndex: Int, octave: Int): Boolean =
    octave > 5 || (octave == 5 && toneIndex >= TONES.indexOf("F"))

fun isCautionSlot(slot: Int): Boolean = isCautionNote(toneIndexOf(slot), octaveOf(slot))
