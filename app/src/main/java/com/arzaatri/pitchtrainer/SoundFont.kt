package com.arzaatri.pitchtrainer

import android.content.Context
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.pow

enum class Instrument(val label: String) {
    SINE("Sine"), VIOLIN("Violin"), PIANO("Piano"),
}

/** One playable sample region extracted from a .sf2 SoundFont preset: a chunk of 16-bit mono
 * PCM covering a MIDI key range, resampled at synthesis time (see PitchEngine) to hit this app's
 * continuous, non-tempered target frequencies rather than just the pitch it was recorded at. */
class Sf2Zone(
    val keyLo: Int,
    val keyHi: Int,
    rootKey: Int,
    pitchCorrectionCents: Int,
    val sampleRate: Int,
    val loop: Boolean,
    val loopStart: Int,
    val loopEnd: Int,
    attenuationCb: Int,
    val pcm: ShortArray,
) {
    val rootFrequency = 440.0 * 2.0.pow((rootKey - 69 + pitchCorrectionCents / 100.0) / 12.0)
    val gain = 10.0.pow(-attenuationCb / 200.0)
}

/** Lazily parses just the Violin and Piano presets out of the bundled GeneralUser GS soundfont
 * and caches the resulting zones - the rest of the ~31MB bank is never kept resident, only the
 * PCM these two presets actually reference. */
object SoundFontBank {
    private const val ASSET_NAME = "GeneralUserGS.sf2"
    private const val GM_PIANO = 0
    private const val GM_VIOLIN = 40

    private var piano: List<Sf2Zone>? = null
    private var violin: List<Sf2Zone>? = null

    /** Parses the asset on first call for a non-Sine instrument (a few hundred ms) - callers
     * must invoke this off the main thread. */
    @Synchronized
    fun zonesFor(context: Context, instrument: Instrument): List<Sf2Zone> {
        if (instrument == Instrument.SINE) return emptyList()
        if (piano == null) {
            val bytes = context.assets.open(ASSET_NAME).use { it.readBytes() }
            piano = Sf2Parser.extractZones(bytes, GM_PIANO)
            violin = Sf2Parser.extractZones(bytes, GM_VIOLIN)
        }
        return if (instrument == Instrument.PIANO) piano!! else violin!!
    }
}

/** Minimal reader for the subset of the SF2 RIFF format needed to pull sample-based zones out of
 * a General MIDI preset: chunk headers, the preset -> instrument -> sample generator chain, and
 * raw PCM. Modulators and envelopes aren't read (PitchEngine supplies its own fade/crossfade
 * envelope instead, and we never send velocity/controller events for them to react to). */
internal object Sf2Parser {
    private const val GEN_INSTRUMENT = 41
    private const val GEN_KEY_RANGE = 43
    private const val GEN_ATTENUATION = 48
    private const val GEN_COARSE_TUNE = 51
    private const val GEN_FINE_TUNE = 52
    private const val GEN_SAMPLE_ID = 53
    private const val GEN_SAMPLE_MODES = 54
    private const val GEN_ROOT_KEY = 58

    private class Chunk(val id: String, val start: Int, val end: Int)

    private fun readChunks(buf: ByteBuffer, from: Int, to: Int): List<Chunk> {
        val chunks = mutableListOf<Chunk>()
        var pos = from
        while (pos + 8 <= to) {
            val id = String(buf.array(), pos, 4, Charsets.US_ASCII)
            val size = buf.getInt(pos + 4)
            val dataStart = pos + 8
            chunks += Chunk(id, dataStart, dataStart + size)
            pos = dataStart + size + (size and 1) // chunks are padded to an even size
        }
        return chunks
    }

    private fun listType(buf: ByteBuffer, chunk: Chunk) = String(buf.array(), chunk.start, 4, Charsets.US_ASCII)

    /** Reads igen/pgen records [genLo, genHi) into oper->amount. Every generator except keyRange
     * is a plain signed 16-bit amount; keyRange packs (lo, hi) into the low/high bytes instead,
     * so it's left as the raw unsigned word for the caller to unpack. */
    private fun readGens(buf: ByteBuffer, genRecords: List<Int>, genLo: Int, genHi: Int): Map<Int, Int> {
        val gens = mutableMapOf<Int, Int>()
        for (i in genLo until genHi) {
            val oper = buf.getShort(genRecords[i]).toInt() and 0xFFFF
            val raw = buf.getShort(genRecords[i] + 2).toInt() and 0xFFFF
            gens[oper] = if (oper == GEN_KEY_RANGE) raw else raw.toShort().toInt()
        }
        return gens
    }

    fun extractZones(bytes: ByteArray, program: Int): List<Sf2Zone> {
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val top = readChunks(buf, 12, bytes.size) // skip "RIFF" + size + "sfbk"
        val sdta = top.first { it.id == "LIST" && listType(buf, it) == "sdta" }
        val pdta = top.first { it.id == "LIST" && listType(buf, it) == "pdta" }
        val smpl = readChunks(buf, sdta.start + 4, sdta.end).first { it.id == "smpl" }
        val pdtaChunks = readChunks(buf, pdta.start + 4, pdta.end).associateBy { it.id }
        fun records(id: String, recordSize: Int) = pdtaChunks.getValue(id).let { (it.start until it.end step recordSize).toList() }

        // A preset's zone count is (next preset's bag index - this preset's bag index): phdr
        // records are guaranteed to be laid out with non-decreasing bag indices, and the file
        // always has a trailing terminal ("EOP") record, so "index + 1" is always safe here.
        val phdrRecords = records("phdr", 38)
        val presetIdx = phdrRecords.indexOfFirst { buf.getShort(it + 20) == program.toShort() && buf.getShort(it + 22) == 0.toShort() }
        val presetBagLo = buf.getShort(phdrRecords[presetIdx] + 24).toInt() and 0xFFFF
        val presetBagHi = buf.getShort(phdrRecords[presetIdx + 1] + 24).toInt() and 0xFFFF
        val pbagRecords = records("pbag", 4)
        val pgenRecords = records("pgen", 4)
        val instrumentIdx = (presetBagLo until presetBagHi).firstNotNullOf { bagI ->
            val genLo = buf.getShort(pbagRecords[bagI]).toInt() and 0xFFFF
            val genHi = buf.getShort(pbagRecords[bagI + 1]).toInt() and 0xFFFF
            readGens(buf, pgenRecords, genLo, genHi)[GEN_INSTRUMENT]
        }

        val instRecords = records("inst", 22)
        val instBagLo = buf.getShort(instRecords[instrumentIdx] + 20).toInt() and 0xFFFF
        val instBagHi = buf.getShort(instRecords[instrumentIdx + 1] + 20).toInt() and 0xFFFF
        val ibagRecords = records("ibag", 4)
        val igenRecords = records("igen", 4)
        val shdrRecords = records("shdr", 46)

        // The first instrument zone, if it has no sampleID generator of its own, sets default
        // generator values for every zone that follows it (a per-zone value still wins).
        var globalGens: Map<Int, Int> = emptyMap()
        val zones = mutableListOf<Sf2Zone>()
        for (bagI in instBagLo until instBagHi) {
            val genLo = buf.getShort(ibagRecords[bagI]).toInt() and 0xFFFF
            val genHi = buf.getShort(ibagRecords[bagI + 1]).toInt() and 0xFFFF
            val zoneGens = readGens(buf, igenRecords, genLo, genHi)
            val sampleId = zoneGens[GEN_SAMPLE_ID]
            if (sampleId == null) {
                globalGens = zoneGens
                continue
            }
            val gens = globalGens + zoneGens
            val shdr = shdrRecords[sampleId]
            val sampleStart = buf.getInt(shdr + 20)
            val sampleEnd = buf.getInt(shdr + 24)
            val loopStart = buf.getInt(shdr + 28)
            val loopEnd = buf.getInt(shdr + 32)
            val sampleRate = buf.getInt(shdr + 36)
            val originalPitch = buf.get(shdr + 40).toInt() and 0xFF
            val pitchCorrection = buf.get(shdr + 41).toInt()

            val keyRange = gens[GEN_KEY_RANGE] ?: 0x7F00 // default: whole keyboard, 0-127
            val sampleModes = gens[GEN_SAMPLE_MODES] ?: 0
            val pcm = ShortArray(sampleEnd - sampleStart) { buf.getShort(smpl.start + (sampleStart + it) * 2) }

            zones += Sf2Zone(
                keyLo = keyRange and 0xFF,
                keyHi = (keyRange shr 8) and 0xFF,
                rootKey = gens[GEN_ROOT_KEY] ?: originalPitch,
                pitchCorrectionCents = pitchCorrection + (gens[GEN_COARSE_TUNE] ?: 0) * 100 + (gens[GEN_FINE_TUNE] ?: 0),
                sampleRate = sampleRate,
                loop = sampleModes == 1 || sampleModes == 3,
                loopStart = loopStart - sampleStart,
                loopEnd = loopEnd - sampleStart,
                attenuationCb = gens[GEN_ATTENUATION] ?: 0,
                pcm = pcm,
            )
        }
        return zones
    }
}
