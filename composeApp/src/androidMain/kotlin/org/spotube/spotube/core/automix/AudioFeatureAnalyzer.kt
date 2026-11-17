package org.spotube.spotube.core.automix

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class AudioFeatures(
    val bpm: Float,
    val beatOffsetSeconds: Float,
    val key: String,
    val keyRoot: Int,
    val keyMinor: Boolean,
    val energy: Float,
    val mixInFraction: Float,
    val mixOutFraction: Float
)

class AudioFeatureAnalyzer {
    private val cache = ConcurrentHashMap<String, AudioFeatures>()

    suspend fun analyze(trackId: String, url: String): AudioFeatures? =
        withContext(Dispatchers.IO) {
            cache[trackId]?.let { return@withContext it }
            val samples = decodeMono(url) ?: return@withContext null
            val features = computeFeatures(samples, ANALYSIS_SAMPLE_RATE) ?: return@withContext null
            cache[trackId] = features
            features
        }

    private fun decodeMono(url: String): FloatArray? {
        var extractor: MediaExtractor? = null
        var decoder: MediaCodec? = null
        return try {
            val source = MediaExtractor()
            extractor = source
            source.setDataSource(url, emptyMap())
            var audioTrack = -1
            var sourceFormat: MediaFormat? = null
            for (index in 0 until source.trackCount) {
                val format = source.getTrackFormat(index)
                val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("audio/")) {
                    audioTrack = index
                    sourceFormat = format
                    break
                }
            }
            if (audioTrack < 0 || sourceFormat == null) return null
            source.selectTrack(audioTrack)
            val mime = sourceFormat.getString(MediaFormat.KEY_MIME) ?: return null
            var channelCount = sourceFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                .coerceAtLeast(1)
            var sampleRate = sourceFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                .coerceAtLeast(8_000)
            var decimation = (sampleRate / ANALYSIS_SAMPLE_RATE).coerceAtLeast(1)

            val codec = MediaCodec.createDecoderByType(mime)
            decoder = codec
            codec.configure(sourceFormat, null, null, 0)
            codec.start()

            val maxSamples = (ANALYSIS_SAMPLE_RATE.toLong() * MAX_ANALYSIS_SECONDS).toInt()
            val output = FloatArray(maxSamples)
            var outputSize = 0
            var frameCounter = 0
            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            var pcmFloat = false

            while (!outputDone && outputSize < maxSamples) {
                if (!inputDone) {
                    val inputIndex = codec.dequeueInputBuffer(10_000)
                    if (inputIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputIndex) ?: continue
                        val sampleSize = source.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(
                                inputIndex,
                                0,
                                0,
                                0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(
                                inputIndex,
                                0,
                                sampleSize,
                                source.sampleTime,
                                0
                            )
                            source.advance()
                        }
                    }
                }

                when (val outputIndex = codec.dequeueOutputBuffer(info, 10_000)) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val format = codec.outputFormat
                        channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                            .coerceAtLeast(1)
                        sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                            .coerceAtLeast(8_000)
                        decimation = (sampleRate / ANALYSIS_SAMPLE_RATE).coerceAtLeast(1)
                        pcmFloat = runCatching {
                            format.getInteger(MediaFormat.KEY_PCM_ENCODING)
                        }.getOrDefault(AudioFormat.ENCODING_PCM_16BIT) ==
                            AudioFormat.ENCODING_PCM_FLOAT
                    }

                    MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                    MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> Unit
                    else -> if (outputIndex >= 0) {
                        if (info.size > 0) {
                            val buffer = codec.getOutputBuffer(outputIndex)
                            if (buffer != null) {
                                buffer.position(info.offset)
                                buffer.limit(info.offset + info.size)
                                if (pcmFloat) {
                                    val floats = buffer.order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
                                    while (floats.hasRemaining()) {
                                        var mono = 0f
                                        for (channel in 0 until channelCount) {
                                            if (floats.hasRemaining()) mono += floats.get()
                                        }
                                        mono /= channelCount
                                        if (frameCounter % decimation == 0 && outputSize < maxSamples) {
                                            output[outputSize++] = mono
                                        }
                                        frameCounter++
                                    }
                                } else {
                                    val shorts = buffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                                    while (shorts.hasRemaining()) {
                                        var mono = 0f
                                        for (channel in 0 until channelCount) {
                                            if (shorts.hasRemaining()) {
                                                mono += shorts.get() / 32768f
                                            }
                                        }
                                        mono /= channelCount
                                        if (frameCounter % decimation == 0 && outputSize < maxSamples) {
                                            output[outputSize++] = mono
                                        }
                                        frameCounter++
                                    }
                                }
                            }
                        }
                        codec.releaseOutputBuffer(outputIndex, false)
                        if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            outputDone = true
                        }
                    }
                }
            }
            if (outputSize < ANALYSIS_SAMPLE_RATE * 10) return null
            output.copyOf(outputSize)
        } catch (_: Exception) {
            null
        } finally {
            runCatching { decoder?.stop() }
            runCatching { decoder?.release() }
            runCatching { extractor?.release() }
        }
    }

    private fun computeFeatures(samples: FloatArray, sampleRate: Int): AudioFeatures? {
        val fftSize = 1024
        val hop = 256
        if (samples.size < fftSize * 8) return null
        val frameCount = (samples.size - fftSize) / hop
        val frameRate = sampleRate.toFloat() / hop
        val window = FloatArray(fftSize) { index ->
            (0.5 - 0.5 * cos(2.0 * PI * index / (fftSize - 1))).toFloat()
        }
        val real = FloatArray(fftSize)
        val imag = FloatArray(fftSize)
        val previousMagnitude = FloatArray(fftSize / 2)
        val flux = FloatArray(frameCount)
        val chroma = DoubleArray(12)

        for (frame in 0 until frameCount) {
            val offset = frame * hop
            for (index in 0 until fftSize) {
                real[index] = samples[offset + index] * window[index]
                imag[index] = 0f
            }
            fft(real, imag)
            for (bin in 1 until fftSize / 2) {
                val magnitude = sqrt(
                    (real[bin] * real[bin] + imag[bin] * imag[bin]).toDouble()
                ).toFloat()
                val difference = magnitude - previousMagnitude[bin]
                if (difference > 0f) flux[frame] += difference
                previousMagnitude[bin] = magnitude
                val frequency = bin.toFloat() * sampleRate / fftSize
                if (frequency in 80f..2000f) {
                    val midi = 69.0 + 12.0 * ln(frequency / 440.0) / ln(2.0)
                    val pitchClass = ((midi.roundToInt() % 12) + 12) % 12
                    chroma[pitchClass] += magnitude.toDouble()
                }
            }
        }

        var mean = 0.0
        for (value in flux) mean += value
        mean /= flux.size
        var variance = 0.0
        for (value in flux) {
            val delta = value - mean
            variance += delta * delta
        }
        val deviation = sqrt(variance / flux.size).toFloat().coerceAtLeast(1e-6f)
        for (index in flux.indices) {
            flux[index] = ((flux[index] - mean.toFloat()) / deviation).coerceAtLeast(0f)
        }

        val minLag = (frameRate * 60f / 200f).toInt().coerceAtLeast(2)
        val maxLag = (frameRate * 60f / 60f).toInt().coerceAtLeast(minLag + 1)
        var bestLag = 0
        var bestScore = -Float.MAX_VALUE
        for (lag in minLag..maxLag) {
            var score = 0f
            for (index in 0 until flux.size - lag) {
                score += flux[index] * flux[index + lag]
            }
            val bpm = frameRate * 60f / lag
            val preference = when {
                bpm in 80f..160f -> 1.08f
                bpm in 70f..180f -> 1.0f
                else -> 0.92f
            }
            score *= preference
            if (score > bestScore) {
                bestScore = score
                bestLag = lag
            }
        }
        if (bestLag == 0) return null

        var numerator = 0f
        var denominator = 0f
        val before = autocorrelation(flux, bestLag - 1)
        val center = autocorrelation(flux, bestLag)
        val after = autocorrelation(flux, bestLag + 1)
        denominator = before - 2f * center + after
        if (abs(denominator) > 1e-6f) {
            numerator = 0.5f * (before - after) / denominator
        }
        val refinedLag = bestLag + numerator.coerceIn(-0.5f, 0.5f)
        var bpm = frameRate * 60f / refinedLag
        while (bpm < 70f) bpm *= 2f
        while (bpm > 180f) bpm /= 2f

        var bestPhase = 0
        var bestPhaseScore = -Float.MAX_VALUE
        val lag = bestLag
        for (phase in 0 until lag) {
            var score = 0f
            var index = phase
            while (index < flux.size) {
                score += flux[index]
                index += lag
            }
            if (score > bestPhaseScore) {
                bestPhaseScore = score
                bestPhase = phase
            }
        }
        val beatOffset = bestPhase / frameRate

        val keyResult = detectKey(chroma)
        val totalRms = sqrt(samples.fold(0.0) { acc, value ->
            acc + value * value
        } / samples.size).toFloat()
        val energy = (totalRms / 0.12f).coerceIn(0f, 1f)

        val segmentSeconds = 2
        val segmentSamples = sampleRate * segmentSeconds
        val segmentCount = (samples.size / segmentSamples).coerceAtLeast(1)
        val segmentEnergy = FloatArray(segmentCount)
        for (segment in 0 until segmentCount) {
            var sum = 0.0
            val start = segment * segmentSamples
            val end = ((segment + 1) * segmentSamples).coerceAtMost(samples.size)
            for (index in start until end) {
                sum += samples[index] * samples[index]
            }
            segmentEnergy[segment] = sqrt(sum / (end - start).coerceAtLeast(1)).toFloat()
        }
        val peakEnergy = segmentEnergy.maxOrNull() ?: 0f
        var peakIndex = 0
        for (segment in segmentEnergy.indices) {
            if (segmentEnergy[segment] >= peakEnergy) {
                peakIndex = segment
                break
            }
        }
        val totalSeconds = samples.size.toFloat() / sampleRate
        val mixInFraction = ((peakIndex * segmentSeconds) / totalSeconds)
            .coerceIn(0.18f, 0.45f)
        var lastHigh = segmentCount - 1
        for (segment in segmentCount - 1 downTo 0) {
            if (segmentEnergy[segment] >= peakEnergy * 0.55f) {
                lastHigh = segment
                break
            }
        }
        val mixOutFraction = (((lastHigh + 1) * segmentSeconds) / totalSeconds)
            .coerceIn(0.65f, 0.9f)

        return AudioFeatures(
            bpm = bpm,
            beatOffsetSeconds = beatOffset,
            key = keyResult.label,
            keyRoot = keyResult.root,
            keyMinor = keyResult.minor,
            energy = energy,
            mixInFraction = mixInFraction,
            mixOutFraction = mixOutFraction
        )
    }

    private fun autocorrelation(values: FloatArray, lag: Int): Float {
        if (lag <= 0 || lag >= values.size) return 0f
        var score = 0f
        for (index in 0 until values.size - lag) {
            score += values[index] * values[index + lag]
        }
        return score
    }

    private data class KeyResult(val label: String, val root: Int, val minor: Boolean)

    private fun detectKey(chroma: DoubleArray): KeyResult {
        val total = chroma.sum().coerceAtLeast(1e-9)
        val normalized = DoubleArray(12) { chroma[it] / total }
        var bestRoot = 0
        var bestMinor = false
        var bestScore = -Double.MAX_VALUE
        for (root in 0 until 12) {
            for (minor in listOf(false, true)) {
                val profile = if (minor) MINOR_PROFILE else MAJOR_PROFILE
                var score = 0.0
                for (index in 0 until 12) {
                    score += normalized[(root + index) % 12] * profile[index]
                }
                if (score > bestScore) {
                    bestScore = score
                    bestRoot = root
                    bestMinor = minor
                }
            }
        }
        val label = if (bestMinor) {
            MINOR_CAMELOT[bestRoot]
        } else {
            MAJOR_CAMELOT[bestRoot]
        }
        return KeyResult(label, bestRoot, bestMinor)
    }

    private fun fft(real: FloatArray, imag: FloatArray) {
        val size = real.size
        var j = 0
        for (i in 1 until size) {
            var bit = size shr 1
            while ((j and bit) != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                val tempReal = real[i]
                real[i] = real[j]
                real[j] = tempReal
                val tempImag = imag[i]
                imag[i] = imag[j]
                imag[j] = tempImag
            }
        }
        var length = 2
        while (length <= size) {
            val angle = -2.0 * PI / length
            val wReal = cos(angle).toFloat()
            val wImag = sin(angle).toFloat()
            var start = 0
            while (start < size) {
                var currentReal = 1f
                var currentImag = 0f
                for (offset in 0 until length / 2) {
                    val even = start + offset
                    val odd = even + length / 2
                    val oddReal = real[odd] * currentReal - imag[odd] * currentImag
                    val oddImag = real[odd] * currentImag + imag[odd] * currentReal
                    real[odd] = real[even] - oddReal
                    imag[odd] = imag[even] - oddImag
                    real[even] += oddReal
                    imag[even] += oddImag
                    val nextReal = currentReal * wReal - currentImag * wImag
                    currentImag = currentReal * wImag + currentImag * wReal
                    currentReal = nextReal
                }
                start += length
            }
            length = length shl 1
        }
    }

    companion object {
        private const val ANALYSIS_SAMPLE_RATE = 11_025
        private const val MAX_ANALYSIS_SECONDS = 75L

        private val MAJOR_PROFILE = doubleArrayOf(
            6.35, 2.23, 3.48, 2.33, 4.38, 4.09, 2.52, 5.19, 2.39, 3.66, 2.29, 2.88
        )
        private val MINOR_PROFILE = doubleArrayOf(
            6.33, 2.68, 3.52, 5.38, 2.60, 3.53, 2.54, 4.75, 3.98, 2.69, 3.34, 3.17
        )
        private val MAJOR_CAMELOT = arrayOf(
            "8B", "3B", "10B", "5B", "12B", "7B",
            "2B", "9B", "4B", "11B", "6B", "1B"
        )
        private val MINOR_CAMELOT = arrayOf(
            "5A", "12A", "7A", "2A", "9A", "4A",
            "11A", "6A", "1A", "8A", "3A", "10A"
        )
    }
}
