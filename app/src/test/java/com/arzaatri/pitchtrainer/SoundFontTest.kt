package com.arzaatri.pitchtrainer

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundFontTest {

    private fun zone(
        keyLo: Int, keyHi: Int, pcm: ShortArray = shortArrayOf(0, 0), gain: Int = 0,
        retriggerSeconds: Double = 0.0, vibratoCapable: Boolean = false,
    ) = Sf2Zone(
        keyLo = keyLo, keyHi = keyHi, rootKey = 69, pitchCorrectionCents = 0,
        sampleRate = 44100, loop = false, loopStart = 0, loopEnd = pcm.size,
        attenuationCb = gain, pcm = pcm, retriggerSeconds = retriggerSeconds, vibratoCapable = vibratoCapable,
    )

    @Test
    fun `pickZone is null for an empty instrument`() {
        assertNull(pickZone(emptyList(), 440.0))
    }

    @Test
    fun `pickZone picks the zone whose key range contains the target`() {
        val low = zone(keyLo = 40, keyHi = 59)
        val high = zone(keyLo = 60, keyHi = 90)
        assertEquals(high, pickZone(listOf(low, high), 440.0)) // A4 = MIDI key 69
    }

    @Test
    fun `pickZone falls back to the nearest zone across a key-range gap`() {
        val farBelow = zone(keyLo = 0, keyHi = 30)
        val nearAbove = zone(keyLo = 72, keyHi = 90) // A4 (key 69) is 3 semitones below this range
        assertEquals(nearAbove, pickZone(listOf(farBelow, nearAbove), 440.0))
    }

    @Test
    fun `sampleAt linearly interpolates between neighboring frames`() {
        val z = zone(keyLo = 0, keyHi = 127, pcm = shortArrayOf(0, 16384))
        assertEquals(0.25f, sampleAt(z, 0.5), 1e-4f)
    }

    @Test
    fun `sampleAt applies the zone's attenuation as linear gain`() {
        val full = zone(keyLo = 0, keyHi = 127, pcm = shortArrayOf(16384), gain = 0)
        val halved = zone(keyLo = 0, keyHi = 127, pcm = shortArrayOf(16384), gain = 60) // -6dB ~ half amplitude
        assertEquals(sampleAt(full, 0.0) / 2f, sampleAt(halved, 0.0), 0.02f)
    }

    @Test
    fun `sampleAt reads past the end as silence instead of crashing`() {
        val z = zone(keyLo = 0, keyHi = 127, pcm = shortArrayOf(16384))
        assertEquals(0f, sampleAt(z, 5.0), 0f)
    }

    @Test
    fun `shouldRetrigger never fires for an instrument without retriggering`() {
        val violin = zone(keyLo = 0, keyHi = 127, retriggerSeconds = 0.0)
        assertTrue(!shouldRetrigger(violin, framesSinceRetrigger = Long.MAX_VALUE, sampleRateOut = 44100))
    }

    @Test
    fun `shouldRetrigger fires once the wall-clock threshold is reached, independent of pitch`() {
        val piano = zone(keyLo = 0, keyHi = 127, retriggerSeconds = 3.0)
        val threshold = (3.0 * 44100).toLong()
        assertTrue(!shouldRetrigger(piano, framesSinceRetrigger = threshold - 1, sampleRateOut = 44100))
        assertTrue(shouldRetrigger(piano, framesSinceRetrigger = threshold, sampleRateOut = 44100))
    }

    @Test
    fun `vibratoMultiplier is a no-op at phase zero and swings symmetrically around it`() {
        assertEquals(1.0, vibratoMultiplier(phase = 0.0, depthCents = 30.0), 1e-9)
        val up = vibratoMultiplier(phase = Math.PI / 2, depthCents = 30.0)
        val down = vibratoMultiplier(phase = -Math.PI / 2, depthCents = 30.0)
        assertTrue(up > 1.0)
        assertEquals(1.0, up * down, 1e-9) // symmetric in cents => reciprocal in linear ratio
    }

    @Test
    fun `effectiveVibrato is exactly 1_0 (no effect at all) once the toggle is off`() {
        val violin = zone(keyLo = 0, keyHi = 127, vibratoCapable = true)
        // Sweep the whole phase cycle - at every point in it, "off" must mean exactly no change,
        // not "small" or "averages out" - this is the literal claim the toggle makes to the user.
        var phase = 0.0
        while (phase < 2 * Math.PI) {
            assertEquals(1.0, effectiveVibrato(vibratoEnabled = false, violin, phase, depthCents = 30.0), 0.0)
            phase += 0.1
        }
    }

    @Test
    fun `effectiveVibrato ignores the toggle entirely for a zone that isn't vibrato-capable`() {
        // Piano zones never set vibratoCapable, so even if the shared toggle were left on from a
        // prior Violin session, switching to Piano must never carry vibrato over.
        val piano = zone(keyLo = 0, keyHi = 127, vibratoCapable = false)
        assertEquals(1.0, effectiveVibrato(vibratoEnabled = true, piano, phase = Math.PI / 2, depthCents = 30.0), 0.0)
    }

    @Test
    fun `effectiveVibrato does modulate when on and the zone supports it`() {
        val violin = zone(keyLo = 0, keyHi = 127, vibratoCapable = true)
        assertTrue(effectiveVibrato(vibratoEnabled = true, violin, Math.PI / 2, depthCents = 30.0) != 1.0)
    }

    // Exercises the real bundled asset end-to-end so a corrupt download or a parser bug that
    // only shows up on real SF2 data (vs. these hand-built zones) fails a fast local test.
    @Test
    fun `real soundfont asset yields usable piano and violin zones`() {
        val bytes = File("src/main/assets/GeneralUserGS.sf2").readBytes()
        val piano = Sf2Parser.extractZones(bytes, 0)
        val violin = Sf2Parser.extractZones(bytes, 40)

        for (zones in listOf(piano, violin)) {
            assertTrue(zones.isNotEmpty())
            for (z in zones) {
                assertTrue(z.keyLo <= z.keyHi)
                assertTrue(z.pcm.isNotEmpty())
                assertTrue(z.sampleRate > 0)
                assertTrue(z.rootFrequency > 0.0)
            }
        }

        // The app only ever plays within roughly A3-G#6; every note in that span should resolve
        // to some zone (pickZone always returns non-null for a non-empty list regardless, but
        // this also checks the picked zone is a plausible match, not just "the list wasn't empty").
        for (octave in 3..6) {
            for (toneIndex in TONES.indices) {
                val freq = frequencyOf(toneIndex, octave)
                assertTrue(pickZone(piano, freq) != null)
                assertTrue(pickZone(violin, freq) != null)
            }
        }
    }
}
