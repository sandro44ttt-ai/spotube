package org.spotube.spotube.core.ui.component

import android.content.Context
import android.media.AudioManager
import android.media.audiofx.Equalizer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.media3.exoplayer.ExoPlayer
import com.composeunstyled.Text
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.spotube.millennium.composables.MillenniumButton
import org.spotube.millennium.composables.MillenniumButtonStyle
import org.spotube.millennium.composables.MillenniumSlider
import org.spotube.spotube.core.audioplayer.AudioPlayerInterface
import org.spotube.spotube.core.ui.component.spotify.SpotifyTokens
import org.spotube.spotube.core.ui.theming.AppTheme
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.mix_lab_android_only
import spotube.composeapp.generated.resources.mix_lab_bass
import spotube.composeapp.generated.resources.mix_lab_close
import spotube.composeapp.generated.resources.mix_lab_crossfader
import spotube.composeapp.generated.resources.mix_lab_deck_a
import spotube.composeapp.generated.resources.mix_lab_deck_b
import spotube.composeapp.generated.resources.mix_lab_drift
import spotube.composeapp.generated.resources.mix_lab_no_tracks
import spotube.composeapp.generated.resources.mix_lab_pause
import spotube.composeapp.generated.resources.mix_lab_play
import spotube.composeapp.generated.resources.mix_lab_speed
import spotube.composeapp.generated.resources.mix_lab_start_synced
import spotube.composeapp.generated.resources.mix_lab_title
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

    val audioManager = remember {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    val sessionA = remember { audioManager.generateAudioSessionId() }
    val sessionB = remember { audioManager.generateAudioSessionId() }
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
            .also { runCatching { it.setAudioSessionId(sessionA) } }
    }
    val deckB = remember {
        ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, false)
            .build()
            .also { runCatching { it.setAudioSessionId(sessionB) } }
    }

    var isPlaying by remember { mutableStateOf(false) }
    var crossfade by remember { mutableFloatStateOf(0.5f) }
    var speedA by remember { mutableFloatStateOf(1f) }
    var speedB by remember { mutableFloatStateOf(1f) }
    var bassA by remember { mutableFloatStateOf(0f) }
    var bassB by remember { mutableFloatStateOf(0f) }
    var positionA by remember { mutableStateOf(0L) }
    var positionB by remember { mutableStateOf(0L) }
    var durationA by remember { mutableStateOf(0L) }
    var durationB by remember { mutableStateOf(0L) }
    var eqA by remember { mutableStateOf<Equalizer?>(null) }
    var eqB by remember { mutableStateOf<Equalizer?>(null) }

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
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            if (eqA == null) {
                eqA = runCatching {
                    Equalizer(0, sessionA).apply { enabled = true }
                }.getOrNull()
            }
            if (eqB == null) {
                eqB = runCatching {
                    Equalizer(0, sessionB).apply { enabled = true }
                }.getOrNull()
            }
        }
    }
    LaunchedEffect(eqA, bassA) {
        runCatching { eqA?.setBandLevel(0.toShort(), (bassA * 100f).toInt().toShort()) }
    }
    LaunchedEffect(eqB, bassB) {
        runCatching { eqB?.setBandLevel(0.toShort(), (bassB * 100f).toInt().toShort()) }
    }
    LaunchedEffect(visible) {
        while (visible) {
            positionA = deckA.currentPosition
            positionB = deckB.currentPosition
            durationA = deckA.duration.coerceAtLeast(0L)
            durationB = deckB.duration.coerceAtLeast(0L)
            delay(200)
        }
    }

    DisposableEffect(deckA) {
        onDispose { deckA.release() }
    }
    DisposableEffect(deckB) {
        onDispose { deckB.release() }
    }
    DisposableEffect(eqA) {
        onDispose { runCatching { eqA?.release() } }
    }
    DisposableEffect(eqB) {
        onDispose { runCatching { eqB?.release() } }
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
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(Res.string.mix_lab_title),
                        style = AppTheme.typography.headlineSmall.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = androidx.compose.ui.graphics.Color.White
                        )
                    )
                    Text(
                        text = stringResource(Res.string.mix_lab_close),
                        style = AppTheme.typography.labelLarge.copy(
                            color = SpotifyTokens.Muted
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SpotifyTokens.Surface)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .clickable {
                                pauseBoth()
                                onDismiss()
                            }
                    )
                }

                if (itemA == null || itemB == null) {
                    Text(
                        text = stringResource(Res.string.mix_lab_no_tracks),
                        style = AppTheme.typography.bodyMedium,
                        color = SpotifyTokens.Muted
                    )
                } else {
                    MixDeckPanel(
                        label = stringResource(Res.string.mix_lab_deck_a),
                        title = itemA.title,
                        artist = itemA.artist,
                        positionMs = positionA,
                        durationMs = durationA,
                        speed = speedA,
                        bass = bassA,
                        onSpeedChange = { speedA = it },
                        onBassChange = { bassA = it }
                    )
                    MixDeckPanel(
                        label = stringResource(Res.string.mix_lab_deck_b),
                        title = itemB.title,
                        artist = itemB.artist,
                        positionMs = positionB,
                        durationMs = durationB,
                        speed = speedB,
                        bass = bassB,
                        onSpeedChange = { speedB = it },
                        onBassChange = { bassB = it }
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(Res.string.mix_lab_crossfader),
                            style = AppTheme.typography.labelLarge,
                            color = SpotifyTokens.Muted
                        )
                        MillenniumSlider(
                            value = crossfade,
                            onValueChange = { crossfade = it },
                            label = stringResource(Res.string.mix_lab_crossfader),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MillenniumButton(
                            onClick = ::playBoth,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(Res.string.mix_lab_play))
                        }
                        MillenniumButton(
                            onClick = ::pauseBoth,
                            style = MillenniumButtonStyle.Secondary,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(Res.string.mix_lab_pause))
                        }
                    }
                    MillenniumButton(
                        onClick = {
                            deckA.seekTo(0)
                            deckB.seekTo(0)
                            playBoth()
                        },
                        style = MillenniumButtonStyle.Secondary,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(Res.string.mix_lab_start_synced))
                    }

                    Text(
                        text = stringResource(
                            Res.string.mix_lab_drift,
                            (positionA - positionB).toInt()
                        ),
                        style = AppTheme.typography.bodySmall,
                        color = SpotifyTokens.Muted
                    )
                }
            }
        }
    }
}

@Composable
private fun MixDeckPanel(
    label: String,
    title: String,
    artist: String,
    positionMs: Long,
    durationMs: Long,
    speed: Float,
    bass: Float,
    onSpeedChange: (Float) -> Unit,
    onBassChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SpotifyTokens.Surface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = AppTheme.typography.labelSmall.copy(
                color = SpotifyTokens.Muted,
                fontSize = 11.sp
            )
        )
        Text(
            text = title,
            style = AppTheme.typography.titleSmall.copy(
                color = androidx.compose.ui.graphics.Color.White,
                fontWeight = FontWeight.SemiBold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = artist,
            style = AppTheme.typography.bodySmall,
            color = SpotifyTokens.Muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${formatMs(positionMs)} / ${formatMs(durationMs)}",
            style = AppTheme.typography.labelSmall,
            color = SpotifyTokens.Muted
        )
        Text(
            text = "${stringResource(Res.string.mix_lab_speed)} ${"%.2f".format(speed)}x",
            style = AppTheme.typography.labelSmall,
            color = SpotifyTokens.Muted
        )
        MillenniumSlider(
            value = (speed - 0.9f) / 0.2f,
            onValueChange = { onSpeedChange(0.9f + it.coerceIn(0f, 1f) * 0.2f) },
            label = stringResource(Res.string.mix_lab_speed),
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "${stringResource(Res.string.mix_lab_bass)} ${bass.toInt()} dB",
            style = AppTheme.typography.labelSmall,
            color = SpotifyTokens.Muted
        )
        MillenniumSlider(
            value = (bass + 12f) / 24f,
            onValueChange = { onBassChange((it.coerceIn(0f, 1f) * 24f) - 12f) },
            label = stringResource(Res.string.mix_lab_bass),
            modifier = Modifier.fillMaxWidth()
        )
    }
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
