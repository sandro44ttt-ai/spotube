package org.spotube.spotube.core.ui.component

import android.media.audiofx.Equalizer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
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
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import com.composeunstyled.Text
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.spotube.spotube.core.audioplayer.AudioPlayerInterface
import org.spotube.spotube.core.ui.component.spotify.SpotifyCircleTopBarButton
import org.spotube.spotube.core.ui.component.spotify.SpotifyOutlinePillButton
import org.spotube.spotube.core.ui.component.spotify.SpotifyPrimaryButton
import org.spotube.spotube.core.ui.component.spotify.SpotifySecondaryButton
import org.spotube.spotube.core.ui.component.spotify.SpotifySeekBar
import org.spotube.spotube.core.ui.component.spotify.SpotifyTokens
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.resources.iconsax.Iconsax
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.mix_lab_align_b
import spotube.composeapp.generated.resources.mix_lab_bass
import spotube.composeapp.generated.resources.mix_lab_close
import spotube.composeapp.generated.resources.mix_lab_crossfader
import spotube.composeapp.generated.resources.mix_lab_deck_a
import spotube.composeapp.generated.resources.mix_lab_deck_b
import spotube.composeapp.generated.resources.mix_lab_drift
import spotube.composeapp.generated.resources.mix_lab_eq_active
import spotube.composeapp.generated.resources.mix_lab_eq_unavailable
import spotube.composeapp.generated.resources.mix_lab_hint
import spotube.composeapp.generated.resources.mix_lab_no_tracks
import spotube.composeapp.generated.resources.mix_lab_pause
import spotube.composeapp.generated.resources.mix_lab_play
import spotube.composeapp.generated.resources.mix_lab_preparing
import spotube.composeapp.generated.resources.mix_lab_speed
import spotube.composeapp.generated.resources.mix_lab_start_synced
import spotube.composeapp.generated.resources.mix_lab_state
import spotube.composeapp.generated.resources.mix_lab_state_buffering
import spotube.composeapp.generated.resources.mix_lab_state_ended
import spotube.composeapp.generated.resources.mix_lab_state_idle
import spotube.composeapp.generated.resources.mix_lab_state_ready
import spotube.composeapp.generated.resources.mix_lab_state_unknown
import spotube.composeapp.generated.resources.mix_lab_title
import kotlin.math.abs
import kotlin.math.sqrt

@Composable
actual fun MixLabDialog(
    visible: Boolean,
    onDismiss: () -> Unit
) {
    if (!visible) return

    val context = LocalContext.current
    val mainPlayer: AudioPlayerInterface = koinInject()
    val scope = rememberCoroutineScope()
    val playlist by mainPlayer.playlistFlow.collectAsStateWithLifecycle()
    val currentItem by mainPlayer.currentMediaItemFlow.collectAsStateWithLifecycle()

    val currentIndex = remember(playlist, currentItem?.url) {
        playlist.indexOfFirst { it.url == currentItem?.url }.coerceAtLeast(0)
    }
    val itemA = playlist.getOrNull(currentIndex)
    val itemB = playlist.getOrNull(currentIndex + 1)
        ?: playlist.firstOrNull { it.url != itemA?.url }

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

    var isPlaying by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }
    var crossfade by remember { mutableFloatStateOf(0.5f) }
    var speedA by remember { mutableFloatStateOf(1f) }
    var speedB by remember { mutableFloatStateOf(1f) }
    var bassA by remember { mutableFloatStateOf(0f) }
    var bassB by remember { mutableFloatStateOf(0f) }
    var positionA by remember { mutableStateOf(0L) }
    var positionB by remember { mutableStateOf(0L) }
    var durationA by remember { mutableStateOf(0L) }
    var durationB by remember { mutableStateOf(0L) }
    var stateA by remember { mutableStateOf(Player.STATE_IDLE) }
    var stateB by remember { mutableStateOf(Player.STATE_IDLE) }
    var sessionIdA by remember { mutableStateOf(0) }
    var sessionIdB by remember { mutableStateOf(0) }
    var eqA by remember { mutableStateOf<Equalizer?>(null) }
    var eqB by remember { mutableStateOf<Equalizer?>(null) }
    var eqStatusA by remember { mutableStateOf("") }
    var eqStatusB by remember { mutableStateOf("") }

    fun attachEqualizer(sessionId: Int, deckIsA: Boolean) {
        if (sessionId == 0) return
        if (deckIsA && eqA != null) return
        if (!deckIsA && eqB != null) return
        val equalizer = runCatching {
            Equalizer(0, sessionId).apply { enabled = true }
        }.getOrNull()
        if (deckIsA) {
            eqA = equalizer
            eqStatusA = if (equalizer != null) "ok" else "failed"
        } else {
            eqB = equalizer
            eqStatusB = if (equalizer != null) "ok" else "failed"
        }
    }

    fun applyBass(equalizer: Equalizer?, value: Float) {
        if (equalizer == null) return
        runCatching {
            val range = equalizer.bandLevelRange
            val level = (value * 100f).toInt().coerceIn(range[0].toInt(), range[1].toInt())
            equalizer.setBandLevel(bassBand(equalizer), level.toShort())
        }
    }

    LaunchedEffect(Unit) { mainPlayer.pause() }

    LaunchedEffect(itemA?.url, itemB?.url) {
        isPlaying = false
        itemA?.let { item ->
            deckA.setMediaItem(item.toMedia3Item())
            deckA.prepare()
        }
        itemB?.let { item ->
            deckB.setMediaItem(item.toMedia3Item())
            deckB.prepare()
        }
    }

    LaunchedEffect(crossfade) {
        deckA.volume = sqrt((1f - crossfade).coerceIn(0f, 1f))
        deckB.volume = sqrt(crossfade.coerceIn(0f, 1f))
    }
    LaunchedEffect(speedA) {
        deckA.playbackParameters = PlaybackParameters(speedA)
    }
    LaunchedEffect(speedB) {
        deckB.playbackParameters = PlaybackParameters(speedB)
    }
    LaunchedEffect(eqA, bassA) { applyBass(eqA, bassA) }
    LaunchedEffect(eqB, bassB) { applyBass(eqB, bassB) }

    DisposableEffect(deckA) {
        val listener = object : AnalyticsListener {
            override fun onAudioSessionIdChanged(eventTime: AnalyticsListener.EventTime, audioSessionId: Int) {
                sessionIdA = audioSessionId
                attachEqualizer(audioSessionId, deckIsA = true)
            }
        }
        deckA.addAnalyticsListener(listener)
        onDispose {
            deckA.removeAnalyticsListener(listener)
            deckA.release()
        }
    }
    DisposableEffect(deckB) {
        val listener = object : AnalyticsListener {
            override fun onAudioSessionIdChanged(eventTime: AnalyticsListener.EventTime, audioSessionId: Int) {
                sessionIdB = audioSessionId
                attachEqualizer(audioSessionId, deckIsA = false)
            }
        }
        deckB.addAnalyticsListener(listener)
        onDispose {
            deckB.removeAnalyticsListener(listener)
            deckB.release()
        }
    }
    DisposableEffect(eqA) {
        onDispose { runCatching { eqA?.release() } }
    }
    DisposableEffect(eqB) {
        onDispose { runCatching { eqB?.release() } }
    }

    LaunchedEffect(visible) {
        while (visible) {
            positionA = deckA.currentPosition
            positionB = deckB.currentPosition
            durationA = deckA.duration.coerceAtLeast(0L)
            durationB = deckB.duration.coerceAtLeast(0L)
            stateA = deckA.playbackState
            stateB = deckB.playbackState
            if (eqA == null && sessionIdA != 0) attachEqualizer(sessionIdA, deckIsA = true)
            if (eqB == null && sessionIdB != 0) attachEqualizer(sessionIdB, deckIsA = false)
            delay(200)
        }
    }

    fun playBoth() {
        deckA.play()
        deckB.play()
        isPlaying = true
    }

    fun pauseBoth() {
        deckA.pause()
        deckB.pause()
        isPlaying = false
    }

    fun startSynced() {
        scope.launch {
            isSyncing = true
            deckA.pause()
            deckB.pause()
            deckA.seekTo(0)
            deckB.seekTo(0)
            val bothReady = withTimeoutOrNull(8_000) {
                while (
                    deckA.playbackState != Player.STATE_READY ||
                    deckB.playbackState != Player.STATE_READY
                ) {
                    delay(25)
                }
                true
            } != null
            if (bothReady) {
                deckA.playWhenReady = true
                deckB.playWhenReady = true
                isPlaying = true
            }
            isSyncing = false
        }
    }

    fun alignBToA() {
        deckB.seekTo(deckA.currentPosition)
        if (deckA.isPlaying) {
            deckB.play()
        }
        isPlaying = deckA.isPlaying || deckB.isPlaying
    }

    Dialog(
        onDismissRequest = {
            pauseBoth()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SpotifyTokens.Base)
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SpotifyCircleTopBarButton(
                        icon = Iconsax.SpotifyChevronDown,
                        contentDescription = stringResource(Res.string.mix_lab_close),
                        onClick = {
                            pauseBoth()
                            onDismiss()
                        }
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.mix_lab_title),
                            style = AppTheme.typography.headlineSmall.copy(
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = stringResource(Res.string.mix_lab_hint),
                            style = AppTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                color = SpotifyTokens.Muted
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    SpotifyCircleTopBarButton(
                        icon = if (isPlaying) Iconsax.SpotifyPause else Iconsax.SpotifyPlay,
                        contentDescription = if (isPlaying) {
                            stringResource(Res.string.mix_lab_pause)
                        } else {
                            stringResource(Res.string.mix_lab_play)
                        },
                        onClick = {
                            if (isPlaying) pauseBoth() else playBoth()
                        }
                    )
                }

                if (itemA == null || itemB == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(Res.string.mix_lab_no_tracks),
                            style = AppTheme.typography.bodyMedium,
                            color = SpotifyTokens.Muted
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MixDeckCard(
                            label = stringResource(Res.string.mix_lab_deck_a),
                            title = itemA.title,
                            artist = itemA.artist,
                            positionMs = positionA,
                            durationMs = durationA,
                            speed = speedA,
                            onSpeedChange = { speedA = it },
                            bass = bassA,
                            onBassChange = { bassA = it },
                            eqAvailable = eqA != null,
                            eqStatus = eqStatusA
                        )
                        MixDeckCard(
                            label = stringResource(Res.string.mix_lab_deck_b),
                            title = itemB.title,
                            artist = itemB.artist,
                            positionMs = positionB,
                            durationMs = durationB,
                            speed = speedB,
                            onSpeedChange = { speedB = it },
                            bass = bassB,
                            onBassChange = { bassB = it },
                            eqAvailable = eqB != null,
                            eqStatus = eqStatusB
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SpotifyTokens.Surface)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(Res.string.mix_lab_crossfader),
                                    style = AppTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        color = SpotifyTokens.Muted
                                    )
                                )
                                Text(
                                    text = "${"%.2f".format(crossfade)}",
                                    style = AppTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        color = SpotifyTokens.Muted
                                    )
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "A",
                                    style = AppTheme.typography.labelLarge.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Box(modifier = Modifier.weight(1f)) {
                                    SpotifySeekBar(
                                        value = crossfade,
                                        onValueChange = { crossfade = it },
                                        onValueChangeFinished = {},
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                Text(
                                    text = "B",
                                    style = AppTheme.typography.labelLarge.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SpotifyTokens.Surface)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = stringResource(
                                    Res.string.mix_lab_state,
                                    stateLabel(stateA),
                                    stateLabel(stateB)
                                ),
                                style = AppTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    color = SpotifyTokens.Muted
                                )
                            )
                            Text(
                                text = if (isSyncing) {
                                    stringResource(Res.string.mix_lab_preparing)
                                } else if (stateA == Player.STATE_READY && stateB == Player.STATE_READY) {
                                    stringResource(
                                        Res.string.mix_lab_drift,
                                        (positionA - positionB).toInt()
                                    )
                                } else {
                                    stringResource(Res.string.mix_lab_preparing)
                                },
                                style = AppTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    color = if (stateA == Player.STATE_READY && stateB == Player.STATE_READY) {
                                        Color.White
                                    } else {
                                        SpotifyTokens.Muted
                                    }
                                )
                            )
                        }

                        SpotifyPrimaryButton(
                            text = stringResource(Res.string.mix_lab_play),
                            onClick = ::playBoth,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SpotifyOutlinePillButton(
                                text = stringResource(Res.string.mix_lab_start_synced),
                                icon = Iconsax.SpotifyRepeatGlyph,
                                onClick = ::startSynced,
                                modifier = Modifier.weight(1f)
                            )
                            SpotifySecondaryButton(
                                text = stringResource(Res.string.mix_lab_align_b),
                                onClick = ::alignBToA,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MixDeckCard(
    label: String,
    title: String,
    artist: String,
    positionMs: Long,
    durationMs: Long,
    speed: Float,
    onSpeedChange: (Float) -> Unit,
    bass: Float,
    onBassChange: (Float) -> Unit,
    eqAvailable: Boolean,
    eqStatus: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SpotifyTokens.Surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label.uppercase(),
                style = AppTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    color = SpotifyTokens.Muted
                )
            )
            Text(
                text = if (eqAvailable) {
                    stringResource(Res.string.mix_lab_eq_active)
                } else if (eqStatus == "failed") {
                    stringResource(Res.string.mix_lab_eq_unavailable)
                } else {
                    ""
                },
                style = AppTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    color = if (eqAvailable) SpotifyTokens.Green else SpotifyTokens.Muted
                )
            )
        }
        Text(
            text = title,
            style = AppTheme.typography.titleSmall.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = artist,
            style = AppTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = SpotifyTokens.Muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${formatMs(positionMs)} / ${formatMs(durationMs)}",
            style = AppTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                color = SpotifyTokens.Muted
            )
        )
        SpotifyLabSlider(
            label = stringResource(Res.string.mix_lab_speed),
            valueLabel = "${"%.2f".format(speed)}x",
            value = ((speed - 0.9f) / 0.2f).coerceIn(0f, 1f),
            onValueChange = { onSpeedChange(0.9f + it.coerceIn(0f, 1f) * 0.2f) }
        )
        SpotifyLabSlider(
            label = stringResource(Res.string.mix_lab_bass),
            valueLabel = "${bass.toInt()} dB",
            value = ((bass + 12f) / 24f).coerceIn(0f, 1f),
            onValueChange = { onBassChange((it.coerceIn(0f, 1f) * 24f) - 12f) }
        )
    }
}

@Composable
private fun SpotifyLabSlider(
    label: String,
    valueLabel: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = AppTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    color = SpotifyTokens.Muted
                )
            )
            Text(
                text = valueLabel,
                style = AppTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    color = SpotifyTokens.Muted
                )
            )
        }
        SpotifySeekBar(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = {},
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun stateLabel(state: Int): String = when (state) {
    Player.STATE_IDLE -> stringResource(Res.string.mix_lab_state_idle)
    Player.STATE_BUFFERING -> stringResource(Res.string.mix_lab_state_buffering)
    Player.STATE_READY -> stringResource(Res.string.mix_lab_state_ready)
    Player.STATE_ENDED -> stringResource(Res.string.mix_lab_state_ended)
    else -> stringResource(Res.string.mix_lab_state_unknown)
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

private fun formatMs(value: Long): String {
    val totalSeconds = (value / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

private fun org.spotube.spotube.core.audioplayer.MediaItem.toMedia3Item(): androidx.media3.common.MediaItem {
    return androidx.media3.common.MediaItem.Builder()
        .setUri(url)
        .setMediaMetadata(
            androidx.media3.common.MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .build()
        )
        .build()
}
