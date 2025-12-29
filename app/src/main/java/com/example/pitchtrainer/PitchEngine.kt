package com.example.pitchtrainer // Replace with your actual package name

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.*

class PitchEngine {
    private val sampleRate = 44100
    private var frequency = 440.0
    private var isPlaying = false

    private val audioTrack = AudioTrack.Builder()
        .setAudioAttributes(AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
        .setAudioFormat(AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
        .setBufferSizeInBytes(AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT))
        .setTransferMode(AudioTrack.MODE_STREAM).build()

    fun updateFrequency(newFreq: Double) { frequency = newFreq }

    fun start() {
        if (isPlaying) return
        isPlaying = true
        audioTrack.play()
        Thread {
            var phase = 0.0
            while (isPlaying) {
                val buffer = FloatArray(512)
                for (i in buffer.indices) {
                    buffer[i] = sin(phase).toFloat()
                    phase += 2.0 * PI * frequency / sampleRate
                }
                audioTrack.write(buffer, 0, buffer.size, AudioTrack.WRITE_BLOCKING)
            }
        }.start()
    }

    fun stop() { isPlaying = false; audioTrack.stop() }
}