package org.spotube.spotube.core.automix

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.util.Log
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import org.spotube.plugin_interfaces.plugin_apis.audio.StreamProtocol
import org.spotube.spotube.core.server.StreamRequest
import org.spotube.spotube.core.server.StreamTrackRegistry
import org.spotube.spotube.core.server.StreamingUrlRepository
import org.spotube.spotube.media.AutoMixForegroundService
import org.spotube.spotube.modules.plugin.PluginProvider
import org.spotube.spotube.modules.settings.SettingsProvider
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

class AutoMixPlaybackManager(
    private val context: Context,
    private val settingsProvider: SettingsProvider,
    private val pluginProvider: PluginProvider,
    private val analyzer: AudioFeatureAnalyzer,
    private val streamingUrlRepository: StreamingUrlRepository
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
        val isAnalyzing: Boolean = false,
        val playbackState: Int = Player.STATE_IDLE,
        val lastError: String = "",
        val debugInfo: String = "",
        val transitionKind: TransitionKind = TransitionKind.HardCut,
        val planReason: String = "",
        val planLevel: AutoMixLevel? = null,
        val planVibeDistance: Float = 1f
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
    private val dspA = MixDspProcessor()
    private val dspB = MixDspProcessor()
    private val deckA = ExoPlayer.Builder(context)
        .setRenderersFactory(AutoMixRenderersFactory(context, dspA))
        .setAudioAttributes(audioAttributes, false)
        .build()
    private val deckB = ExoPlayer.Builder(context)
        .setRenderersFactory(AutoMixRenderersFactory(context, dspB))
        .setAudioAttributes(audioAttributes, false)
        .build()

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
    private var transitionKind: TransitionKind = TransitionKind.HardCut
    private var planReason: String = ""
    private var planLevel: AutoMixLevel? = null
    private var planVibeDistance: Float = 1f
    private var positionMs: Long = 0L
    private var durationMs: Long = 0L
    private var sessionKey: String = ""
    private var title: String = ""
    private var pollJob: Job? = null
    private var transitionJob: Job? = null
    private var loopJob: Job? = null
    private var analysisJob: Job? = null
    private val features = mutableMapOf<String, AudioFeatures>()
    private var activeSpeed: Float = 1f
    private var activePitchSemitones: Int = 0
    private var lastStartTrackId: String? = null
    private val localAnalysisEnabled = true
    private var lastError: String = ""

    init {
        deckA.addAnalyticsListener(object : AnalyticsListener {
            override fun onAudioSessionIdChanged(
                eventTime: AnalyticsListener.EventTime,
                audioSessionId: Int
            ) {
                sessionA = audioSessionId
            }
        })
        deckB.addAnalyticsListener(object : AnalyticsListener {
            override fun onAudioSessionIdChanged(
                eventTime: AnalyticsListener.EventTime,
                audioSessionId: Int
            ) {
                sessionB = audioSessionId
            }
        })
        deckA.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Log.e("AutoMix", "deckA error: ${error.errorCode} ${error.message}", error)
                if (activeIsA) {
                    lastError = "A${error.errorCode}: ${error.message.orEmpty()}"
                    skipNext()
                }
            }
        })
        deckB.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Log.e("AutoMix", "deckB error: ${error.errorCode} ${error.message}", error)
                if (!activeIsA) {
                    lastError = "B${error.errorCode}: ${error.message.orEmpty()}"
                    skipNext()
                }
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
        lastError = ""
        planLevel = null
        planVibeDistance = 1f
        planReason = ""
        queue.clear()
        features.clear()
        activeSpeed = 1f
        activePitchSemitones = 0
        dspA.resetEffects()
        dspB.resetEffects()
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
        planLevel = null
        planVibeDistance = 1f
        planReason = ""
        pollJob?.cancel()
        transitionJob?.cancel()
        analysisJob?.cancel()
        loopJob?.cancel()
        pollJob = null
        transitionJob = null
        analysisJob = null
        loopJob = null
        deckA.pause()
        deckB.pause()
        deckA.playbackParameters = PlaybackParameters(1f)
        deckB.playbackParameters = PlaybackParameters(1f)
        dspA.resetEffects()
        dspB.resetEffects()
        deckA.clearMediaItems()
        deckB.clearMediaItems()
        current = null
        next = null
        StreamTrackRegistry.unregisterAll(queue.map { it.id })
        queue.clear()
        features.clear()
        activeSpeed = 1f
        activePitchSemitones = 0
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

    private fun skipNext() {
        if (!isStarted || isTransitioning) return
        val target = queue.getOrNull(index + 1) ?: return
        activePlayer().pause()
        activePlayer().playbackParameters = PlaybackParameters(1f)
        activeSpeed = 1f
        activePitchSemitones = 0
        if (activeIsA) dspA.resetEffects() else dspB.resetEffects()
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

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = scope.launch {
            while (isStarted) {
                val player = activePlayer()
                positionMs = player.currentPosition.coerceAtLeast(0L)
                durationMs = player.duration.coerceAtLeast(0L)
                if (!isTransitioning && isPlaying) {
                    if (player.volume < 0.99f) player.volume = 1f
                    if (
                        player.playbackState == Player.STATE_READY &&
                        !player.isPlaying
                    ) {
                        player.play()
                    }
                }
                val plan = currentPlan()
                val triggerAt = plan?.let { transitionTriggerAt(it, durationMs) }
                    ?: Long.MAX_VALUE
                if (
                    isPlaying &&
                    !isTransitioning &&
                    next != null &&
                    plan != null &&
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
        val plan = currentPlan() ?: return
        val from = activePlayer()
        val to = idlePlayer()
        val fromDsp = if (activeIsA) dspA else dspB
        val toDsp = if (activeIsA) dspB else dspA
        fromDsp.resetEffects()
        toDsp.resetEffects()
        analysisJob?.cancel()
        analysisJob = null
        isTransitioning = true
        transitionKind = plan.kind
        transitionStyle = styleFor(plan.kind)
        planReason = plan.reason
        planLevel = plan.level
        planVibeDistance = plan.vibeDistance
        transitionProgress = 0f
        val pitchFactor = 2.0.pow(plan.pitchSemitones / 12.0).toFloat()
        to.playbackParameters = PlaybackParameters(plan.tempoRatio, pitchFactor)
        to.seekTo((plan.incomingCueMs + plan.incomingOffsetMs).coerceAtLeast(0L))
        to.volume = 0f
        to.play()
        if (plan.loopBeats > 0 && plan.loopBpm > 0f) {
            toDsp.startLoop(plan.loopBpm, plan.loopBeats, plan.loopRoll)
        }
        val ownerDsp = if (plan.bassOwnerIsA) dspA else dspB
        val secondaryDsp = if (plan.bassOwnerIsA) dspB else dspA
        ownerDsp.highPassHz = 20f
        ownerDsp.midDipDb = plan.midDipDb
        ownerDsp.centerAttenuationDb = plan.centerAttenuationDb
        secondaryDsp.highPassHz = plan.secondaryHighPassHz
        secondaryDsp.midDipDb = 0f
        secondaryDsp.centerAttenuationDb = 0f
        val headroomGain = 10.0.pow(plan.headroomDb / 20.0).toFloat()
        fromDsp.gain = 1f
        toDsp.gain = headroomGain
        val remainingMs = if (from.duration > 0L) {
            (from.duration - from.currentPosition).coerceAtLeast(0L)
        } else {
            plan.durationMs
        }
        val effectiveDurationMs = plan.durationMs.coerceAtMost(
            (remainingMs - 500L).coerceAtLeast(4_000L)
        )
        transitionJob = scope.launch {
            try {
            var loopStopped = false
            val steps = (effectiveDurationMs / 50L).toInt().coerceAtLeast(1)
            for (step in 1..steps) {
                if (from.playbackState == Player.STATE_ENDED) {
                    toDsp.gain = 1f
                    to.volume = 1f
                    break
                }
                val t = step.toFloat() / steps.toFloat()
                transitionProgress = t
                from.volume = sqrt(1f - t)
                to.volume = sqrt(t)
                if (plan.loopBeats > 0 && !loopStopped && t >= 0.5f) {
                    toDsp.stopLoop()
                    loopStopped = true
                }
                if (t > 0.35f) {
                    toDsp.gain = headroomGain +
                        (1f - headroomGain) * ((t - 0.35f) / 0.55f).coerceIn(0f, 1f)
                }
                if (plan.kind == TransitionKind.SafeBlend) {
                    secondaryDsp.lowPassHz = 20_000f - 12_000f * t * t
                }
                if (t > 0.72f) {
                    fromDsp.echoMix = ((t - 0.72f) / 0.28f).coerceIn(0f, 1f) * 0.2f
                    fromDsp.echoFeedback = 0.18f
                }
                val ramp = (t * 4f).coerceAtMost(1f)
                val speed = 1f + (plan.tempoRatio - 1f) * ramp
                to.playbackParameters = PlaybackParameters(speed, pitchFactor)
                if (step % 2 == 0 || step == steps) publish()
                delay(50)
            }
            loopJob?.cancel()
            loopJob = null
            fromDsp.resetEffects()
            toDsp.highPassHz = 20f
            toDsp.lowPassHz = 20_000f
            toDsp.echoMix = 0f
            toDsp.echoFeedback = 0f
            toDsp.midDipDb = 0f
            toDsp.centerAttenuationDb = 0f
            toDsp.gain = 1f
            from.pause()
            from.volume = 1f
            to.volume = 1f
            activeIsA = !activeIsA
            activeSpeed = plan.tempoRatio
            activePitchSemitones = plan.pitchSemitones
            index += 1
            current = target
            positionMs = 0L
            durationMs = 0L
            isTransitioning = false
            transitionProgress = 0f
            preloadNext()
            ensureAnalysis()
            publish()
            } catch (e: Exception) {
                Log.e("AutoMix", "transition failed", e)
                lastError = "transition: ${e.message.orEmpty()}"
                loopJob?.cancel()
                loopJob = null
                fromDsp.resetEffects()
                toDsp.resetEffects()
                from.pause()
                to.pause()
                from.volume = 1f
                to.volume = 0f
                isTransitioning = false
                transitionProgress = 0f
                publish()
            }
        }
    }

    private fun startLoop(player: ExoPlayer, spec: LoopSpec) {
        loopJob?.cancel()
        loopJob = scope.launch {
            var loops = 0
            while (isActive && loops < 2) {
                if (player.currentPosition >= spec.endMs - 60L) {
                    player.seekTo(spec.startMs)
                    loops++
                }
                delay(20)
            }
        }
    }

    private fun styleFor(kind: TransitionKind): TransitionStyle = when (kind) {
        TransitionKind.HardCut -> TransitionStyle.Cut
        TransitionKind.Mashup -> TransitionStyle.Mashup
        else -> TransitionStyle.Blend
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

    private fun publish() {
        AutoMixController.setPlaybackActive(isStarted)
        val currentFeatures = current?.let { features[it.id] }
        val nextFeatures = next?.let { features[it.id] }
        val player = activePlayer()
        val session = if (activeIsA) sessionA else sessionB
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
            isAnalyzing = analysisJob?.isActive == true,
            playbackState = player.playbackState,
            lastError = lastError,
            debugInfo = "state=${player.playbackState} playing=${player.isPlaying} " +
                "vol=${(player.volume * 100).roundToInt()}% " +
                "gain=${((if (activeIsA) dspA.gain else dspB.gain) * 100).roundToInt()}% " +
                "eff=${((player.volume * (if (activeIsA) dspA.gain else dspB.gain)) * 100).roundToInt()}% " +
                "session=$session " +
                "buffered=${player.bufferedPosition} " +
                "media=${audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)}/" +
                "${audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)}",
            transitionKind = transitionKind,
            planReason = planReason,
            planLevel = planLevel,
            planVibeDistance = planVibeDistance
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

    private fun currentPlan(): TransitionPlan? {
        val currentTrack = current ?: return null
        val nextTrack = next ?: return null
        return AutoMixPlanner.plan(
            current = currentTrack,
            next = nextTrack,
            currentFeatures = features[currentTrack.id],
            nextFeatures = features[nextTrack.id],
            activeTempo = activeSpeed,
            currentPitchSemitones = activePitchSemitones
        )
    }

    private fun transitionTriggerAt(plan: TransitionPlan, durationMs: Long): Long {
        if (durationMs <= 20_000L) {
            return (durationMs - 6_000L).coerceAtLeast(0L)
        }
        return plan.outgoingCueMs.coerceIn(0L, durationMs - 500L)
    }


    private fun refineQueue() {
        if (!isStarted || isTransitioning) return
        val currentTrack = current ?: return
        val currentFeatures = features[currentTrack.id] ?: return
        val start = index + 1
        if (start >= queue.size) return
        val window = queue.subList(start, queue.size).toList()
        val recent = queue.subList(
            maxOf(0, index - 8),
            index + 1
        ).map { it.id }.toSet()
        val ordered = AutoMixQueueSelector.orderUpcoming(
            current = currentTrack,
            currentFeatures = currentFeatures,
            candidates = window,
            features = features,
            recentTrackIds = recent,
            currentPitchSemitones = activePitchSemitones
        )
        for (offset in ordered.indices) {
            queue[start + offset] = ordered[offset]
        }
        Log.d(
            "AutoMix",
            "queue top: " + ordered.take(5).joinToString(" | ") { track ->
                val f = features[track.id]
                "${track.title.take(18)} bpm=${f?.bpm?.roundToInt()} " +
                    "key=${f?.key} full=${f?.analysisComplete}"
            }
        )
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
                val settings = settingsProvider.settingsState.value ?: return@launch
                val request = StreamRequest(
                    format = settings.streamingMusicFormat,
                    quality = settings.streamingMusicQuality,
                    progressiveOnly = true
                )
                val attemptedFull = mutableSetOf<String>()
                val attemptedLight = mutableSetOf<String>()
                while (isStarted) {
                    val lookaheadEnd = min(queue.lastIndex, index + 2)
                    val fullCandidates = buildList {
                        current?.let { add(it) }
                        for (position in index + 1..lookaheadEnd) {
                            queue.getOrNull(position)?.let { add(it) }
                        }
                    }.distinctBy { it.id }
                        .filter {
                            it.id !in attemptedFull &&
                                features[it.id]?.analysisComplete != true
                        }
                    val lightCandidates = if (fullCandidates.isEmpty()) {
                        queue.drop(index + 1).filter {
                            it.id !in features &&
                                it.id !in attemptedLight &&
                                it.id !in attemptedFull
                        }
                    } else {
                        emptyList()
                    }
                    val track = fullCandidates.firstOrNull()
                        ?: lightCandidates.firstOrNull()
                        ?: break
                    val fullTrack = fullCandidates.isNotEmpty()
                    if (fullTrack) {
                        attemptedFull += track.id
                    } else {
                        attemptedLight += track.id
                    }
                    if (!isStarted) break
                    val info = streamingUrlRepository.resolveStreamInfo(
                        track = track,
                        request = request
                    )
                    if (info == null || info.protocol != StreamProtocol.PROGRESSIVE) {
                        continue
                    }
                    Log.d("AutoMix", "analysis start: ${track.id} full=$fullTrack")
                    val result = analyzer.analyze(track.id, info.url, fullTrack)
                    if (result == null) {
                        Log.w("AutoMix", "analysis failed: ${track.id} full=$fullTrack")
                        continue
                    }
                    Log.d(
                        "AutoMix",
                        "analysis done: ${track.id} bpm=${result.bpm} " +
                            "key=${result.key} full=${result.analysisComplete}"
                    )
                    features[track.id] = result
                    refineQueue()
                    publish()
                    delay(180)
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
