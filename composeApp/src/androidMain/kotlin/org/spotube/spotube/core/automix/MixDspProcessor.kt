package org.spotube.spotube.core.automix

import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import android.media.AudioFormat as AndroidAudioFormat
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.AudioProcessor.UnhandledAudioFormatException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.tanh
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
    @Volatile var midDipDb: Float = 0f
    @Volatile var midFrequencyHz: Float = 1_500f
    @Volatile var centerAttenuationDb: Float = 0f
    @Volatile var limiterEnabled: Boolean = true
    @Volatile var loopBpm: Float = 0f
    @Volatile var loopBeats: Int = 0
    @Volatile var loopRoll: Boolean = false

    private var channels = 2
    private var encoding = AndroidAudioFormat.ENCODING_PCM_16BIT
    private var highPass1 = emptyArray<Biquad>()
    private var highPass2 = emptyArray<Biquad>()
    private var lowPassFilters = emptyArray<Biquad>()
    private var peakFilters = emptyArray<Biquad>()
    private var echoBuffer = FloatArray(0)
    private var echoWrite = 0
    private var echoDelaySamples = 0
    private var echoChannelCount = 0
    private var loopBuffer = FloatArray(0)
    private var loopFrames = 0
    private var loopRead = 0
    private var loopWrite = 0
    private var loopCapturing = false
    private var loopActive = false
    private var loopStopping = false
    private var loopFadeRemaining = 0
    private var loopFadeTotal = 1

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
        highPass1 = Array(channels) { Biquad() }
        highPass2 = Array(channels) { Biquad() }
        lowPassFilters = Array(channels) { Biquad() }
        peakFilters = Array(channels) { Biquad() }
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
            echoMix <= 0.001f &&
            midDipDb == 0f &&
            centerAttenuationDb <= 0.01f &&
            !loopActive
        if (neutral) {
            output.put(inputBuffer)
            inputBuffer.position(inputBuffer.limit())
            output.flip()
            return
        }

        val frameCount = remaining / (channels * bytesPerSample())
        val raw = FloatArray(channels)
        val frame = FloatArray(channels)
        if (encoding == AndroidAudioFormat.ENCODING_PCM_FLOAT) {
            val input = inputBuffer.order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
            val out = output.asFloatBuffer()
            for (index in 0 until frameCount) {
                for (channel in 0 until channels) raw[channel] = input.get()
                applyLoop(raw, frame)
                for (channel in 0 until channels) {
                    frame[channel] = processSample(channel, frame[channel])
                }
                applyCenterAttenuation(frame)
                for (channel in 0 until channels) out.put(limit(frame[channel]))
            }
        } else {
            val input = inputBuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
            val out = output.asShortBuffer()
            for (index in 0 until frameCount) {
                for (channel in 0 until channels) raw[channel] = input.get() / 32768f
                applyLoop(raw, frame)
                for (channel in 0 until channels) {
                    frame[channel] = processSample(channel, frame[channel])
                }
                applyCenterAttenuation(frame)
                for (channel in 0 until channels) {
                    out.put((limit(frame[channel]) * 32767f).toInt().toShort())
                }
            }
        }
        inputBuffer.position(inputBuffer.limit())
        output.position(frameCount * channels * bytesPerSample())
        output.flip()
    }

    override fun onFlush() {
        highPass1.forEach { it.reset() }
        highPass2.forEach { it.reset() }
        lowPassFilters.forEach { it.reset() }
        peakFilters.forEach { it.reset() }
        echoBuffer.fill(0f)
        echoWrite = 0
        resetLoop()
    }

    override fun onReset() {
        highPass1 = emptyArray()
        highPass2 = emptyArray()
        lowPassFilters = emptyArray()
        peakFilters = emptyArray()
        echoBuffer = FloatArray(0)
        echoWrite = 0
        echoDelaySamples = 0
        echoChannelCount = 0
        loopBuffer = FloatArray(0)
        loopFrames = 0
        loopRead = 0
        loopWrite = 0
        loopCapturing = false
        loopActive = false
        loopStopping = false
        loopFadeRemaining = 0
    }

    fun resetEffects() {
        gain = 1f
        highPassHz = 20f
        lowPassHz = 20_000f
        echoMix = 0f
        echoFeedback = 0f
        midDipDb = 0f
        centerAttenuationDb = 0f
        limiterEnabled = true
        resetLoop()
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

    fun startLoop(bpm: Float, beats: Int, roll: Boolean) {
        if (bpm <= 0f || beats <= 0) return
        val frames = (lastSampleRate * 60f / bpm * beats)
            .toInt()
            .coerceIn(256, lastSampleRate * 20)
        val needed = frames * channels
        if (loopBuffer.size < needed) loopBuffer = FloatArray(needed)
        else loopBuffer.fill(0f, 0, needed)
        loopFrames = frames
        loopRead = 0
        loopWrite = 0
        loopCapturing = true
        loopActive = true
        loopStopping = false
        loopRoll = roll
        loopFadeRemaining = 0
        loopFadeTotal = 1
    }

    fun stopLoop() {
        if (!loopActive) return
        if (loopCapturing) {
            loopActive = false
            loopCapturing = false
            return
        }
        loopStopping = true
        loopFadeTotal = (lastSampleRate * 0.035f).toInt().coerceAtLeast(1)
        loopFadeRemaining = loopFadeTotal
    }

    private fun resetLoop() {
        loopActive = false
        loopCapturing = false
        loopStopping = false
        loopFrames = 0
        loopRead = 0
        loopWrite = 0
        loopFadeRemaining = 0
    }

    private fun applyLoop(raw: FloatArray, frame: FloatArray) {
        if (!loopActive) {
            raw.copyInto(frame)
            return
        }
        if (loopCapturing) {
            raw.copyInto(frame)
            val end = loopFrames * channels
            for (channel in 0 until channels) {
                if (loopWrite + channel < loopBuffer.size) {
                    loopBuffer[loopWrite + channel] = raw[channel]
                }
            }
            loopWrite += channels
            if (loopWrite >= end) {
                loopCapturing = false
                loopWrite = 0
                loopRead = 0
            }
            return
        }
        val end = loopFrames * channels
        for (channel in 0 until channels) {
            frame[channel] = if (loopRead + channel < loopBuffer.size) {
                loopBuffer[loopRead + channel]
            } else {
                0f
            }
        }
        loopRead += channels
        if (loopRead >= end) {
            loopRead = 0
            if (loopRoll && loopFrames > 512) {
                loopFrames /= 2
            }
        }
        if (loopStopping) {
            val alpha = loopFadeRemaining.toFloat() / loopFadeTotal.toFloat()
            for (channel in 0 until channels) {
                frame[channel] = frame[channel] * alpha + raw[channel] * (1f - alpha)
            }
            loopFadeRemaining--
            if (loopFadeRemaining <= 0) {
                loopActive = false
                loopStopping = false
            }
        }
    }
    private fun processSample(channel: Int, input: Float): Float {
        var value = input
        if (highPassHz > 20.1f) {
            val first = highPass1.getOrNull(channel)
            val second = highPass2.getOrNull(channel)
            if (first != null && second != null) {
                first.updateHighPass(highPassHz, lastSampleRate)
                second.updateHighPass(highPassHz, lastSampleRate)
                value = second.process(first.process(value))
            }
        }
        if (lowPassHz < 19_999f) {
            val filter = lowPassFilters.getOrNull(channel)
            if (filter != null) {
                filter.updateLowPass(lowPassHz, lastSampleRate)
                value = filter.process(value)
            }
        }
        if (midDipDb != 0f) {
            val filter = peakFilters.getOrNull(channel)
            if (filter != null) {
                filter.updatePeaking(midFrequencyHz, midDipDb, lastSampleRate)
                value = filter.process(value)
            }
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

    private fun applyCenterAttenuation(frame: FloatArray) {
        if (centerAttenuationDb <= 0.01f || frame.size != 2) return
        val attenuation = 10.0.pow(-centerAttenuationDb / 20.0).toFloat()
        val mid = (frame[0] + frame[1]) * 0.5f * attenuation
        val side = (frame[0] - frame[1]) * 0.5f
        frame[0] = mid + side
        frame[1] = mid - side
    }

    private fun limit(value: Float): Float {
        if (!limiterEnabled) return value.coerceIn(-1f, 1f)
        val threshold = 0.89f
        return when {
            value > threshold -> threshold + (1f - threshold) *
                tanh((value - threshold) / (1f - threshold))
            value < -threshold -> -threshold - (1f - threshold) *
                tanh((-value - threshold) / (1f - threshold))
            else -> value
        }
    }

    private var lastSampleRate = 44_100
        set(value) {
            if (field != value) {
                field = value
                highPass1.forEach { it.reset() }
                highPass2.forEach { it.reset() }
                lowPassFilters.forEach { it.reset() }
                peakFilters.forEach { it.reset() }
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
        private var lastA = Float.NaN
        private var lastB = Float.NaN
        private var lastRate = -1f

        fun updateHighPass(frequency: Float, sampleRate: Int) {
            val rate = sampleRate.toFloat().coerceAtLeast(8_000f)
            val f = frequency.coerceIn(20f, rate * 0.45f)
            if (f == lastA && rate == lastRate) return
            lastA = f
            lastRate = rate
            val omega = 2.0 * PI * f / rate
            val alpha = sin(omega) / (2.0 * 0.7071)
            val cosOmega = cos(omega)
            val a0 = 1.0 + alpha
            b0 = ((1.0 + cosOmega) / 2.0 / a0).toFloat()
            b1 = (-(1.0 + cosOmega) / a0).toFloat()
            b2 = b0
            a1 = (-2.0 * cosOmega / a0).toFloat()
            a2 = ((1.0 - alpha) / a0).toFloat()
        }

        fun updateLowPass(frequency: Float, sampleRate: Int) {
            val rate = sampleRate.toFloat().coerceAtLeast(8_000f)
            val f = frequency.coerceIn(100f, rate * 0.45f)
            if (f == lastA && rate == lastRate) return
            lastA = f
            lastRate = rate
            val omega = 2.0 * PI * f / rate
            val alpha = sin(omega) / (2.0 * 0.7071)
            val cosOmega = cos(omega)
            val a0 = 1.0 + alpha
            b0 = ((1.0 - cosOmega) / 2.0 / a0).toFloat()
            b1 = ((1.0 - cosOmega) / a0).toFloat()
            b2 = b0
            a1 = (-2.0 * cosOmega / a0).toFloat()
            a2 = ((1.0 - alpha) / a0).toFloat()
        }

        fun updatePeaking(frequency: Float, gainDb: Float, sampleRate: Int) {
            val rate = sampleRate.toFloat().coerceAtLeast(8_000f)
            val f = frequency.coerceIn(100f, rate * 0.45f)
            if (f == lastA && gainDb == lastB && rate == lastRate) return
            lastA = f
            lastB = gainDb
            lastRate = rate
            val omega = 2.0 * PI * f / rate
            val alpha = sin(omega) / (2.0 * 1.0)
            val cosOmega = cos(omega)
            val a = 10.0.pow(gainDb / 40.0)
            val a0 = 1.0 + alpha / a
            b0 = ((1.0 + alpha * a) / a0).toFloat()
            b1 = ((-2.0 * cosOmega) / a0).toFloat()
            b2 = ((1.0 - alpha * a) / a0).toFloat()
            a1 = ((-2.0 * cosOmega) / a0).toFloat()
            a2 = ((1.0 - alpha / a) / a0).toFloat()
        }

        fun process(input: Float): Float {
            val output = b0 * input + z1
            z1 = b1 * input - a1 * output + z2
            z2 = b2 * input - a2 * output
            return output
        }

        fun reset() {
            z1 = 0f
            z2 = 0f
        }
    }
}