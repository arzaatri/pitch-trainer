package com.example.pitchtrainer

import android.content.Context

private const val PREFS_NAME = "pitch_trainer_prefs"
private const val BYTES_NEEDED = (TOTAL_NOTES + 7) / 8 // 14 bytes for 108 bits

const val PREF_KEY_TUNE_SETTINGS = "tune_settings"
const val PREF_KEY_GUESS_SETTINGS = "guess_settings"
const val PREF_KEY_TUNE_DIFFICULTY = "tune_difficulty"
private const val PREF_KEY_OCTAVE6_WARNING_DISMISSED = "octave6_warning_dismissed"

/**
 * Persists the 48-slot (12 tones x 4 octaves) active-note set as a packed bitset,
 * hex-encoded into a single short SharedPreferences string instead of a note-name list.
 */
object SettingsStore {

    /** Defaults to A3-E5 inclusive, the comfortable middle of the range. */
    fun defaultSettings(): BooleanArray = BooleanArray(TOTAL_NOTES) { slot ->
        val octave = octaveOf(slot)
        octave in 3..4 || (octave == 5 && toneIndexOf(slot) <= TONES.indexOf("E"))
    }

    fun easyPresetOctave4(): BooleanArray = BooleanArray(TOTAL_NOTES) { slot -> octaveOf(slot) == 4 }

    fun load(context: Context, key: String): BooleanArray {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hex = prefs.getString(key, null) ?: return defaultSettings()
        return decode(hex)
    }

    fun isOctave6WarningDismissed(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(PREF_KEY_OCTAVE6_WARNING_DISMISSED, false)
    }

    fun setOctave6WarningDismissed(context: Context, dismissed: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(PREF_KEY_OCTAVE6_WARNING_DISMISSED, dismissed).apply()
    }

    fun save(context: Context, key: String, active: BooleanArray) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(key, encode(active)).apply()
    }

    fun getString(context: Context, key: String, default: String): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(key, default) ?: default
    }

    fun putString(context: Context, key: String, value: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(key, value).apply()
    }

    private fun encode(active: BooleanArray): String {
        val bytes = ByteArray(BYTES_NEEDED)
        for (slot in active.indices) {
            if (active[slot]) {
                bytes[slot / 8] = (bytes[slot / 8].toInt() or (1 shl (slot % 8))).toByte()
            }
        }
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun decode(hex: String): BooleanArray {
        val result = BooleanArray(TOTAL_NOTES)
        if (hex.length < BYTES_NEEDED * 2) return defaultSettings()
        for (slot in result.indices) {
            val byteIndex = slot / 8
            val byte = hex.substring(byteIndex * 2, byteIndex * 2 + 2).toInt(16)
            result[slot] = (byte and (1 shl (slot % 8))) != 0
        }
        return result
    }
}
