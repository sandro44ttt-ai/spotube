package org.spotube.spotube.core.automix

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val CLIP_RENDER_VERSION = 4

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
        pitchSemitones: Int,
        role: MashupRole
    ): File? {
        val file = clipFile(
            currentId,
            nextId,
            outgoingCueMs,
            incomingCueMs,
            durationMs,
            tempoRatio,
            pitchSemitones,
            role
        )
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
        role: MashupRole,
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
            pitchSemitones,
            role
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
        val rendered = mix(current, next, frames, role)
        writeWav(output, rendered, frames)
        Log.d("AutoMix", "stem clip rendered ${output.name} bytes=${output.length()}")
        onProgress(1f)
        output
    }

    private fun mix(
        current: StemSeparator.StemWindow,
        next: StemSeparator.StemWindow,
        frames: Int,
        role: MashupRole
    ): ShortArray {
        val result = FloatArray(frames * 2)
        val loopFrames = (frames / 4).coerceAtLeast(1)
        val loopFadeFrames = (StemSeparator.SAMPLE_RATE * 0.025f).toInt().coerceAtLeast(1)
        val echoDelayFrames = (StemSeparator.SAMPLE_RATE * 0.28f).toInt().coerceAtLeast(1)
        val echoBuffer = FloatArray(frames * 2)
        for (frame in 0 until frames) {
            val t = frame.toFloat() / frames.toFloat()
            val gains = when (role) {
                MashupRole.AInstrumentalBVocals -> floatArrayOf(
                    1f - DjFade.smoothstep(DjFade.window(t, 0.78f, 1f)),
                    DjFade.fadeOut(t, 0f, 0.18f),
                    DjFade.fadeIn(t, 0.82f, 1f),
                    DjFade.fadeIn(t, 0.18f, 0.42f)
                )
                MashupRole.BInstrumentalAVocals -> floatArrayOf(
                    0f,
                    DjFade.fadeOut(t, 0.15f, 0.45f),
                    1f,
                    0f
                )
                MashupRole.InstrumentalCrossfade -> floatArrayOf(
                    DjFade.equalPowerOut(t, 0.15f, 0.85f),
                    0f,
                    DjFade.equalPowerIn(t, 0.15f, 0.85f),
                    0f
                )
                MashupRole.None -> floatArrayOf(
                    DjFade.equalPowerOut(t, 0.10f, 0.90f),
                    DjFade.fadeOut(t, 0.20f, 0.42f),
                    DjFade.equalPowerIn(t, 0.10f, 0.90f),
                    DjFade.fadeIn(t, 0.55f, 0.75f)
                )
            }
            for (channel in 0..1) {
                val index = frame * 2 + channel
                val currentInstrumental = if (role == MashupRole.AInstrumentalBVocals) {
                    loopedSample(
                        current.instrumental,
                        frame,
                        loopFrames,
                        loopFadeFrames,
                        channel
                    )
                } else {
                    current.instrumental.getOrElse(index) { 0f }
                }
                var sample = 0f
                sample += currentInstrumental * gains[0]
                sample += current.vocals.getOrElse(index) { 0f } * gains[1]
                sample += next.instrumental.getOrElse(index) { 0f } * gains[2]
                sample += next.vocals.getOrElse(index) { 0f } * gains[3]
                if (role != MashupRole.None) {
                    val echoRead = frame - echoDelayFrames
                    val echo = if (echoRead >= 0) {
                        echoBuffer[echoRead * 2 + channel] * 0.08f
                    } else {
                        0f
                    }
                    echoBuffer[index] = current.vocals.getOrElse(index) { 0f } * gains[1] +
                        echo * 0.18f
                    sample += echo
                }
                result[index] = softClip(sample)
            }
        }
        val short = ShortArray(frames * 2)
        for (index in result.indices) {
            short[index] = (result[index].coerceIn(-1f, 1f) * 32767f).roundToInt().toShort()
        }
        return short
    }

    private fun loopedSample(
        samples: FloatArray,
        frame: Int,
        loopFrames: Int,
        fadeFrames: Int,
        channel: Int
    ): Float {
        val position = frame % loopFrames
        val index = position * 2 + channel
        val value = samples.getOrElse(index) { 0f }
        if (position < loopFrames - fadeFrames) return value
        val start = samples.getOrElse(channel) { 0f }
        val alpha = ((position - (loopFrames - fadeFrames)).toFloat() / fadeFrames)
            .coerceIn(0f, 1f)
        return value * (1f - alpha) + start * alpha
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
        pitchSemitones: Int,
        role: MashupRole
    ): File {
        val key = "$CLIP_RENDER_VERSION|$currentId|$nextId|$outgoingCueMs|$incomingCueMs|" +
            "$durationMs|${(tempoRatio * 1000).roundToInt()}|$pitchSemitones|${role.name}"
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
