package org.spotube.spotube.core.automix

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
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
        val drums: FloatArray,
        val bass: FloatArray,
        val other: FloatArray,
        val vocals: FloatArray
    )

    private val modelFile = File(context.filesDir, "automix/stems/models/htdemucs.onnx")
    private var environment: OrtEnvironment? = null
    private var session: OrtSession? = null

    fun modelReady(): Boolean = modelFile.exists() && modelFile.length() > MIN_MODEL_BYTES

    @Synchronized
    fun closeSession() {
        runCatching { session?.close() }
        session = null
    }

    suspend fun ensureModel(onProgress: (Float) -> Unit = {}): File? = withContext(Dispatchers.IO) {
        if (modelReady()) return@withContext modelFile
        val parent = modelFile.parentFile
        if (parent != null && !parent.exists()) parent.mkdirs()
        val temp = File(modelFile.parentFile, "htdemucs.onnx.download")
        if (temp.exists()) temp.delete()
        val client = httpClientFactory.create {
            install(HttpTimeout) {
                requestTimeoutMillis = 30 * 60 * 1000L
                connectTimeoutMillis = 15_000L
                socketTimeoutMillis = 120_000L
            }
        }
        try {
            val completed = withTimeoutOrNull(30 * 60 * 1000L) {
                client.prepareGet(MODEL_URL) {
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
                            if (total - lastReported >= 8 * 1024 * 1024L) {
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
                    Log.d("AutoMix", "stem model downloaded bytes=$total")
                    expected == null || total >= expected * 0.98
                }
            } ?: false
            if (!completed) {
                temp.delete()
                return@withContext null
            }
            if (temp.length() <= MIN_MODEL_BYTES) {
                temp.delete()
                return@withContext null
            }
            if (modelFile.exists()) modelFile.delete()
            if (!temp.renameTo(modelFile)) {
                temp.copyTo(modelFile, overwrite = true)
                temp.delete()
            }
            onProgress(1f)
            Log.d("AutoMix", "stem model ready size=${modelFile.length()}")
            modelFile
        } catch (error: Exception) {
            temp.delete()
            Log.w("AutoMix", "stem model download failed: ${error.message}")
            null
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
            if (available < SAMPLE_RATE * 2) return@withContext null
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
        val onnx = ensureSession() ?: return null
        Log.d("AutoMix", "stem inference start frames=$frames")
        val overlap = SEGMENT_SAMPLES / 4
        val stride = SEGMENT_SAMPLES - overlap
        val drums = FloatArray(frames * 2)
        val bass = FloatArray(frames * 2)
        val other = FloatArray(frames * 2)
        val vocals = FloatArray(frames * 2)
        val weights = FloatArray(frames)
        val chunkStarts = buildList {
            var start = 0
            while (start < frames) {
                add(start)
                if (start + SEGMENT_SAMPLES >= frames) break
                start += stride
            }
        }
        val output = FloatArray(4 * 2 * SEGMENT_SAMPLES)
        chunkStarts.forEachIndexed { chunkIndex, chunkStart ->
            val mix = FloatArray(2 * SEGMENT_SAMPLES)
            val copyFrames = min(SEGMENT_SAMPLES, frames - chunkStart)
            for (frame in 0 until copyFrames) {
                val source = (startFrame + chunkStart + frame) * 2
                mix[frame] = pcm[source]
                mix[SEGMENT_SAMPLES + frame] = pcm[source + 1]
            }
            if (!runChunk(onnx, mix, output)) return null
            val isFirst = chunkIndex == 0
            val isLast = chunkIndex == chunkStarts.lastIndex
            for (frame in 0 until min(SEGMENT_SAMPLES, frames - chunkStart)) {
                val head = if (isFirst) 1f else frame.toFloat() / overlap.toFloat()
                val tail = if (isLast) 1f else (SEGMENT_SAMPLES - 1 - frame).toFloat() / overlap.toFloat()
                val w = minOf(1f, head, tail).coerceIn(0f, 1f)
                val target = (chunkStart + frame) * 2
                drums[target] += output[0 * 2 * SEGMENT_SAMPLES + frame] * w
                drums[target + 1] += output[0 * 2 * SEGMENT_SAMPLES + SEGMENT_SAMPLES + frame] * w
                bass[target] += output[1 * 2 * SEGMENT_SAMPLES + frame] * w
                bass[target + 1] += output[1 * 2 * SEGMENT_SAMPLES + SEGMENT_SAMPLES + frame] * w
                other[target] += output[2 * 2 * SEGMENT_SAMPLES + frame] * w
                other[target + 1] += output[2 * 2 * SEGMENT_SAMPLES + SEGMENT_SAMPLES + frame] * w
                vocals[target] += output[3 * 2 * SEGMENT_SAMPLES + frame] * w
                vocals[target + 1] += output[3 * 2 * SEGMENT_SAMPLES + SEGMENT_SAMPLES + frame] * w
                weights[chunkStart + frame] += w
            }
            onProgress((chunkIndex + 1).toFloat() / chunkStarts.size.toFloat())
            Log.d("AutoMix", "stem chunk ${chunkIndex + 1}/${chunkStarts.size}")
        }
        for (frame in 0 until frames) {
            val norm = weights[frame].coerceAtLeast(1e-5f)
            val index = frame * 2
            drums[index] /= norm
            drums[index + 1] /= norm
            bass[index] /= norm
            bass[index + 1] /= norm
            other[index] /= norm
            other[index + 1] /= norm
            vocals[index] /= norm
            vocals[index + 1] /= norm
        }
        return StemWindow(frames, drums, bass, other, vocals)
    }

    private fun runChunk(session: OrtSession, mix: FloatArray, output: FloatArray): Boolean {
        val env = environment ?: return false
        return try {
            val input = OnnxTensor.createTensor(
                env,
                FloatBuffer.wrap(mix),
                longArrayOf(1L, 2L, SEGMENT_SAMPLES.toLong())
            )
            input.use { tensor ->
                session.run(mapOf(session.inputNames.first() to tensor)).use { result ->
                    val out = result.get(0) as OnnxTensor
                    val buffer = out.floatBuffer
                    if (buffer == null) return false
                    buffer.get(output)
                }
            }
            true
        } catch (error: Exception) {
            Log.w("AutoMix", "stem inference failed: ${error.message}")
            false
        }
    }

    @Synchronized
    private fun ensureSession(): OrtSession? {
        session?.let { return it }
        if (!modelReady()) return null
        return try {
            val env = OrtEnvironment.getEnvironment()
            val options = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(4)
                setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
                runCatching { setMemoryPatternOptimization(false) }
                runCatching { setCPUArenaAllocator(false) }
            }
            val created = env.createSession(modelFile.absolutePath, options)
            environment = env
            session = created
            Log.d("AutoMix", "stem session ready inputs=${created.inputNames}")
            created
        } catch (error: Exception) {
            Log.w("AutoMix", "stem session failed: ${error.message}")
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
            source.seekTo(windowStartUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
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
            var targetFrames = requestedFrames * sampleRate / SAMPLE_RATE + sampleRate
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
                        targetFrames = requestedFrames * sampleRate / SAMPLE_RATE + sampleRate
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
        const val SEGMENT_SAMPLES = 343_980
        private const val DECODE_TIMEOUT_MS = 30_000L
        private const val MAX_AUDIO_BYTES = 40L * 1024 * 1024
        private const val MIN_MODEL_BYTES = 300_000_000L
        private const val MODEL_URL =
            "https://huggingface.co/smank/htdemucs-onnx/resolve/main/htdemucs.onnx?download=true"
    }
}
