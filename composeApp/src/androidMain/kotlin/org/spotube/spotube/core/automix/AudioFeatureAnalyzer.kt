package org.spotube.spotube.core.automix

import android.content.Context
import android.media.AudioFormat
import android.util.Log
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Process
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import java.io.File
import java.io.FileOutputStream
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

private data class MonoAudio(
    val samples: FloatArray,
    val sampleRate: Int
)

class AudioFeatureAnalyzer(
    private val context: Context,
    private val cache: MixAnalysisCache,
    private val httpClientFactory: org.spotube.spotube.core.network.HttpClientFactory,
    private val cacheManager: org.spotube.spotube.core.server.CacheManager
) {
    private val memory = ConcurrentHashMap<String, AudioFeatures>()

    suspend fun analyze(
        trackId: String,
        url: String,
        fullTrack: Boolean = true
    ): AudioFeatures? =
        withContext(Dispatchers.IO) {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            memory[trackId]?.let { cached ->
                if (!fullTrack || cached.analysisComplete) return@withContext cached
            }
            cache.get(trackId)?.let { cached ->
                if (!fullTrack || cached.analysisComplete) {
                    memory[trackId] = cached
                    return@withContext cached
                }
            }
            val file = downloadTrack(url) ?: return@withContext null
            val decoded = try {
                decodeMono(file.absolutePath, if (fullTrack) MAX_ANALYSIS_SECONDS else LIGHT_ANALYSIS_SECONDS)
            } finally {
                file.delete()
            } ?: return@withContext null
            val features = computeFeatures(decoded.samples, decoded.sampleRate, fullTrack)
                ?: return@withContext null
            val stamped = features.copy(
                analysisComplete = fullTrack,
                analyzedAtMs = System.currentTimeMillis()
            )
            memory[trackId] = stamped
            cache.put(trackId, stamped)
            stamped
        }

    private fun cachedTrackFile(trackId: String): File? {
        val path = cacheManager.findCachedEntry(trackId)?.first ?: return null
        val file = File(path.toString())
        return file.takeIf { it.exists() && it.length() > 32 * 1024L }
    }

    private suspend fun downloadTrack(url: String): File? {
        val file = File.createTempFile("automix-analysis-", ".audio", context.cacheDir)
        val client = httpClientFactory.create {
            install(HttpTimeout) {
                requestTimeoutMillis = 45_000
                connectTimeoutMillis = 8_000
                socketTimeoutMillis = 20_000
            }
        }
        return try {
            val completed = withTimeoutOrNull(50_000L) {
                client.prepareGet(url) {
                    headers.append(HttpHeaders.UserAgent, "Mozilla/5.0")
                    headers.append(HttpHeaders.Accept, "*/*")
                    headers.append(HttpHeaders.Range, "bytes=0-")
                    headers.append("Accept-Encoding", "identity")
                }.execute { response ->
                    if (!response.status.isSuccess()) {
                        Log.w("AutoMix", "analysis HTTP ${response.status.value} for $url")
                        return@execute false
                    }
                    var total = 0L
                    val channel = response.bodyAsChannel()
                    FileOutputStream(file).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (total < MAX_ANALYSIS_BYTES) {
                            val read = channel.readAvailable(buffer, 0, buffer.size)
                            if (read < 0) break
                            if (read > 0) {
                                output.write(buffer, 0, read)
                                total += read
                            }
                        }
                    }
                    Log.d("AutoMix", "analysis downloaded ${file.length()} bytes")
                    file.length() > 32 * 1024L
                }
            } ?: false
            if (completed) file else {
                file.delete()
                Log.w("AutoMix", "analysis download timeout for $url")
                null
            }
        } catch (error: Exception) {
            file.delete()
            Log.w("AutoMix", "analysis download failed: ${error.message}")
            null
        } finally {
            client.close()
        }
    }
    private fun decodeMono(url: String, maxSeconds: Long): MonoAudio? {
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
            var analysisRate = (sampleRate / decimation).coerceAtLeast(1)

            val codec = MediaCodec.createDecoderByType(mime)
            decoder = codec
            codec.configure(sourceFormat, null, null, 0)
            codec.start()

            var maxSamples = (analysisRate.toLong() * maxSeconds).toInt()
            var output = FloatArray(maxSamples)
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
                        analysisRate = (sampleRate / decimation).coerceAtLeast(1)
                        maxSamples = (analysisRate.toLong() * maxSeconds).toInt()
                        if (maxSamples > output.size) {
                            output = output.copyOf(maxSamples)
                        }
                        Log.d(
                            "AutoMix",
                            "analysis decode sourceRate=$sampleRate " +
                                "effectiveRate=$analysisRate decimation=$decimation"
                        )
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
            if (outputSize < analysisRate * 10) return null
            MonoAudio(output.copyOf(outputSize), analysisRate)
        } catch (error: Exception) {
            Log.w("AutoMix", "analysis decode failed: ${error.message}")
            null
        } finally {
            runCatching { decoder?.stop() }
            runCatching { decoder?.release() }
            runCatching { extractor?.release() }
        }
    }

    private fun computeFeatures(samples: FloatArray, sampleRate: Int, fullTrack: Boolean): AudioFeatures? {
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
        val previousLowMagnitude = FloatArray(fftSize / 2)
        val flux = FloatArray(frameCount)
        val lowFlux = FloatArray(frameCount)
        val chroma = DoubleArray(12)
        var midBandMagnitude = 0.0
        var totalMagnitude = 0.0
        var centroidWeighted = 0.0
        var lowBandMagnitude = 0.0
        var highBandMagnitude = 0.0
        var zeroCrossings = 0
        for (index in 1 until samples.size) {
            if ((samples[index - 1] < 0f) != (samples[index] < 0f)) zeroCrossings++
        }

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
                if (frequency in 20f..150f) {
                    val lowDifference = magnitude - previousLowMagnitude[bin]
                    if (lowDifference > 0f) lowFlux[frame] += lowDifference
                }
                previousLowMagnitude[bin] = magnitude
                val weighted = magnitude.toDouble()
                totalMagnitude += weighted
                centroidWeighted += frequency * weighted
                if (frequency < 250f) lowBandMagnitude += weighted
                if (frequency >= 4000f) highBandMagnitude += weighted
                if (frequency in 300f..3000f) midBandMagnitude += magnitude
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

        var lowMean = 0.0
        for (value in lowFlux) lowMean += value
        lowMean /= lowFlux.size
        var lowVariance = 0.0
        for (value in lowFlux) {
            val delta = value - lowMean
            lowVariance += delta * delta
        }
        val lowDeviation = sqrt(lowVariance / lowFlux.size)
            .toFloat()
            .coerceAtLeast(1e-6f)
        for (index in lowFlux.indices) {
            lowFlux[index] = ((lowFlux[index] - lowMean.toFloat()) / lowDeviation)
                .coerceAtLeast(0f)
        }

        val minLag = (frameRate * 60f / 200f).toInt().coerceAtLeast(2)
        val maxLag = (frameRate * 60f / 60f).toInt().coerceAtLeast(minLag + 1)
        var bestLag = 0
        var bestScore = -Float.MAX_VALUE
        for (lag in minLag..maxLag) {
            var score = 0f
            for (index in 0 until flux.size - lag) {
                score += flux[index] * flux[index + lag] * 0.35f +
                    lowFlux[index] * lowFlux[index + lag] * 0.65f
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

        val onsetBpm = estimateOnsetBpm(lowFlux, frameRate)
        var bpmConfidence = 0.55f
        if (onsetBpm != null) {
            val directDelta = abs(onsetBpm - bpm) / bpm
            val halfDelta = abs(bpm / 2f - onsetBpm) / (bpm / 2f)
            val doubleDelta = abs(bpm * 2f - onsetBpm) / (bpm * 2f)
            when {
                directDelta <= 0.07f -> {
                    bpm = bpm * 0.5f + onsetBpm * 0.5f
                    bpmConfidence = 0.92f
                }
                bpm > 135f && halfDelta <= 0.08f -> {
                    bpm = onsetBpm
                    bpmConfidence = 0.88f
                }
                bpm < 78f && doubleDelta <= 0.08f -> {
                    bpm = onsetBpm
                    bpmConfidence = 0.82f
                }
                else -> {
                    val nearest = listOf(bpm, bpm / 2f, bpm * 2f)
                        .minByOrNull { abs(it - onsetBpm) }
                    if (nearest != null &&
                        abs(nearest - onsetBpm) / onsetBpm <= 0.12f
                    ) {
                        bpm = nearest
                        bpmConfidence = 0.72f
                    } else {
                        bpmConfidence = 0.45f
                    }
                }
            }
        }
        while (bpm < 65f) bpm *= 2f
        while (bpm > 190f) bpm /= 2f

        var bestPhase = 0
        var bestPhaseScore = -Float.MAX_VALUE
        val lag = (frameRate * 60f / bpm)
            .roundToInt()
            .coerceIn(2, flux.size - 1)
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
        var transientPeak = bestPhase
        var transientPeakScore = -1f
        val searchFrom = (bestPhase - lag / 4).coerceAtLeast(0)
        val searchTo = (bestPhase + lag / 4).coerceAtMost(flux.size - 1)
        for (index in searchFrom..searchTo) {
            if (flux[index] > transientPeakScore) {
                transientPeakScore = flux[index]
                transientPeak = index
            }
        }
        val transientOffsetMs = ((transientPeak - bestPhase) * 1000f / frameRate)
            .coerceIn(-60f, 60f)
        val halfLag = (lag / 2).coerceAtLeast(1)
        val quarterLag = (lag / 4).coerceAtLeast(1)
        var earlyOffbeat = 0f
        var lateOffbeat = 0f
        var phase = bestPhase
        while (phase + lag < flux.size) {
            earlyOffbeat += flux[(phase + halfLag).coerceAtMost(flux.size - 1)]
            lateOffbeat += flux[(phase + halfLag + quarterLag).coerceAtMost(flux.size - 1)]
            phase += lag
        }
        val swingRatio = (lateOffbeat / (earlyOffbeat + lateOffbeat).coerceAtLeast(1e-6f))
            .coerceIn(0.3f, 0.7f)
        val downbeatConfidence = (bestPhaseScore / flux.size.toFloat().coerceAtLeast(1f))
            .coerceIn(0.2f, 1f)

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
        val introEnd = (segmentEnergy.indexOfFirst { it >= peakEnergy * 0.5f }
            .takeIf { it >= 0 } ?: 0
            ).let { index ->
            ((index * segmentSeconds) / totalSeconds).coerceIn(0.03f, 0.25f)
        }
        val dropFraction = ((peakIndex * segmentSeconds) / totalSeconds)
            .coerceIn(0.12f, 0.6f)
        var lastHigh = segmentCount - 1
        for (segment in segmentCount - 1 downTo 0) {
            if (segmentEnergy[segment] >= peakEnergy * 0.6f) {
                lastHigh = segment
                break
            }
        }
        val outroStart = (((lastHigh + 1) * segmentSeconds) / totalSeconds)
            .coerceIn(0.65f, 0.94f)
        val mixOutFraction = (outroStart - 0.01f).coerceIn(0.6f, 0.93f)
        val mixInFraction = if (dropFraction > 0.16f) {
            dropFraction
        } else {
            introEnd
        }
        val vocalness = (midBandMagnitude / totalMagnitude.coerceAtLeast(1e-9))
            .toFloat()
            .coerceIn(0f, 1f)
        val sections = buildSections(
            segmentEnergy = segmentEnergy,
            segmentSeconds = segmentSeconds,
            totalSeconds = totalSeconds,
            introEnd = introEnd,
            outroStart = outroStart,
            vocalness = vocalness,
            fullTrack = fullTrack
        )
        val safeMagnitude = totalMagnitude.coerceAtLeast(1e-9)
        val centroid = centroidWeighted / safeMagnitude
        val lowRatio = (lowBandMagnitude / safeMagnitude).toFloat()
        val highRatio = (highBandMagnitude / safeMagnitude).toFloat()
        val zcrRate = zeroCrossings.toFloat() / samples.size.coerceAtLeast(1)
        val onsetFrames = flux.count { it > 0.6f }
        val onsetRate = onsetFrames.toFloat() / totalSeconds.coerceAtLeast(1f)
        val density = ((onsetRate / 5f) * 0.65f + (highRatio / 0.3f) * 0.35f)
            .coerceIn(0f, 1f)
        val percussiveness = ((flux.count { it > 1.2f }.toFloat() /
            totalSeconds.coerceAtLeast(1f) / 2f) * 0.7f + (highRatio / 0.3f) * 0.3f)
            .coerceIn(0f, 1f)
        val brightness = sqrt(centroid / 8000.0).toFloat().coerceIn(0f, 1f)
        val warmth = (lowRatio / 0.45f).coerceIn(0f, 1f)
        val roughness = ((highRatio / 0.25f) * 0.65f + (zcrRate / 0.15f) * 0.35f)
            .coerceIn(0f, 1f)
        val meanSegment = segmentEnergy.average().toFloat().coerceAtLeast(1e-6f)
        val segmentVariance = segmentEnergy.fold(0.0) { acc, value ->
            val delta = value - meanSegment
            acc + delta * delta
        } / segmentEnergy.size.coerceAtLeast(1)
        val dynamicRange = (sqrt(segmentVariance).toFloat() / meanSegment / 1.2f)
            .coerceIn(0f, 1f)
        val acousticness = (1f - percussiveness * 0.4f - highRatio.toFloat() * 1.5f -
            density * 0.2f).coerceIn(0f, 1f)
        val valence = ((if (keyResult.minor) 0.35f else 0.65f) * 0.45f +
            brightness * 0.3f + energy * 0.25f).coerceIn(0f, 1f)
        val danceability = (bpmConfidence * 0.45f +
            (if (bpm in 80f..150f) 0.25f else 0.1f) +
            density * 0.2f + energy * 0.1f).coerceIn(0f, 1f)
        val vibe = VibeProfile(
            brightness = brightness,
            density = density,
            percussiveness = percussiveness,
            roughness = roughness,
            warmth = warmth,
            acousticness = acousticness,
            dynamicRange = dynamicRange,
            valence = valence,
            danceability = danceability
        )

        return AudioFeatures(
            bpm = bpm,
            bpmConfidence = bpmConfidence,
            beatOffsetSeconds = beatOffset,
            key = keyResult.label,
            keyRoot = keyResult.root,
            keyMinor = keyResult.minor,
            energy = energy,
            mixInFraction = mixInFraction,
            mixOutFraction = mixOutFraction,
            introEndFraction = introEnd,
            outroStartFraction = outroStart,
            dropFraction = dropFraction,
            vocalness = vocalness,
            confidence = (bpmConfidence * 0.6f + 0.4f).coerceIn(0.3f, 0.95f),
            durationSeconds = totalSeconds,
            swingRatio = swingRatio,
            transientOffsetMs = transientOffsetMs,
            downbeatConfidence = downbeatConfidence,
            vibe = vibe,
            sections = sections,
            analysisComplete = fullTrack
        )
    }

    private fun buildSections(
        segmentEnergy: FloatArray,
        segmentSeconds: Int,
        totalSeconds: Float,
        introEnd: Float,
        outroStart: Float,
        vocalness: Float,
        fullTrack: Boolean
    ): List<AudioSection> {
        if (segmentEnergy.isEmpty() || totalSeconds <= 0f) return emptyList()
        val peak = segmentEnergy.maxOrNull()?.coerceAtLeast(1e-6f) ?: return emptyList()
        val result = mutableListOf<AudioSection>()
        for (index in segmentEnergy.indices) {
            val start = (index * segmentSeconds / totalSeconds).coerceIn(0f, 1f)
            val end = (((index + 1) * segmentSeconds) / totalSeconds).coerceIn(0f, 1f)
            if (end <= start) continue
            val normalized = (segmentEnergy[index] / peak).coerceIn(0f, 1f)
            val type = when {
                start < introEnd -> SectionType.Intro
                start >= outroStart -> SectionType.Outro
                normalized < 0.42f -> SectionType.Breakdown
                normalized > 0.8f && vocalness > 0.46f -> SectionType.Chorus
                normalized > 0.8f -> SectionType.Drop
                else -> SectionType.Verse
            }
            val sectionVocalness = (vocalness * (0.65f + normalized * 0.35f))
                .coerceIn(0f, 1f)
            val previous = result.lastOrNull()
            if (previous != null && previous.type == type && previous.endFraction >= start - 0.02f) {
                result[result.lastIndex] = previous.copy(
                    endFraction = end,
                    energy = (previous.energy + normalized) / 2f,
                    vocalness = (previous.vocalness + sectionVocalness) / 2f
                )
            } else {
                result += AudioSection(
                    startFraction = start,
                    endFraction = end,
                    type = type,
                    energy = normalized,
                    vocalness = sectionVocalness
                )
            }
        }
        if (!fullTrack && result.isNotEmpty()) {
            return result.filter { it.type != SectionType.Outro }
        }
        return result
    }

    private fun estimateOnsetBpm(lowFlux: FloatArray, frameRate: Float): Float? {
        if (lowFlux.size < 8) return null
        val peaks = mutableListOf<Int>()
        var index = 1
        while (index < lowFlux.size - 1) {
            if (lowFlux[index] >= 0.32f &&
                lowFlux[index] >= lowFlux[index - 1] &&
                lowFlux[index] >= lowFlux[index + 1]
            ) {
                peaks += index
                index += 2
            } else {
                index++
            }
        }
        if (peaks.size < 4) return null
        val minInterval = (frameRate * 60f / 190f).toInt().coerceAtLeast(2)
        val maxInterval = (frameRate * 60f / 65f).toInt()
        val intervals = peaks.zipWithNext { first, second -> second - first }
            .filter { it in minInterval..maxInterval }
            .sorted()
        if (intervals.size < 3) return null
        val median = intervals[intervals.size / 2].toFloat()
        var bpm = 60f * frameRate / median
        while (bpm < 65f) bpm *= 2f
        while (bpm > 190f) bpm /= 2f
        return bpm
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
        private const val MAX_ANALYSIS_SECONDS = 360L
        private const val LIGHT_ANALYSIS_SECONDS = 45L
        private const val MAX_ANALYSIS_BYTES = 16L * 1024 * 1024

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
