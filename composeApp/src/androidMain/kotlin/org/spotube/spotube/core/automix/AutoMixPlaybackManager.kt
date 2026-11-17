package org.spotube.spotube.core.automix

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.audiofx.Equalizer
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import org.spotube.spotube.core.server.StreamTrackRegistry
import org.spotube.spotube.media.AutoMixForegroundService
import org.spotube.spotube.modules.plugin.PluginProvider
import org.spotube.spotube.modules.settings.SettingsProvider
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

class AutoMixPlaybackManager(
    private val context: Context,
    private val settingsProvider: SettingsProvider,
    private val pluginProvider: PluginProvider,
    private val analyzer: AudioFeatureAnalyzer
) {
    enum class TransitionStyle { Cut, Blend, Mashup }

    data class State(
        val isActive: Boolean = false,
        val title: String = "",
        val current: MetadataTrack? = null,
        val next: MetadataTrack? = null,
        val isPlaying: Boolean = false,
        val isTransitioning: Boolean = false,
        val transitionProgress: Float = 0f,
        val transitionStyle: TransitionStyle = TransitionStyle.Blend,
        val positionMs: Long = 0L,
        val durationMs: Long = 0L,
        val currentBpm: Float? = null,
        val currentKey: String = "",
        val nextBpm: Float? = null,
        val nextKey: String = "",
        val isAnalyzing: Boolean = false
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .build()
    private val deckA = ExoPlayer.Builder(context)
        .setAudioAttributes(audioAttributes, false)
        .build()
    private val deckB = ExoPlayer.Builder(context)
        .setAudioAttributes(audioAttributes, false)
        .build()

    private var eqA: Equalizer? = null
    private var eqB: Equalizer? = null
    private var sessionA: Int = 0
    private var sessionB: Int = 0
    private var focusRequest: AudioFocusRequest? = null

    private val queue = mutableListOf<MetadataTrack>()
    private var index: Int = 0
    private var current: MetadataTrack? = null
    private var next: MetadataTrack? = null
    private var activeIsA: Boolean = true
    private var isStarted: Boolean = false
    private var isPlaying: Boolean = false
    private var isTransitioning: Boolean = false
    private var transitionProgress: Float = 0f
    private var transitionStyle: TransitionStyle = TransitionStyle.Blend
    private var positionMs: Long = 0L
    private var durationMs: Long = 0L
    private var sessionKey: String = ""
    private var title: String = ""
    private var pollJob: Job? = null
    private var transitionJob: Job? = null
    private var analysisJob: Job? = null
    private val features = mutableMapOf<String, AudioFeatures>()
    private var activeSpeed: Float = 1f
    private var lastStartTrackId: String? = null
    private val localAnalysisEnabled = false

    init {
        deckA.addAnalyticsListener(object : AnalyticsListener {
            override fun onAudioSessionIdChanged(
                eventTime: AnalyticsListener.EventTime,
                audioSessionId: Int
            ) {
                sessionA = audioSessionId
                attachEqualizer(audioSessionId, isA = true)
            }
        })
        deckB.addAnalyticsListener(object : AnalyticsListener {
            override fun onAudioSessionIdChanged(
                eventTime: AnalyticsListener.EventTime,
                audioSessionId: Int
            ) {
                sessionB = audioSessionId
                attachEqualizer(audioSessionId, isA = false)
            }
        })
        deckA.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                if (activeIsA) skipNext()
            }
        })
        deckB.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                if (!activeIsA) skipNext()
            }
        })
    }

    fun start(requestedTitle: String, tracks: List<MetadataTrack>) {
        if (tracks.isEmpty()) return
        val key = requestedTitle + "|" + tracks.joinToString(",") { it.id }
        if (isStarted && key == sessionKey) {
            AutoMixForegroundService.start(context)
            requestFocus()
            if (!isPlaying) {
                activePlayer().play()
                isPlaying = true
            }
            publish()
            return
        }
        if (isStarted) {
            stop()
        }
        title = requestedTitle
        sessionKey = key
        queue.clear()
        features.clear()
        activeSpeed = 1f
        val ordered = smartOrder(tracks)
        val startIndex = chooseStartIndex(ordered)
        queue.addAll(ordered.drop(startIndex) + ordered.take(startIndex))
        StreamTrackRegistry.registerAll(queue)
        lastStartTrackId = queue.firstOrNull()?.id
        index = 0
        activeIsA = true
        isStarted = true
        isPlaying = true
        isTransitioning = false
        transitionProgress = 0f
        current = queue.firstOrNull()
        next = queue.getOrNull(1)
        AutoMixForegroundService.start(context)
        requestFocus()
        current?.let { loadTrack(deckA, it, 1f) }
        next?.let { loadTrack(deckB, it, 0f) }
        deckA.playWhenReady = true
        startPolling()
        ensureAnalysis()
        publish()
    }

    fun stop() {
        isStarted = false
        isPlaying = false
        isTransitioning = false
        pollJob?.cancel()
        transitionJob?.cancel()
        analysisJob?.cancel()
        pollJob = null
        transitionJob = null
        analysisJob = null
        deckA.pause()
        deckB.pause()
        deckA.playbackParameters = PlaybackParameters(1f)
        deckB.playbackParameters = PlaybackParameters(1f)
        deckA.clearMediaItems()
        deckB.clearMediaItems()
        current = null
        next = null
        StreamTrackRegistry.unregisterAll(queue.map { it.id })
        queue.clear()
        features.clear()
        activeSpeed = 1f
        abandonFocus()
        AutoMixForegroundService.stop(context)
        publish()
    }

    fun togglePlayPause() {
        if (!isStarted || isTransitioning) return
        if (isPlaying) {
            deckA.pause()
            deckB.pause()
            isPlaying = false
        } else {
            activePlayer().play()
            isPlaying = true
        }
        publish()
    }

    fun skipNext() {
        if (!isStarted || isTransitioning) return
        val target = queue.getOrNull(index + 1)
        if (target == null) {
            preloadNext()
            return
        }
        activePlayer().pause()
        activePlayer().playbackParameters = PlaybackParameters(1f)
        activeSpeed = 1f
        index += 1
        current = target
        positionMs = 0L
        durationMs = 0L
        loadTrack(activePlayer(), target, 1f)
        activePlayer().play()
        isPlaying = true
        next = queue.getOrNull(index + 1)
        preloadNext()
        ensureAnalysis()
        publish()
    }

    fun skipPrevious() {
        if (!isStarted || isTransitioning) return
        val previousIndex = (index - 1).coerceAtLeast(0)
        val target = queue.getOrNull(previousIndex) ?: return
        activePlayer().pause()
        activePlayer().playbackParameters = PlaybackParameters(1f)
        activeSpeed = 1f
        index = previousIndex
        current = target
        positionMs = 0L
        durationMs = 0L
        loadTrack(activePlayer(), target, 1f)
        activePlayer().play()
        isPlaying = true
        next = queue.getOrNull(index + 1)
        preloadNext()
        ensureAnalysis()
        publish()
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = scope.launch {
            while (isStarted) {
                val player = activePlayer()
                positionMs = player.currentPosition.coerceAtLeast(0L)
                durationMs = player.duration.coerceAtLeast(0L)
                val plan = planTransition(current, next)
                val triggerAt = transitionTriggerAt(plan, durationMs)
                if (
                    isPlaying &&
                    !isTransitioning &&
                    next != null &&
                    durationMs > 0L &&
                    positionMs >= triggerAt
                ) {
                    startTransition()
                }
                publish()
                delay(100)
            }
        }
    }

    private fun startTransition() {
        if (isTransitioning) return
        val target = next ?: return
        val plan = planTransition(current, target)
        val from = activePlayer()
        val to = idlePlayer()
        isTransitioning = true
        transitionStyle = plan.style
        transitionProgress = 0f
        val targetDuration = to.duration.takeIf { it > 0L } ?: target.durationMs
        val startAt = alignedStartMs(target, plan, targetDuration)
        to.playbackParameters = PlaybackParameters(plan.speed)
        to.seekTo(startAt)
        to.volume = 0f
        to.play()
        transitionJob = scope.launch {
            val steps = (plan.durationMs / 100L).toInt().coerceAtLeast(1)
            for (step in 1..steps) {
                val t = step.toFloat() / steps.toFloat()
                transitionProgress = t
                from.volume = sqrt(1f - t)
                to.volume = sqrt(t)
                if (step % 10 == 0 || step == steps) {
                    val fromDb = -12f * t
                    val toDb = -12f * (1f - t)
                    if (activeIsA) {
                        applyBass(eqA, fromDb)
                        applyBass(eqB, toDb)
                    } else {
                        applyBass(eqB, fromDb)
                        applyBass(eqA, toDb)
                    }
                }
                publish()
                delay(100)
            }
            from.pause()
            from.volume = 1f
            to.volume = 1f
            if (activeIsA) {
                applyBass(eqA, 0f)
                applyBass(eqB, 0f)
            } else {
                applyBass(eqB, 0f)
                applyBass(eqA, 0f)
            }
            activeIsA = !activeIsA
            activeSpeed = plan.speed
            index += 1
            current = target
            positionMs = 0L
            durationMs = 0L
            isTransitioning = false
            transitionProgress = 0f
            preloadNext()
            ensureAnalysis()
            publish()
        }
    }

    private fun preloadNext() {
        val target = queue.getOrNull(index + 1)
        if (target == null) {
            scope.launch {
                val additions = fetchRecommendations(queue.takeLast(5), queue.map { it.id }.toSet())
                if (!isStarted) return@launch
                queue.addAll(if (additions.isNotEmpty()) additions else queue)
                StreamTrackRegistry.registerAll(additions)
                next = queue.getOrNull(index + 1)
                next?.let { loadTrack(idlePlayer(), it, 0f) }
                ensureAnalysis()
                publish()
            }
            return
        }
        next = target
        loadTrack(idlePlayer(), target, 0f)
        publish()
    }

    private fun loadTrack(player: ExoPlayer, track: MetadataTrack, volume: Float) {
        val port = settingsProvider.settingsState.value?.playbackProxyServerPort ?: 14769
        player.playbackParameters = PlaybackParameters(1f)
        player.setMediaItem(track.toMedia3Item(port))
        player.prepare()
        player.volume = volume
    }

    private fun activePlayer() = if (activeIsA) deckA else deckB
    private fun idlePlayer() = if (activeIsA) deckB else deckA

    private suspend fun fetchRecommendations(
        seeds: List<MetadataTrack>,
        existingIds: Set<String>
    ): List<MetadataTrack> {
        if (seeds.isEmpty()) return emptyList()
        val plugin = pluginProvider.selectedMetadataPlugin.value ?: return emptyList()
        return try {
            plugin.use {
                metadataTrackAPI.recommendationsBasedOnTracks(
                    seedTrackIds = seeds.map { it.id },
                    limit = 20
                )
            }.filter { it.id !in existingIds }.take(15)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun requestFocus() {
        if (focusRequest != null) return
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setOnAudioFocusChangeListener { change ->
                if (
                    change == AudioManager.AUDIOFOCUS_LOSS ||
                    change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
                ) {
                    deckA.pause()
                    deckB.pause()
                    isPlaying = false
                    publish()
                }
            }
            .build()
        runCatching { audioManager.requestAudioFocus(request) }
        focusRequest = request
    }

    private fun abandonFocus() {
        focusRequest?.let { request ->
            runCatching { audioManager.abandonAudioFocusRequest(request) }
        }
        focusRequest = null
    }

    private fun attachEqualizer(sessionId: Int, isA: Boolean) {
        if (sessionId == 0) return
        if (isA && eqA != null) return
        if (!isA && eqB != null) return
        val equalizer = runCatching {
            Equalizer(0, sessionId).apply { enabled = true }
        }.getOrNull()
        if (isA) eqA = equalizer else eqB = equalizer
    }

    private fun applyBass(equalizer: Equalizer?, db: Float) {
        if (equalizer == null) return
        runCatching {
            val range = equalizer.bandLevelRange
            val level = (db * 100f).toInt().coerceIn(range[0].toInt(), range[1].toInt())
            equalizer.setBandLevel(bassBand(equalizer), level.toShort())
        }
    }

    private fun publish() {
        val currentFeatures = current?.let { features[it.id] }
        val nextFeatures = next?.let { features[it.id] }
        _state.value = State(
            isActive = isStarted,
            title = title,
            current = current,
            next = next,
            isPlaying = isPlaying,
            isTransitioning = isTransitioning,
            transitionProgress = transitionProgress,
            transitionStyle = transitionStyle,
            positionMs = positionMs,
            durationMs = durationMs,
            currentBpm = currentFeatures?.bpm?.times(activeSpeed),
            currentKey = currentFeatures?.key.orEmpty(),
            nextBpm = nextFeatures?.bpm,
            nextKey = nextFeatures?.key.orEmpty(),
            isAnalyzing = analysisJob?.isActive == true
        )
    }

    private fun chooseStartIndex(tracks: List<MetadataTrack>): Int {
        if (tracks.size <= 1) return 0
        var candidate = Random.nextInt(tracks.size)
        if (tracks[candidate].id == lastStartTrackId) {
            candidate = (candidate + 1 + Random.nextInt(tracks.size - 1)) % tracks.size
        }
        return candidate
    }

    private fun smartOrder(tracks: List<MetadataTrack>): List<MetadataTrack> {
        if (tracks.size <= 2) return tracks
        val remaining = tracks.toMutableList()
        val ordered = mutableListOf(remaining.removeAt(0))
        while (remaining.isNotEmpty()) {
            val lastArtist = ordered.last().artists.firstOrNull()?.id.orEmpty()
            val differentArtistIndex = remaining.indexOfFirst { track ->
                track.artists.none { it.id == lastArtist }
            }
            ordered += remaining.removeAt(
                if (differentArtistIndex >= 0) differentArtistIndex else 0
            )
        }
        return ordered
    }

    private data class TransitionPlan(
        val style: TransitionStyle,
        val durationMs: Long,
        val mixOutFraction: Float,
        val mixInFraction: Float,
        val speed: Float = 1f,
        val beatMatched: Boolean = false
    )

    private fun planTransition(
        current: MetadataTrack?,
        next: MetadataTrack?
    ): TransitionPlan {
        val hash = (((current?.id ?: "") + (next?.id ?: "")).hashCode() and 0x7FFFFFFF)
        val variation = (hash / 10) % 4
        val currentFeatures = current?.let { features[it.id] }
        val nextFeatures = next?.let { features[it.id] }
        val bothLong = (current?.durationMs ?: 0L) > 60_000L &&
            (next?.durationMs ?: 0L) > 60_000L

        if (
            currentFeatures != null &&
            nextFeatures != null &&
            currentFeatures.bpm > 0f &&
            nextFeatures.bpm > 0f
        ) {
            val effectiveBpm = currentFeatures.bpm * activeSpeed
            val ratio = effectiveBpm / nextFeatures.bpm
            val beatMatched = ratio in 0.92f..1.08f
            val speed = if (beatMatched) ratio.coerceIn(0.92f, 1.08f) else 1f
            val keyDistance = keyDistance(currentFeatures.key, nextFeatures.key)
            val energyDistance = abs(currentFeatures.energy - nextFeatures.energy)
            val compatibleMashup = bothLong &&
                beatMatched &&
                keyDistance <= 1 &&
                energyDistance < 0.5f

            if (compatibleMashup && hash % 10 < 8) {
                val beats = if (hash % 2 == 0) 32 else 48
                val duration = (beats * 60_000f / effectiveBpm)
                    .coerceIn(12_000f, 24_000f)
                return TransitionPlan(
                    style = TransitionStyle.Mashup,
                    durationMs = duration.toLong(),
                    mixOutFraction = currentFeatures.mixOutFraction,
                    mixInFraction = nextFeatures.mixInFraction,
                    speed = speed,
                    beatMatched = true
                )
            }
            if (beatMatched && hash % 3 == 0) {
                val duration = (16 * 60_000f / effectiveBpm)
                    .coerceIn(6_000f, 12_000f)
                return TransitionPlan(
                    style = TransitionStyle.Blend,
                    durationMs = duration.toLong(),
                    mixOutFraction = currentFeatures.mixOutFraction,
                    mixInFraction = nextFeatures.mixInFraction,
                    speed = speed,
                    beatMatched = true
                )
            }
            return TransitionPlan(
                style = TransitionStyle.Cut,
                durationMs = 1_200L + variation * 200L,
                mixOutFraction = currentFeatures.mixOutFraction,
                mixInFraction = nextFeatures.mixInFraction,
                speed = if (beatMatched) speed else 1f,
                beatMatched = beatMatched
            )
        }

        if (bothLong && hash % 10 < 6) {
            return TransitionPlan(
                style = TransitionStyle.Mashup,
                durationMs = 16_000L + variation * 2_000L,
                mixOutFraction = 0.60f + variation * 0.03f,
                mixInFraction = 0.32f + variation * 0.04f
            )
        }
        if (hash % 2 == 0) {
            return TransitionPlan(
                style = TransitionStyle.Cut,
                durationMs = 1_200L + variation * 200L,
                mixOutFraction = 0.78f + variation * 0.02f,
                mixInFraction = 0.22f + variation * 0.03f
            )
        }
        return TransitionPlan(
            style = TransitionStyle.Blend,
            durationMs = 6_000L + variation * 1_000L,
            mixOutFraction = 0.82f + variation * 0.02f,
            mixInFraction = 0.16f + variation * 0.03f
        )
    }

    private fun transitionTriggerAt(plan: TransitionPlan, durationMs: Long): Long {
        if (durationMs <= 20_000L) {
            return (durationMs - 6_000L).coerceAtLeast(0L)
        }
        val targetSeconds = durationMs / 1000f * plan.mixOutFraction
        val currentFeatures = current?.let { features[it.id] }
        if (currentFeatures == null || currentFeatures.bpm <= 0f) {
            return (targetSeconds * 1000f).toLong().coerceIn(0L, durationMs - 500L)
        }
        val beatSeconds = 60f / currentFeatures.bpm
        val barSeconds = beatSeconds * 4f
        val offset = currentFeatures.beatOffsetSeconds.coerceIn(0f, beatSeconds)
        val bars = ((targetSeconds - offset) / barSeconds).roundToInt()
        val aligned = offset + bars * barSeconds
        return (aligned * 1000f).toLong().coerceIn(0L, durationMs - 500L)
    }

    private fun alignedStartMs(
        track: MetadataTrack,
        plan: TransitionPlan,
        durationMs: Long
    ): Long {
        if (durationMs <= 0L) return 0L
        val targetSeconds = durationMs / 1000f * plan.mixInFraction
        val trackFeatures = features[track.id]
        if (trackFeatures == null || trackFeatures.bpm <= 0f) {
            return (targetSeconds * 1000f).toLong().coerceIn(0L, durationMs - 1_000L)
        }
        val beatSeconds = 60f / trackFeatures.bpm
        val barSeconds = beatSeconds * 4f
        val offset = trackFeatures.beatOffsetSeconds.coerceIn(0f, beatSeconds)
        val bars = ((targetSeconds - offset) / barSeconds).roundToInt()
        val aligned = offset + bars * barSeconds
        return (aligned * 1000f).toLong().coerceIn(0L, durationMs - 1_000L)
    }

    private fun keyDistance(first: String, second: String): Int {
        if (first.isBlank() || second.isBlank()) return 2
        val firstNumber = first.dropLast(1).toIntOrNull() ?: return 2
        val secondNumber = second.dropLast(1).toIntOrNull() ?: return 2
        val firstMode = first.last()
        val secondMode = second.last()
        if (firstMode == secondMode) {
            val difference = abs(firstNumber - secondNumber)
            return min(difference, 12 - difference)
        }
        return if (firstNumber == secondNumber) 0 else 2
    }

    private fun transitionCost(reference: AudioFeatures, candidate: AudioFeatures?): Float {
        if (candidate == null) return 500f
        val bpmDifference = abs(reference.bpm - candidate.bpm) /
            reference.bpm.coerceAtLeast(1f)
        return bpmDifference * 20f +
            keyDistance(reference.key, candidate.key) +
            abs(reference.energy - candidate.energy)
    }

    private fun refineQueue() {
        if (!isStarted || isTransitioning) return
        val currentFeatures = current?.let { features[it.id] } ?: return
        val start = index + 1
        if (start >= queue.size) return
        val end = min(queue.size, start + 5)
        val window = queue.subList(start, end).toMutableList()
        window.sortWith(compareBy { transitionCost(currentFeatures, features[it.id]) })
        for (offset in window.indices) {
            queue[start + offset] = window[offset]
        }
        val newNext = queue.getOrNull(start)
        if (newNext?.id != next?.id) {
            next = newNext
            newNext?.let { loadTrack(idlePlayer(), it, 0f) }
            publish()
        }
    }

    private fun ensureAnalysis() {
        if (!localAnalysisEnabled) return
        if (!isStarted || analysisJob?.isActive == true) return
        analysisJob = scope.launch {
            try {
                val port = settingsProvider.settingsState.value?.playbackProxyServerPort
                    ?: 14769
                while (isStarted) {
                    val candidates = buildList {
                        current?.let { add(it) }
                        val end = min(queue.lastIndex, index + 2)
                        for (position in index + 1..end) {
                            queue.getOrNull(position)?.let { add(it) }
                        }
                    }.distinctBy { it.id }
                        .filter { !features.containsKey(it.id) }
                    if (candidates.isEmpty()) break
                    var analyzedAny = false
                    for (track in candidates) {
                        if (!isStarted) break
                        val result = analyzer.analyze(
                            track.id,
                            "http://127.0.0.1:$port/stream/${track.id}"
                        ) ?: continue
                        features[track.id] = result
                        analyzedAny = true
                        refineQueue()
                        publish()
                    }
                    if (!analyzedAny) break
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            } finally {
                analysisJob = null
                publish()
            }
        }
    }

    private fun bassBand(equalizer: Equalizer): Short {
        var bestBand: Short = 0
        var bestDistance = Float.MAX_VALUE
        for (bandIndex in 0 until equalizer.numberOfBands.toInt()) {
            val band = bandIndex.toShort()
            val range = runCatching {
                equalizer.getBandFreqRange(band)
            }.getOrNull() ?: continue
            val centerHz = ((range[0].toLong() + range[1].toLong()) / 2L) / 1000f
            val distance = abs(centerHz - 90f)
            if (distance < bestDistance) {
                bestDistance = distance
                bestBand = band
            }
        }
        return bestBand
    }
}

private fun MetadataTrack.artworkUrl(): String? {
    return (album?.thumbnails ?: thumbnails)
        ?.maxByOrNull { it.width * it.height }
        ?.url
}

private fun MetadataTrack.toMedia3Item(port: Int): androidx.media3.common.MediaItem {
    val artwork = artworkUrl()
    return androidx.media3.common.MediaItem.Builder()
        .setUri("http://127.0.0.1:$port/stream/$id")
        .setMediaMetadata(
            androidx.media3.common.MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artists.joinToString(", ") { it.name })
                .setAlbumTitle(album?.title)
                .apply {
                    if (!artwork.isNullOrBlank()) {
                        setArtworkUri(android.net.Uri.parse(artwork))
                    }
                }
                .build()
        )
        .build()
}
