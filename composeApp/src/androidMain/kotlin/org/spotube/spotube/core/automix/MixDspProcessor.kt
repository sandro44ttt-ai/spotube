package org.spotube.spotube.core.automix

import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import android.media.AudioFormat as AndroidAudioFormat
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.AudioProcessor.UnhandledAudioFormatException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Lightweight per-deck DSP used by Auto Mix for bass swaps, filter sweeps and echo outs.
 * Neutral defaults are bit-transparent so playback stays untouched outside transitions.
 */
class MixDspProcessor : BaseAudioProcessor() {
    @Volatile var gain: Float = 1f
    @Volatile var highPassHz: Float = 20f
    @Volatile var lowPassHz: Float = 20_000f
    @Volatile var echoMix: Float = 0f
    @Volatile var echoFeedback: Float = 0f

    private var channels = 2
    private var encoding = AndroidAudioFormat.ENCODING_PCM_16BIT
    private var filters = emptyArray<Biquad>()
    private var echoBuffer = FloatArray(0)
    private var echoWrite = 0
    private var echoDelaySamples = 0
    private var echoChannelCount = 0

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        if (
            inputAudioFormat.encoding != AndroidAudioFormat.ENCODING_PCM_16BIT &&
            inputAudioFormat.encoding != AndroidAudioFormat.ENCODING_PCM_FLOAT
        ) {
            throw UnhandledAudioFormatException(inputAudioFormat)
        }
        channels = inputAudioFormat.channelCount
        encoding = inputAudioFormat.encoding
        lastSampleRate = inputAudioFormat.sampleRate
        filters = Array(channels) { Biquad() }
        configureEcho(inputAudioFormat.sampleRate)
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return
        val output = replaceOutputBuffer(remaining)
        output.order(ByteOrder.LITTLE_ENDIAN)
        val neutral = gain == 1f &&
            highPassHz <= 20f &&
            lowPassHz >= 20_000f &&
            echoMix <= 0.001f
        if (neutral) {
            output.put(inputBuffer)
            inputBuffer.position(inputBuffer.limit())
            output.flip()
            return
        }

        val frameCount = remaining / (channels * bytesPerSample())
        if (encoding == AndroidAudioFormat.ENCODING_PCM_FLOAT) {
            val input = inputBuffer.order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
            val out = output.asFloatBuffer()
            for (frame in 0 until frameCount) {
                for (channel in 0 until channels) {
                    val value = processSample(channel, input.get())
                    out.put(value.coerceIn(-1f, 1f))
                }
            }
        } else {
            val input = inputBuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
            val out = output.asShortBuffer()
            for (frame in 0 until frameCount) {
                for (channel in 0 until channels) {
                    val sample = input.get() / 32768f
                    val value = processSample(channel, sample)
                    out.put((value.coerceIn(-1f, 1f) * 32767f).toInt().toShort())
                }
            }
        }
        inputBuffer.position(inputBuffer.limit())
        output.flip()
    }

    override fun onFlush() {
        filters.forEach { it.reset() }
        echoBuffer.fill(0f)
        echoWrite = 0
    }

    override fun onReset() {
        filters = emptyArray()
        echoBuffer = FloatArray(0)
        echoWrite = 0
        echoDelaySamples = 0
        echoChannelCount = 0
    }

    fun resetEffects() {
        gain = 1f
        highPassHz = 20f
        lowPassHz = 20_000f
        echoMix = 0f
        echoFeedback = 0f
    }

    private fun bytesPerSample(): Int =
        if (encoding == AndroidAudioFormat.ENCODING_PCM_FLOAT) 4 else 2

    private fun configureEcho(sampleRate: Int) {
        val maxSamples = (sampleRate * 0.75f).toInt().coerceAtLeast(1)
        echoChannelCount = channels
        echoDelaySamples = (sampleRate * 0.28f).toInt().coerceAtLeast(1)
        echoBuffer = FloatArray(maxSamples * channels)
        echoWrite = 0
    }

    private fun processSample(channel: Int, input: Float): Float {
        var value = input
        val filter = filters.getOrNull(channel)
        if (filter != null) {
            filter.update(highPassHz, lowPassHz, lastSampleRate)
            value = filter.process(value, highPassHz > 20f || lowPassHz < 20_000f)
        }
        value *= gain
        if (echoMix > 0.001f && echoBuffer.isNotEmpty()) {
            val readIndex = ((echoWrite - echoDelaySamples * echoChannelCount) %
                echoBuffer.size + echoBuffer.size) % echoBuffer.size
            val delayed = echoBuffer[readIndex + channel]
            value = value * (1f - echoMix) + delayed * echoMix
            echoBuffer[echoWrite + channel] = input + delayed * echoFeedback
        }
        if (channel == echoChannelCount - 1 && echoBuffer.isNotEmpty()) {
            echoWrite = (echoWrite + echoChannelCount) % echoBuffer.size
        }
        return value
    }

    private var lastSampleRate = 44_100
        set(value) {
            if (field != value) {
                field = value
                filters.forEach { it.reset() }
            }
        }

    private class Biquad {
        private var b0 = 1f
        private var b1 = 0f
        private var b2 = 0f
        private var a1 = 0f
        private var a2 = 0f
        private var z1 = 0f
        private var z2 = 0f
        private var lastHigh = -1f
        private var lastLow = -1f
        private var lastRate = -1

        fun update(highPassHz: Float, lowPassHz: Float, sampleRate: Int) {
            if (highPassHz == lastHigh && lowPassHz == lastLow && sampleRate == lastRate) return
            lastHigh = highPassHz
            lastLow = lowPassHz
            lastRate = sampleRate
            val rate = sampleRate.toFloat().coerceAtLeast(8_000f)
            val high = highPassHz.coerceIn(20f, rate * 0.45f)
            val low = lowPassHz.coerceIn(high + 20f, rate * 0.45f)
            if (high > 20.1f) {
                setHighPass(high, rate)
            } else if (low < 19_999f) {
                setLowPass(low, rate)
            } else {
                b0 = 1f
                b1 = 0f
                b2 = 0f
                a1 = 0f
                a2 = 0f
            }
        }

        fun process(input: Float, active: Boolean): Float {
            if (!active) return input
            val output = b0 * input + z1
            z1 = b1 * input - a1 * output + z2
            z2 = b2 * input - a2 * output
            return output
        }

        fun reset() {
            z1 = 0f
            z2 = 0f
        }

        private fun setHighPass(frequency: Float, rate: Float) {
            val omega = 2.0 * PI * frequency / rate
            val alpha = sin(omega) / (2.0 * 0.7071)
            val cosOmega = cos(omega)
            val a0 = 1.0 + alpha
            b0 = ((1.0 + cosOmega) / 2.0 / a0).toFloat()
            b1 = (-(1.0 + cosOmega) / a0).toFloat()
            b2 = b0
            a1 = (-2.0 * cosOmega / a0).toFloat()
            a2 = ((1.0 - alpha) / a0).toFloat()
        }

        private fun setLowPass(frequency: Float, rate: Float) {
            val omega = 2.0 * PI * frequency / rate
            val alpha = sin(omega) / (2.0 * 0.7071)
            val cosOmega = cos(omega)
            val a0 = 1.0 + alpha
            b0 = ((1.0 - cosOmega) / 2.0 / a0).toFloat()
            b1 = ((1.0 - cosOmega) / a0).toFloat()
            b2 = b0
            a1 = (-2.0 * cosOmega / a0).toFloat()
            a2 = ((1.0 - alpha) / a0).toFloat()
        }
    }
}
