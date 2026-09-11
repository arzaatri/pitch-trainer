package com.example.tonetrainer

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.*

class PitchEngine {
    companion object {
        private const val SAMPLE_RATE = 44100
        private const val FADE_DURATION_MS = 50L

        /** Upper bound on how long a caller should ever wait on isAudible() before giving up and
         * starting the next engine anyway - a safety net in case a device never advances
         * playbackHeadPosition for some reason. Better to risk a rare overlap than hang forever. */
        const val MAX_CROSSFADE_WAIT_MS = 500L
    }

    private val fadeSamples = ((SAMPLE_RATE * FADE_DURATION_MS) / 1000L).toInt()

    @Volatile private var frequency = 440.0
    @Volatile private var pendingFrequency: Double? = null
    @Volatile private var targetAmplitude = 0.0
    @Volatile private var teardownRequested = false
    private var playerThread: Thread? = null

    // AudioTrack (MODE_STREAM) buffers several already-written frames ahead of what's actually
    // reaching the speaker, and how far ahead varies by device/audio route - a fixed estimated
    // gap isn't reliable. So instead we track exactly which frame was the last audible one, and
    // isAudible() compares that against the hardware's real playback position, so callers know
    // precisely (not approximately) when it's safe to start a second engine.
    @Volatile private var framesWritten = 0L
    @Volatile private var lastAudibleFrame = 0L

    private val audioTrack = AudioTrack.Builder()
        .setAudioAttributes(AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
        .setAudioFormat(AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
            .setSampleRate(SAMPLE_RATE)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
        .setBufferSizeInBytes(AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT))
        .setTransferMode(AudioTrack.MODE_STREAM).build()

    /**
     * Changing `frequency` outright while the tone is audible would leave amplitude continuous
     * but make the sine's slope jump instantly at that sample - audible as a click/blip even
     * though there's no amplitude discontinuity. So instead of writing `frequency` directly, hand
     * the new value to the playback thread as pending: it ducks amplitude to 0, swaps the
     * frequency while silent (inaudible - it's being multiplied by zero), then ramps back up to
     * whatever amplitude it's supposed to be at. If already silent this settles in one sample.
     */
    fun updateFrequency(newFreq: Double) {
        if (newFreq == frequency) return
        pendingFrequency = newFreq
    }

    /**
     * Fades the tone in. The underlying AudioTrack/thread is created once and then left running
     * (silent) between notes - repeatedly calling AudioTrack.play()/stop() is what produced an
     * audible click, since each call re-engages the device's audio hardware. Callers that switch
     * between two PitchEngines (e.g. the Tune/Guess panels) should wait until the other engine's
     * isAudible() goes false before calling this, so the two never overlap.
     */
    fun start() {
        targetAmplitude = 1.0
        if (playerThread != null) return

        teardownRequested = false
        audioTrack.play()
        playerThread = Thread {
            var phase = 0.0
            var amplitude = 0.0
            val fadeStep = 1.0 / fadeSamples

            while (true) {
                val buffer = FloatArray(512)
                for (i in buffer.indices) {
                    val changingFrequency = pendingFrequency != null
                    val effectiveTarget = if (changingFrequency) 0.0 else targetAmplitude
                    amplitude = when {
                        amplitude < effectiveTarget -> (amplitude + fadeStep).coerceAtMost(effectiveTarget)
                        amplitude > effectiveTarget -> (amplitude - fadeStep).coerceAtLeast(effectiveTarget)
                        else -> amplitude
                    }
                    if (changingFrequency && amplitude <= 0.0) {
                        frequency = pendingFrequency!!
                        pendingFrequency = null
                    }
                    buffer[i] = (sin(phase) * amplitude).toFloat()
                    phase += 2.0 * PI * frequency / SAMPLE_RATE
                    framesWritten++
                    if (amplitude > 0.0001) lastAudibleFrame = framesWritten
                }
                audioTrack.write(buffer, 0, buffer.size, AudioTrack.WRITE_BLOCKING)
                if (teardownRequested && pendingFrequency == null && amplitude <= 0.0) break
            }
            audioTrack.stop()
        }.also { it.start() }
    }

    /** Fades the tone to silence but keeps the audio stream open for a click-free resume. */
    fun stop() { targetAmplitude = 0.0 }

    /** True while audio that was written earlier is still physically queued in the hardware and
     * hasn't finished playing yet, even if we've already faded to silence in software - i.e. it's
     * not yet safe for a second engine to start without the two briefly overlapping in the speaker. */
    fun isAudible(): Boolean {
        val playedFrames = audioTrack.playbackHeadPosition.toLong() and 0xFFFFFFFFL
        return playedFrames < lastAudibleFrame
    }

    /** Fades out and fully tears down the audio stream. Call when this engine is done for good. */
    fun release() {
        targetAmplitude = 0.0
        teardownRequested = true
        playerThread?.join(200)
        playerThread = null
    }
}
