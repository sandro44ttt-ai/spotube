package org.spotube.spotube.core.automix

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val CLIP_RENDER_VERSION = 2

class StemTransitionRenderer(
    private val context: Context,
    private val separator: StemSeparator
) {
    private val clipDir = File(context.filesDir, "automix/stems/clips")

    fun clipFor(
        currentId: String,
        nextId: String,
        outgoingCueMs: Long,
        incomingCueMs: Long,
        durationMs: Long,
        tempoRatio: Float,
        pitchSemitones: Int
    ): File? {
        val file = clipFile(currentId, nextId, outgoingCueMs, incomingCueMs, durationMs, tempoRatio, pitchSemitones)
        return file.takeIf { it.exists() && it.length() > 44L }
    }

    suspend fun render(
        currentId: String,
        currentUrl: String,
        nextId: String,
        nextUrl: String,
        outgoingCueMs: Long,
        incomingCueMs: Long,
        durationMs: Long,
        tempoRatio: Float,
        pitchSemitones: Int,
        onProgress: (Float) -> Unit = {}
    ): File? = withContext(Dispatchers.IO) {
        if (durationMs < 4_000L) return@withContext null
        if (!clipDir.exists() && !clipDir.mkdirs()) return@withContext null
        val output = clipFile(
            currentId,
            nextId,
            outgoingCueMs,
            incomingCueMs,
            durationMs,
            tempoRatio,
            pitchSemitones
        )
        if (output.exists() && output.length() > 44L) return@withContext output
        val frames = (durationMs * StemSeparator.SAMPLE_RATE / 1000L).toInt().coerceAtLeast(1)
        Log.d("AutoMix", "stem clip current decode start cue=$outgoingCueMs")
        val current = separator.separateWindow(
            trackId = currentId,
            url = currentUrl,
            startMs = outgoingCueMs,
            frames = frames
        ) { progress -> onProgress(progress * 0.45f) } ?: return@withContext null
        Log.d("AutoMix", "stem clip current frames=${current.frames}/$frames")
        if (current.frames < frames * 0.7f) return@withContext null
        onProgress(0.5f)
        Log.d("AutoMix", "stem clip next decode start cue=$incomingCueMs")
        val next = separator.separateWindow(
            trackId = nextId,
            url = nextUrl,
            startMs = incomingCueMs,
            frames = frames
        ) { progress -> onProgress(0.5f + progress * 0.45f) } ?: return@withContext null
        Log.d("AutoMix", "stem clip next frames=${next.frames}/$frames")
        if (next.frames < frames * 0.7f) return@withContext null
        onProgress(0.96f)
        val rendered = mix(current, next, frames)
        writeWav(output, rendered, frames)
        Log.d("AutoMix", "stem clip rendered ${output.name} bytes=${output.length()}")
        onProgress(1f)
        output
    }

    private fun mix(current: StemSeparator.StemWindow, next: StemSeparator.StemWindow, frames: Int): ShortArray {
        val result = FloatArray(frames * 2)
        for (frame in 0 until frames) {
            val t = frame.toFloat() / frames.toFloat()
            val currentVocals = DjFade.fadeOut(t, 0.25f, 0.45f)
            val nextVocals = DjFade.fadeIn(t, 0.55f, 0.75f)
            val currentDrums = DjFade.equalPowerOut(t, 0.10f, 0.90f)
            val nextDrums = DjFade.equalPowerIn(t, 0.10f, 0.90f)
            val currentBass = DjFade.fadeOut(t, 0.30f, 0.50f)
            val nextBass = DjFade.fadeIn(t, 0.50f, 0.70f)
            val currentOther = DjFade.equalPowerOut(t, 0.20f, 0.85f)
            val nextOther = DjFade.equalPowerIn(t, 0.20f, 0.85f)
            for (channel in 0..1) {
                val index = frame * 2 + channel
                var sample = 0f
                sample += current.drums.getOrElse(index) { 0f } * currentDrums
                sample += current.bass.getOrElse(index) { 0f } * currentBass
                sample += current.other.getOrElse(index) { 0f } * currentOther
                sample += current.vocals.getOrElse(index) { 0f } * currentVocals
                sample += next.drums.getOrElse(index) { 0f } * nextDrums
                sample += next.bass.getOrElse(index) { 0f } * nextBass
                sample += next.other.getOrElse(index) { 0f } * nextOther
                sample += next.vocals.getOrElse(index) { 0f } * nextVocals
                result[index] = softClip(sample)
            }
        }
        val short = ShortArray(frames * 2)
        for (index in result.indices) {
            short[index] = (result[index].coerceIn(-1f, 1f) * 32767f).roundToInt().toShort()
        }
        return short
    }

    private fun writeWav(file: File, samples: ShortArray, frames: Int) {
        val dataBytes = frames * 2 * 2
        FileOutputStream(file).use { out ->
            fun writeInt(value: Int) {
                out.write(value and 0xff)
                out.write((value ushr 8) and 0xff)
                out.write((value ushr 16) and 0xff)
                out.write((value ushr 24) and 0xff)
            }
            fun writeShort(value: Int) {
                out.write(value and 0xff)
                out.write((value ushr 8) and 0xff)
            }
            out.write("RIFF".toByteArray())
            writeInt(36 + dataBytes)
            out.write("WAVEfmt ".toByteArray())
            writeInt(16)
            writeShort(1)
            writeShort(2)
            writeInt(StemSeparator.SAMPLE_RATE)
            writeInt(StemSeparator.SAMPLE_RATE * 2 * 2)
            writeShort(2 * 2)
            writeShort(16)
            out.write("data".toByteArray())
            writeInt(dataBytes)
            for (sample in samples) writeShort(sample.toInt())
        }
    }

    private fun clipFile(
        currentId: String,
        nextId: String,
        outgoingCueMs: Long,
        incomingCueMs: Long,
        durationMs: Long,
        tempoRatio: Float,
        pitchSemitones: Int
    ): File {
        val key = "$CLIP_RENDER_VERSION|$currentId|$nextId|$outgoingCueMs|$incomingCueMs|" +
            "$durationMs|${(tempoRatio * 1000).roundToInt()}|$pitchSemitones"
        val hash = key.hashCode().toUInt().toString(16)
        return File(clipDir, "clip-$hash.wav")
    }

    private fun softClip(value: Float): Float {
        val threshold = 0.92f
        return when {
            value > threshold -> threshold + (1f - threshold) * kotlin.math.tanh(
                (value - threshold) / (1f - threshold)
            )
            value < -threshold -> -threshold - (1f - threshold) * kotlin.math.tanh(
                (-value - threshold) / (1f - threshold)
            )
            else -> value
        }
    }
}
