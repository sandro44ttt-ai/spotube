package org.spotube.spotube.core.ui.component

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.audiofx.Equalizer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import coil3.compose.AsyncImage
import com.composeunstyled.Text
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import org.spotube.spotube.core.audioplayer.AudioPlayerInterface
import org.spotube.spotube.core.ui.component.spotify.SpotifyCircleTopBarButton
import org.spotube.spotube.core.ui.component.spotify.SpotifyTokens
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.media.AutoMixForegroundService
import org.spotube.spotube.modules.plugin.PluginProvider
import org.spotube.spotube.modules.settings.SettingsProvider
import org.spotube.spotube.resources.iconsax.Iconsax
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.auto_mix_close
import spotube.composeapp.generated.resources.auto_mix_next
import spotube.composeapp.generated.resources.auto_mix_next_track
import spotube.composeapp.generated.resources.auto_mix_no_tracks
import spotube.composeapp.generated.resources.auto_mix_pause
import spotube.composeapp.generated.resources.auto_mix_play
import spotube.composeapp.generated.resources.auto_mix_previous
import spotube.composeapp.generated.resources.auto_mix_style_blend
import spotube.composeapp.generated.resources.auto_mix_style_cut
import spotube.composeapp.generated.resources.auto_mix_style_mashup
import spotube.composeapp.generated.resources.auto_mix_title
import spotube.composeapp.generated.resources.auto_mix_transitioning
import kotlin.math.abs
import kotlin.math.sqrt

private enum class TransitionStyle { Cut, Blend, Mashup }

private data class TransitionPlan(
    val style: TransitionStyle,
    val durationMs: Long,
    val mixOutFraction: Float,
    val mixInFraction: Float
)

private fun planTransition(current: MetadataTrack?, next: MetadataTrack?): TransitionPlan {
    val canMashup = (current?.durationMs ?: 0L) > 120_000L &&
        (next?.durationMs ?: 0L) > 120_000L
    val hash = (((current?.id ?: "") + (next?.id ?: "")).hashCode() and 0x7FFFFFFF)
    return when {
        !canMashup -> if (hash % 2 == 0) {
            TransitionPlan(TransitionStyle.Cut, 1_500L, 0.80f, 0.25f)
        } else {
            TransitionPlan(TransitionStyle.Blend, 7_000L, 0.84f, 0.18f)
        }

        hash % 3 == 0 -> TransitionPlan(TransitionStyle.Cut, 1_500L, 0.80f, 0.25f)
        hash % 3 == 1 -> TransitionPlan(TransitionStyle.Blend, 7_000L, 0.84f, 0.18f)
        else -> TransitionPlan(TransitionStyle.Mashup, 14_000L, 0.68f, 0.32f)
    }
}

@Composable
actual fun AutoMixDialog(
    visible: Boolean,
    title: String,
    tracks: List<MetadataTrack>,
    onDismiss: () -> Unit
) {
    if (!visible) return

    val context = LocalContext.current
    val mainPlayer: AudioPlayerInterface = koinInject()
    val settingsProvider: SettingsProvider = koinInject()
    val pluginProvider: PluginProvider = koinInject()
    val settings by settingsProvider.settingsState.collectAsStateWithLifecycle()
    val port = settings?.playbackProxyServerPort ?: 14769
    val scope = rememberCoroutineScope()
    val audioManager = remember {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    val audioAttributes = remember {
        AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
    }
    val deckA = remember {
        ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, false)
            .build()
    }
    val deckB = remember {
        ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, false)
            .build()
    }

    var queue by remember { mutableStateOf<List<MetadataTrack>>(emptyList()) }
    var index by remember { mutableStateOf(0) }
    var currentTrack by remember { mutableStateOf<MetadataTrack?>(null) }
    var nextTrack by remember { mutableStateOf<MetadataTrack?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var isTransitioning by remember { mutableStateOf(false) }
    var transitionProgress by remember { mutableFloatStateOf(0f) }
    var transitionStyle by remember { mutableStateOf(TransitionStyle.Blend) }
    var position by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var activeIsA by remember { mutableStateOf(true) }
    var eqA by remember { mutableStateOf<Equalizer?>(null) }
    var eqB by remember { mutableStateOf<Equalizer?>(null) }
    var sessionA by remember { mutableStateOf(0) }
    var sessionB by remember { mutableStateOf(0) }
    var focusRequest by remember { mutableStateOf<AudioFocusRequest?>(null) }

    fun activePlayer() = if (activeIsA) deckA else deckB
    fun idlePlayer() = if (activeIsA) deckB else deckA
    fun activeEq() = if (activeIsA) eqA else eqB
    fun idleEq() = if (activeIsA) eqB else eqA

    fun attachEqualizer(sessionId: Int, isA: Boolean) {
        if (sessionId == 0) return
        if (isA && eqA != null) return
        if (!isA && eqB != null) return
        val equalizer = runCatching {
            Equalizer(0, sessionId).apply { enabled = true }
        }.getOrNull()
        if (isA) eqA = equalizer else eqB = equalizer
    }

    fun applyBass(equalizer: Equalizer?, db: Float) {
        if (equalizer == null) return
        runCatching {
            val range = equalizer.bandLevelRange
            val level = (db * 100f).toInt().coerceIn(range[0].toInt(), range[1].toInt())
            equalizer.setBandLevel(bassBand(equalizer), level.toShort())
        }
    }

    fun loadTrack(player: ExoPlayer, track: MetadataTrack, startVolume: Float) {
        player.setMediaItem(track.toMedia3Item(port))
        player.prepare()
        player.volume = startVolume
    }

    fun preloadNext() {
        val next = queue.getOrNull(index + 1)
        if (next == null) {
            scope.launch {
                val additions = fetchRecommendations(
                    pluginProvider = pluginProvider,
                    seeds = queue.takeLast(5),
                    existingIds = queue.map { it.id }.toSet()
                )
                queue = queue + if (additions.isNotEmpty()) additions else queue
                val updatedNext = queue.getOrNull(index + 1)
                nextTrack = updatedNext
                if (updatedNext != null) {
                    loadTrack(idlePlayer(), updatedNext, 0f)
                }
            }
        } else {
            nextTrack = next
            loadTrack(idlePlayer(), next, 0f)
        }
    }

    fun startTransition() {
        if (isTransitioning) return
        val next = nextTrack ?: return
        val plan = planTransition(currentTrack, next)
        val from = activePlayer()
        val to = idlePlayer()
        isTransitioning = true
        transitionStyle = plan.style
        transitionProgress = 0f
        val targetDuration = to.duration
        val startAt = if (targetDuration > 0L) {
            (targetDuration * plan.mixInFraction).toLong().coerceAtLeast(0L)
        } else {
            0L
        }
        to.seekTo(startAt)
        to.volume = 0f
        to.play()
        scope.launch {
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
            index += 1
            currentTrack = next
            position = 0
            duration = 0
            isTransitioning = false
            transitionProgress = 0f
            preloadNext()
        }
    }

    fun skipNext() {
        if (isTransitioning) return
        val next = queue.getOrNull(index + 1)
        if (next == null) {
            preloadNext()
            return
        }
        activePlayer().pause()
        index += 1
        currentTrack = next
        position = 0
        duration = 0
        loadTrack(activePlayer(), next, 1f)
        activePlayer().play()
        isPlaying = true
        nextTrack = queue.getOrNull(index + 1)
        preloadNext()
    }

    fun skipPrevious() {
        if (isTransitioning) return
        val previousIndex = (index - 1).coerceAtLeast(0)
        val previous = queue.getOrNull(previousIndex) ?: return
        activePlayer().pause()
        index = previousIndex
        currentTrack = previous
        position = 0
        duration = 0
        loadTrack(activePlayer(), previous, 1f)
        activePlayer().play()
        isPlaying = true
        nextTrack = queue.getOrNull(index + 1)
        preloadNext()
    }

    fun startBackgroundPlayback() {
        AutoMixForegroundService.start(context.applicationContext)
        if (focusRequest == null) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setOnAudioFocusChangeListener { change ->
                    if (
                        change == AudioManager.AUDIOFOCUS_LOSS ||
                        change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
                    ) {
                        deckA.pause()
                        deckB.pause()
                        isPlaying = false
                    }
                }
                .build()
            runCatching { audioManager.requestAudioFocus(request) }
            focusRequest = request
        }
    }

    fun stopBackgroundPlayback() {
        deckA.pause()
        deckB.pause()
        isPlaying = false
        focusRequest?.let { request ->
            runCatching { audioManager.abandonAudioFocusRequest(request) }
        }
        focusRequest = null
        AutoMixForegroundService.stop(context.applicationContext)
    }

    LaunchedEffect(visible, tracks) {
        if (tracks.isEmpty()) return@LaunchedEffect
        mainPlayer.pause()
        startBackgroundPlayback()
        queue = smartOrder(tracks)
        index = 0
        activeIsA = true
        isTransitioning = false
        isPlaying = true
        currentTrack = queue.firstOrNull()
        nextTrack = queue.getOrNull(1)
        currentTrack?.let { loadTrack(deckA, it, 1f) }
        nextTrack?.let { loadTrack(deckB, it, 0f) }
        deckA.playWhenReady = true
        if (queue.size <= 1) {
            preloadNext()
        }
    }

    LaunchedEffect(visible, isPlaying, isTransitioning) {
        if (!visible) return@LaunchedEffect
        while (true) {
            val player = activePlayer()
            position = player.currentPosition.coerceAtLeast(0L)
            duration = player.duration.coerceAtLeast(0L)
            val plan = planTransition(currentTrack, nextTrack)
            val triggerAt = if (duration > 20_000L) {
                (duration * plan.mixOutFraction).toLong()
            } else {
                (duration - 6_000L).coerceAtLeast(0L)
            }
            if (
                isPlaying &&
                !isTransitioning &&
                nextTrack != null &&
                duration > 0L &&
                position >= triggerAt
            ) {
                startTransition()
            }
            delay(100)
        }
    }

    DisposableEffect(deckA) {
        val listener = object : AnalyticsListener {
            override fun onAudioSessionIdChanged(
                eventTime: AnalyticsListener.EventTime,
                audioSessionId: Int
            ) {
                sessionA = audioSessionId
                attachEqualizer(audioSessionId, isA = true)
            }
        }
        val errorListener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                if (activeIsA) skipNext()
            }
        }
        deckA.addAnalyticsListener(listener)
        deckA.addListener(errorListener)
        onDispose {
            deckA.removeAnalyticsListener(listener)
            deckA.removeListener(errorListener)
            deckA.release()
        }
    }
    DisposableEffect(deckB) {
        val listener = object : AnalyticsListener {
            override fun onAudioSessionIdChanged(
                eventTime: AnalyticsListener.EventTime,
                audioSessionId: Int
            ) {
                sessionB = audioSessionId
                attachEqualizer(audioSessionId, isA = false)
            }
        }
        val errorListener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                if (!activeIsA) skipNext()
            }
        }
        deckB.addAnalyticsListener(listener)
        deckB.addListener(errorListener)
        onDispose {
            deckB.removeAnalyticsListener(listener)
            deckB.removeListener(errorListener)
            deckB.release()
        }
    }
    DisposableEffect(eqA) { onDispose { runCatching { eqA?.release() } } }
    DisposableEffect(eqB) { onDispose { runCatching { eqB?.release() } } }
    DisposableEffect(Unit) {
        onDispose { stopBackgroundPlayback() }
    }

    LaunchedEffect(sessionA, sessionB) {
        if (eqA == null && sessionA != 0) attachEqualizer(sessionA, isA = true)
        if (eqB == null && sessionB != 0) attachEqualizer(sessionB, isA = false)
    }

    Dialog(
        onDismissRequest = {
            stopBackgroundPlayback()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to SpotifyTokens.Sheet,
                        0.55f to SpotifyTokens.Base,
                        1f to SpotifyTokens.Base
                    )
                )
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SpotifyCircleTopBarButton(
                        icon = Iconsax.SpotifyChevronDown,
                        contentDescription = stringResource(Res.string.auto_mix_close),
                        onClick = {
                            stopBackgroundPlayback()
                            onDismiss()
                        }
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.auto_mix_title),
                            style = AppTheme.typography.headlineSmall.copy(
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = title,
                            style = AppTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                color = SpotifyTokens.Muted
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    currentTrack?.let { track ->
                        AsyncImage(
                            model = track.artworkUrl(),
                            contentDescription = track.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SpotifyTokens.Surface),
                            contentScale = ContentScale.Crop
                        )
                    } ?: Text(
                        text = stringResource(Res.string.auto_mix_no_tracks),
                        style = AppTheme.typography.bodyMedium,
                        color = SpotifyTokens.Muted
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(2.dp))
                        .background(SpotifyTokens.Green)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "AUTO MIX",
                        style = AppTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = SpotifyTokens.Base
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = currentTrack?.title.orEmpty(),
                    style = AppTheme.typography.headlineSmall.copy(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = currentTrack?.artists?.joinToString(", ") { it.name }.orEmpty(),
                    style = AppTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = SpotifyTokens.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SpotifyCircleTopBarButton(
                        icon = Iconsax.SpotifySkipBackGlyph,
                        contentDescription = stringResource(Res.string.auto_mix_previous),
                        onClick = ::skipPrevious
                    )
                    SpotifyCircleTopBarButton(
                        icon = if (isPlaying) Iconsax.SpotifyPause else Iconsax.SpotifyPlay,
                        contentDescription = if (isPlaying) {
                            stringResource(Res.string.auto_mix_pause)
                        } else {
                            stringResource(Res.string.auto_mix_play)
                        },
                        onClick = {
                            if (!isTransitioning) {
                                if (isPlaying) {
                                    deckA.pause()
                                    deckB.pause()
                                    isPlaying = false
                                } else {
                                    activePlayer().play()
                                    isPlaying = true
                                }
                            }
                        }
                    )
                    SpotifyCircleTopBarButton(
                        icon = Iconsax.SpotifySkipNextGlyph,
                        contentDescription = stringResource(Res.string.auto_mix_next_track),
                        onClick = ::skipNext
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                val progress = if (isTransitioning) {
                    transitionProgress
                } else if (duration > 0) {
                    (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.25f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(4.dp)
                            .background(Color.White)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatAutoMixTime(position),
                        style = AppTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            color = SpotifyTokens.Muted
                        )
                    )
                    if (isTransitioning) {
                        Text(
                            text = stringResource(Res.string.auto_mix_transitioning) + " · " + when (transitionStyle) {
                                TransitionStyle.Cut -> stringResource(Res.string.auto_mix_style_cut)
                                TransitionStyle.Blend -> stringResource(Res.string.auto_mix_style_blend)
                                TransitionStyle.Mashup -> stringResource(Res.string.auto_mix_style_mashup)
                            },
                            style = AppTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                color = SpotifyTokens.Green
                            )
                        )
                    }
                    Text(
                        text = formatAutoMixTime(duration),
                        style = AppTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            color = SpotifyTokens.Muted
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                nextTrack?.let { next ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SpotifyTokens.Surface)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = next.artworkUrl(),
                            contentDescription = next.title,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(SpotifyTokens.Highlight),
                            contentScale = ContentScale.Crop
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 10.dp)
                        ) {
                            Text(
                                text = stringResource(Res.string.auto_mix_next),
                                style = AppTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    color = SpotifyTokens.Muted
                                )
                            )
                            Text(
                                text = next.title,
                                style = AppTheme.typography.titleSmall.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

private suspend fun fetchRecommendations(
    pluginProvider: PluginProvider,
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

private fun smartOrder(tracks: List<MetadataTrack>): List<MetadataTrack> {
    if (tracks.size <= 2) return tracks
    val remaining = tracks.toMutableList()
    val ordered = mutableListOf(remaining.removeAt(0))
    while (remaining.isNotEmpty()) {
        val lastArtist = ordered.last().artists.firstOrNull()?.id.orEmpty()
        val differentArtistIndex = remaining.indexOfFirst { track ->
            track.artists.none { it.id == lastArtist }
        }
        val pickIndex = if (differentArtistIndex >= 0) differentArtistIndex else 0
        ordered += remaining.removeAt(pickIndex)
    }
    return ordered
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

private fun bassBand(equalizer: Equalizer): Short {
    var bestBand: Short = 0
    var bestDistance = Float.MAX_VALUE
    for (index in 0 until equalizer.numberOfBands.toInt()) {
        val band = index.toShort()
        val range = runCatching { equalizer.getBandFreqRange(band) }.getOrNull() ?: continue
        val centerHz = ((range[0].toLong() + range[1].toLong()) / 2L) / 1000f
        val distance = abs(centerHz - 90f)
        if (distance < bestDistance) {
            bestDistance = distance
            bestBand = band
        }
    }
    return bestBand
}

private fun formatAutoMixTime(value: Long): String {
    val totalSeconds = (value / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
