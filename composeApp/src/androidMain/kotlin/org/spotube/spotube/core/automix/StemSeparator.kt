package org.spotube.spotube.core.automix

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Process
import android.util.Log
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.spotube.spotube.core.network.HttpClientFactory
import org.spotube.spotube.core.server.CacheManager

class StemSeparator(
    private val context: Context,
    private val httpClientFactory: HttpClientFactory,
    private val cacheManager: CacheManager
) {
    data class StemWindow(
        val frames: Int,
        val vocals: FloatArray,
        val instrumental: FloatArray
    )

    private val modelsDir = File(context.filesDir, "automix/stems/models")
    private val vocalsModel = File(modelsDir, "spleeter_vocals.onnx")
    private val accompanimentModel = File(modelsDir, "spleeter_accompaniment.onnx")
    private var environment: OrtEnvironment? = null
    private var vocalsSession: OrtSession? = null
    private var accompanimentSession: OrtSession? = null

    fun modelReady(): Boolean =
        vocalsModel.length() > MIN_MODEL_BYTES &&
            accompanimentModel.length() > MIN_MODEL_BYTES

    @Synchronized
    fun closeSession() {
        runCatching { vocalsSession?.close() }
        runCatching { accompanimentSession?.close() }
        vocalsSession = null
        accompanimentSession = null
    }

    suspend fun ensureModel(onProgress: (Float) -> Unit = {}): File? = withContext(Dispatchers.IO) {
        if (modelReady()) return@withContext vocalsModel
        val parent = modelsDir
        if (!parent.exists() && !parent.mkdirs()) return@withContext null

        if (!vocalsModel.exists() || vocalsModel.length() <= MIN_MODEL_BYTES) {
            val ok = downloadModel(
                url = VOCALS_MODEL_URL,
                destination = vocalsModel,
                onProgress = { progress -> onProgress(progress * 0.5f) }
            )
            if (!ok) return@withContext null
        }
        if (!accompanimentModel.exists() || accompanimentModel.length() <= MIN_MODEL_BYTES) {
            val ok = downloadModel(
                url = ACCOMPANIMENT_MODEL_URL,
                destination = accompanimentModel,
                onProgress = { progress -> onProgress(0.5f + progress * 0.5f) }
            )
            if (!ok) return@withContext null
        }
        onProgress(1f)
        Log.d(
            "AutoMix",
            "stem models ready vocals=${vocalsModel.length()} accompaniment=${accompanimentModel.length()}"
        )
        File(modelsDir, "htdemucs.onnx").delete()
        vocalsModel
    }

    private suspend fun downloadModel(
        url: String,
        destination: File,
        onProgress: (Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val temp = File(destination.parentFile, destination.name + ".download")
        if (temp.exists()) temp.delete()
        val client = httpClientFactory.create {
            install(HttpTimeout) {
                requestTimeoutMillis = 15 * 60 * 1000L
                connectTimeoutMillis = 15_000L
                socketTimeoutMillis = 120_000L
            }
        }
        try {
            val completed = withTimeoutOrNull(15 * 60 * 1000L) {
                client.prepareGet(url) {
                    headers.append(HttpHeaders.Range, "bytes=0-")
                    headers.append("Accept-Encoding", "identity")
                }.execute { response ->
                    if (!response.status.isSuccess()) {
                        Log.w("AutoMix", "stem model HTTP ${response.status.value}")
                        return@execute false
                    }
                    val expected = response.headers[HttpHeaders.ContentLength]?.toLongOrNull()
                    var total = 0L
                    var lastReported = 0L
                    val channel = response.bodyAsChannel()
                    FileOutputStream(temp).use { output ->
                        val buffer = ByteArray(256 * 1024)
                        while (true) {
                            val read = channel.readAvailable(buffer, 0, buffer.size)
                            if (read < 0) break
                            if (read == 0) continue
                            output.write(buffer, 0, read)
                            total += read
                            if (total - lastReported >= 4 * 1024 * 1024L) {
                                lastReported = total
                                val progress = if (expected != null && expected > 0L) {
                                    (total.toFloat() / expected.toFloat()).coerceIn(0f, 0.99f)
                                } else {
                                    0f
                                }
                                onProgress(progress)
                            }
                        }
                    }
                    Log.d("AutoMix", "stem model downloaded ${destination.name} bytes=$total")
                    expected == null || total >= expected * 0.98
                }
            } ?: false
            if (!completed || temp.length() <= MIN_MODEL_BYTES) {
                temp.delete()
                return@withContext false
            }
            if (destination.exists()) destination.delete()
            if (!temp.renameTo(destination)) {
                temp.copyTo(destination, overwrite = true)
                temp.delete()
            }
            onProgress(1f)
            true
        } catch (error: Exception) {
            temp.delete()
            Log.w("AutoMix", "stem model download failed: ${error.message}")
            false
        } finally {
            client.close()
        }
    }

    suspend fun separateWindow(
        trackId: String,
        url: String,
        startMs: Long,
        frames: Int,
        onProgress: (Float) -> Unit = {}
    ): StemWindow? = withContext(Dispatchers.IO) {
        val source = resolveAudioSource(trackId, url) ?: return@withContext null
        try {
            val pcm = withTimeoutOrNull(DECODE_TIMEOUT_MS) {
                decodeStereoWindow(source.first.absolutePath, startMs, frames)
            } ?: run {
                Log.w("AutoMix", "stem decode timeout for start=${startMs}ms frames=$frames")
                return@withContext null
            }
            val available = pcm.size / 2
            if (available < SAMPLE_RATE / 4) return@withContext null
            separatePcm(pcm, 0, min(available, frames.coerceAtLeast(1)), onProgress)
        } finally {
            if (source.second) source.first.delete()
        }
    }

    private suspend fun resolveAudioSource(trackId: String, url: String): Pair<File, Boolean>? {
        val cached = cacheManager.findCachedEntry(trackId)?.first
        if (cached != null) {
            val file = File(cached.toString())
            if (file.exists() && file.length() > 32 * 1024L) {
                Log.d("AutoMix", "stem decode source cache file=${file.name}")
                return file to false
            }
        }
        Log.d("AutoMix", "stem decode source download")
        val temp = File.createTempFile("stem-audio-", ".audio", context.cacheDir)
        val client = httpClientFactory.create {
            install(HttpTimeout) {
                requestTimeoutMillis = 90_000L
                connectTimeoutMillis = 10_000L
                socketTimeoutMillis = 30_000L
            }
        }
        val ok = try {
            withTimeoutOrNull(90_000L) {
                client.prepareGet(url) {
                    headers.append(HttpHeaders.UserAgent, "Mozilla/5.0")
                    headers.append(HttpHeaders.Accept, "*/*")
                    headers.append(HttpHeaders.Range, "bytes=0-")
                    headers.append("Accept-Encoding", "identity")
                }.execute { response ->
                    if (!response.status.isSuccess()) {
                        Log.w("AutoMix", "stem audio HTTP ${response.status.value}")
                        return@execute false
                    }
                    var total = 0L
                    val channel = response.bodyAsChannel()
                    FileOutputStream(temp).use { output ->
                        val buffer = ByteArray(128 * 1024)
                        while (total < MAX_AUDIO_BYTES) {
                            val read = channel.readAvailable(buffer, 0, buffer.size)
                            if (read < 0) break
                            if (read > 0) {
                                output.write(buffer, 0, read)
                                total += read
                            }
                        }
                    }
                    Log.d("AutoMix", "stem audio downloaded bytes=$total")
                    total > 32 * 1024L
                }
            } ?: false
        } catch (error: Exception) {
            Log.w("AutoMix", "stem audio download failed: ${error.message}")
            false
        } finally {
            client.close()
        }
        if (!ok) {
            temp.delete()
            return null
        }
        return temp to true
    }

    private suspend fun separatePcm(
        pcm: FloatArray,
        startFrame: Int,
        frames: Int,
        onProgress: (Float) -> Unit
    ): StemWindow? {
        val sessions = ensureSessions() ?: return null
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
        onProgress(0.05f)
        val left = FloatArray(frames)
        val right = FloatArray(frames)
        for (frame in 0 until frames) {
            val source = (startFrame + frame) * 2
            left[frame] = pcm.getOrElse(source) { 0f }
            right[frame] = pcm.getOrElse(source + 1) { 0f }
        }
        onProgress(0.12f)
        val leftSpectrum = SpleeterStft.stft(left)
        val rightSpectrum = SpleeterStft.stft(right)
        val splitCount = splitCount(leftSpectrum.frames)
        val input = FloatArray(2 * splitCount * SpleeterStft.FRAMES_PER_SPLIT * SpleeterStft.MODEL_BINS)
        fillModelInput(input, leftSpectrum, 0, splitCount)
        fillModelInput(input, rightSpectrum, 1, splitCount)
        Log.d(
            "AutoMix",
            "stem spleeter stft frames=${leftSpectrum.frames} splits=$splitCount"
        )
        onProgress(0.20f)
        val vocalsOutput = runModel(sessions.first, input, splitCount) ?: return null
        onProgress(0.55f)
        val accompanimentOutput = runModel(sessions.second, input, splitCount) ?: return null
        onProgress(0.85f)
        val vocals = FloatArray(frames * 2)
        val instrumental = FloatArray(frames * 2)
        for (channel in 0..1) {
            val spectrum = if (channel == 0) leftSpectrum else rightSpectrum
            val channelVocals = reconstruct(
                spectrum = spectrum,
                vocalsOutput = vocalsOutput,
                accompanimentOutput = accompanimentOutput,
                splitCount = splitCount,
                channel = channel,
                outputFrames = frames,
                vocals = true
            )
            val channelInstrumental = reconstruct(
                spectrum = spectrum,
                vocalsOutput = vocalsOutput,
                accompanimentOutput = accompanimentOutput,
                splitCount = splitCount,
                channel = channel,
                outputFrames = frames,
                vocals = false
            )
            for (frame in 0 until frames) {
                val target = frame * 2 + channel
                vocals[target] = channelVocals.getOrElse(frame) { 0f }
                instrumental[target] = channelInstrumental.getOrElse(frame) { 0f }
            }
        }
        onProgress(1f)
        Log.d("AutoMix", "stem spleeter done frames=$frames")
        return StemWindow(frames, vocals, instrumental)
    }

    private fun splitCount(spectrumFrames: Int): Int {
        var splits = (spectrumFrames + SpleeterStft.FRAMES_PER_SPLIT - 1) /
            SpleeterStft.FRAMES_PER_SPLIT
        if (spectrumFrames % SpleeterStft.FRAMES_PER_SPLIT == 0) splits += 1
        return splits.coerceAtLeast(1)
    }

    private fun fillModelInput(
        input: FloatArray,
        spectrum: SpleeterStft.Spectrum,
        channel: Int,
        splitCount: Int
    ) {
        val splitSize = SpleeterStft.FRAMES_PER_SPLIT
        val bins = SpleeterStft.MODEL_BINS
        for (split in 0 until splitCount) {
            for (frameInSplit in 0 until splitSize) {
                val frame = split * splitSize + frameInSplit
                if (frame >= spectrum.frames) continue
                val sourceBase = ((channel * splitCount + split) * splitSize + frameInSplit) * bins
                for (bin in 0 until bins) {
                    val real = spectrum.real[frame][bin]
                    val imag = spectrum.imag[frame][bin]
                    input[sourceBase + bin] = sqrt(real * real + imag * imag)
                }
            }
        }
    }

    private fun runModel(
        session: OrtSession,
        input: FloatArray,
        splitCount: Int
    ): FloatArray? {
        val env = environment ?: return null
        return try {
            val tensor = OnnxTensor.createTensor(
                env,
                FloatBuffer.wrap(input),
                longArrayOf(
                    2L,
                    splitCount.toLong(),
                    SpleeterStft.FRAMES_PER_SPLIT.toLong(),
                    SpleeterStft.MODEL_BINS.toLong()
                )
            )
            tensor.use { value ->
                session.run(mapOf(session.inputNames.first() to value)).use { result ->
                    val output = result.get(0) as OnnxTensor
                    val buffer = output.floatBuffer ?: return null
                    val values = FloatArray(buffer.remaining())
                    buffer.get(values)
                    values
                }
            }
        } catch (error: Exception) {
            Log.w("AutoMix", "stem spleeter inference failed: ${error.message}")
            null
        }
    }

    private fun reconstruct(
        spectrum: SpleeterStft.Spectrum,
        vocalsOutput: FloatArray,
        accompanimentOutput: FloatArray,
        splitCount: Int,
        channel: Int,
        outputFrames: Int,
        vocals: Boolean
    ): FloatArray {
        val splitSize = SpleeterStft.FRAMES_PER_SPLIT
        val bins = SpleeterStft.MODEL_BINS
        return SpleeterStft.istft(spectrum, outputFrames) { frame, bin ->
            if (bin >= bins) {
                0f
            } else {
                val split = frame / splitSize
                val frameInSplit = frame % splitSize
                val sourceBase = ((channel * splitCount + split) * splitSize + frameInSplit) * bins
                val vocal = vocalsOutput.getOrElse(sourceBase + bin) { 0f }
                val accompaniment = accompanimentOutput.getOrElse(sourceBase + bin) { 0f }
                val denominator = vocal * vocal + accompaniment * accompaniment + 1e-10f
                if (vocals) {
                    (vocal * vocal + 5e-11f) / denominator
                } else {
                    (accompaniment * accompaniment + 5e-11f) / denominator
                }
            }
        }
    }

    @Synchronized
    private fun ensureSessions(): Pair<OrtSession, OrtSession>? {
        val existingVocals = vocalsSession
        val existingAccompaniment = accompanimentSession
        if (existingVocals != null && existingAccompaniment != null) {
            return existingVocals to existingAccompaniment
        }
        if (!modelReady()) return null
        return try {
            val env = OrtEnvironment.getEnvironment()
            val options = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(1)
                setInterOpNumThreads(1)
                setExecutionMode(OrtSession.SessionOptions.ExecutionMode.SEQUENTIAL)
                setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
                runCatching { setMemoryPatternOptimization(false) }
                runCatching { setCPUArenaAllocator(false) }
            }
            val createdVocals = env.createSession(vocalsModel.absolutePath, options)
            val createdAccompaniment = env.createSession(accompanimentModel.absolutePath, options)
            environment = env
            vocalsSession = createdVocals
            accompanimentSession = createdAccompaniment
            Log.d(
                "AutoMix",
                "stem spleeter sessions ready vocals=${createdVocals.inputNames} " +
                    "accompaniment=${createdAccompaniment.inputNames}"
            )
            createdVocals to createdAccompaniment
        } catch (error: Exception) {
            Log.w("AutoMix", "stem spleeter session failed: ${error.message}")
            closeSession()
            null
        }
    }

    private suspend fun decodeStereoWindow(
        url: String,
        startMs: Long,
        requestedFrames: Int
    ): FloatArray? {
        var extractor: MediaExtractor? = null
        var decoder: MediaCodec? = null
        val windowStartUs = startMs * 1000L
        return try {
            Log.d("AutoMix", "stem decode start startMs=$startMs frames=$requestedFrames")
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
            val codec = MediaCodec.createDecoderByType(mime)
            decoder = codec
            codec.configure(sourceFormat, null, null, 0)
            codec.start()
            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            var pcmFloat = false
            val chunks = ArrayList<FloatArray>(64)
            var collectedFrames = 0
            var targetFrames = (requestedFrames.toLong() * sampleRate / SAMPLE_RATE + sampleRate).toInt()
            while (!outputDone && collectedFrames < targetFrames) {
                currentCoroutineContext()[Job]?.ensureActive()
                if (!inputDone) {
                    val inputIndex = codec.dequeueInputBuffer(10_000)
                    if (inputIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputIndex) ?: continue
                        val sampleSize = source.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(
                                inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(
                                inputIndex, 0, sampleSize, source.sampleTime, 0
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
                        targetFrames = (requestedFrames.toLong() * sampleRate / SAMPLE_RATE + sampleRate).toInt()
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
                                val audio = if (pcmFloat) {
                                    val f = buffer.order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
                                    FloatArray(f.remaining()) { f.get() }
                                } else {
                                    val s = buffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                                    FloatArray(s.remaining()) { s.get() / 32768f }
                                }
                                val bufferFrames = audio.size / channelCount
                                var skipFrames = 0
                                if (info.presentationTimeUs >= 0L) {
                                    val bufferStartUs = info.presentationTimeUs
                                    val bufferEndUs = bufferStartUs +
                                        bufferFrames * 1_000_000L / sampleRate
                                    skipFrames = when {
                                        bufferEndUs <= windowStartUs -> bufferFrames
                                        bufferStartUs < windowStartUs ->
                                            ((windowStartUs - bufferStartUs) * sampleRate /
                                                1_000_000L).toInt()
                                        else -> 0
                                    }.coerceIn(0, bufferFrames)
                                }
                                val frames = bufferFrames - skipFrames
                                if (frames > 0) {
                                    val take = min(frames, targetFrames - collectedFrames)
                                    if (take > 0) {
                                        val stereo = FloatArray(take * 2)
                                        for (frame in 0 until take) {
                                            val base = (skipFrames + frame) * channelCount
                                            if (base + channelCount <= audio.size) {
                                                val left = audio[base]
                                                val right = if (channelCount > 1) audio[base + 1] else left
                                                stereo[frame * 2] = left
                                                stereo[frame * 2 + 1] = right
                                            }
                                        }
                                        chunks += stereo
                                        collectedFrames += take
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
            if (collectedFrames < SAMPLE_RATE / 2) return null
            val merged = FloatArray(collectedFrames * 2)
            var offset = 0
            for (chunk in chunks) {
                val copy = min(chunk.size, merged.size - offset)
                chunk.copyInto(merged, offset, 0, copy)
                offset += copy
            }
            val resampled = resampleTo44k(merged, sampleRate)
            val result = if (resampled.size / 2 > requestedFrames) {
                resampled.copyOf(requestedFrames * 2)
            } else {
                resampled
            }
            Log.d("AutoMix", "stem decode done frames=${result.size / 2} sourceRate=$sampleRate")
            result
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (error: Exception) {
            Log.w("AutoMix", "stem decode failed: ${error.message}")
            null
        } finally {
            runCatching { decoder?.stop() }
            runCatching { decoder?.release() }
            runCatching { extractor?.release() }
        }
    }

    private fun resampleTo44k(input: FloatArray, inputRate: Int): FloatArray {
        if (inputRate == SAMPLE_RATE) return input
        val inputFrames = input.size / 2
        val outputFrames = (inputFrames.toLong() * SAMPLE_RATE / inputRate)
            .toInt()
            .coerceAtLeast(1)
        val output = FloatArray(outputFrames * 2)
        for (frame in 0 until outputFrames) {
            val position = frame.toDouble() * inputRate / SAMPLE_RATE
            val left = position.toInt().coerceIn(0, inputFrames - 1)
            val right = (left + 1).coerceAtMost(inputFrames - 1)
            val fraction = (position - left).toFloat()
            for (channel in 0..1) {
                val a = input[left * 2 + channel]
                val b = input[right * 2 + channel]
                output[frame * 2 + channel] = a + (b - a) * fraction
            }
        }
        return output
    }

    companion object {
        const val SAMPLE_RATE = 44_100
        private const val DECODE_TIMEOUT_MS = 60_000L
        private const val MAX_AUDIO_BYTES = 40L * 1024 * 1024
        private const val MIN_MODEL_BYTES = 30_000_000L
        private const val VOCALS_MODEL_URL =
            "https://huggingface.co/csukuangfj/sherpa-onnx-spleeter-2stems/resolve/main/vocals.onnx"
        private const val ACCOMPANIMENT_MODEL_URL =
            "https://huggingface.co/csukuangfj/sherpa-onnx-spleeter-2stems/resolve/main/accompaniment.onnx"
    }
}
