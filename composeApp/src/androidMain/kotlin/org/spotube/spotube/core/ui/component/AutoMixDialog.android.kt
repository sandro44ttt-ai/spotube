package org.spotube.spotube.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.composeunstyled.Text
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import org.spotube.spotube.core.automix.AutoMixController
import org.spotube.spotube.core.automix.AutoMixPlaybackManager
import org.spotube.spotube.core.ui.component.spotify.SpotifyCircleTopBarButton
import org.spotube.spotube.core.ui.component.spotify.SpotifyTokens
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.resources.iconsax.Iconsax
import kotlin.math.roundToInt
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.auto_mix_analyzing
import spotube.composeapp.generated.resources.auto_mix_close
import spotube.composeapp.generated.resources.auto_mix_minimize
import spotube.composeapp.generated.resources.auto_mix_stop
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

@Composable
actual fun AutoMixDialog(
    visible: Boolean,
    title: String,
    tracks: List<MetadataTrack>,
    onDismiss: () -> Unit
) {
    if (!visible) return

    val manager: AutoMixPlaybackManager = koinInject()
    val state by manager.state.collectAsStateWithLifecycle()

    LaunchedEffect(visible, tracks) {
        if (tracks.isNotEmpty()) {
            manager.start(title, tracks)
        }
    }

    val styleLabel = when (state.transitionStyle) {
        AutoMixPlaybackManager.TransitionStyle.Cut ->
            stringResource(Res.string.auto_mix_style_cut)

        AutoMixPlaybackManager.TransitionStyle.Blend ->
            stringResource(Res.string.auto_mix_style_blend)

        AutoMixPlaybackManager.TransitionStyle.Mashup ->
            stringResource(Res.string.auto_mix_style_mashup)
    }

    Dialog(
        onDismissRequest = onDismiss,
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
                        contentDescription = stringResource(Res.string.auto_mix_minimize),
                        onClick = onDismiss
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
                            text = state.title.ifBlank { title },
                            style = AppTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                color = SpotifyTokens.Muted
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = stringResource(Res.string.auto_mix_stop),
                        style = AppTheme.typography.labelMedium.copy(
                            fontSize = 11.sp,
                            color = SpotifyTokens.Muted
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                manager.stop()
                                AutoMixController.clear()
                                onDismiss()
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    state.current?.let { track ->
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

                Row(verticalAlignment = Alignment.CenterVertically) {
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
                    AutoMixLevelBadge(
                        level = state.planLevel,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = state.current?.title.orEmpty(),
                    style = AppTheme.typography.headlineSmall.copy(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = state.current?.artists?.joinToString(", ") { it.name }.orEmpty(),
                    style = AppTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = SpotifyTokens.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val featureLine = listOfNotNull(
                    state.currentBpm?.roundToInt()?.let { "$it BPM" },
                    state.currentKey.takeIf { it.isNotBlank() }
                ).joinToString(" · ")
                if (featureLine.isNotBlank()) {
                    Text(
                        text = featureLine,
                        style = AppTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SpotifyTokens.Green
                        )
                    )
                } else if (state.isAnalyzing) {
                    Text(
                        text = stringResource(Res.string.auto_mix_analyzing),
                        style = AppTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            color = SpotifyTokens.Muted
                        )
                    )
                }
                if (state.lastError.isNotBlank()) {
                    Text(
                        text = state.lastError,
                        style = AppTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            color = Color(0xFFFF6B6B)
                        ),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (state.debugInfo.isNotBlank()) {
                    Text(
                        text = state.debugInfo,
                        style = AppTheme.typography.labelSmall.copy(
                            fontSize = 8.sp,
                            color = SpotifyTokens.Muted.copy(alpha = 0.7f)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (state.planReason.isNotBlank()) {
                    Text(
                        text = state.planReason,
                        style = AppTheme.typography.labelSmall.copy(
                            fontSize = 8.sp,
                            color = SpotifyTokens.Muted.copy(alpha = 0.6f)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SpotifyCircleTopBarButton(
                        icon = Iconsax.SpotifySkipBackGlyph,
                        contentDescription = stringResource(Res.string.auto_mix_previous),
                        onClick = manager::skipPrevious
                    )
                    SpotifyCircleTopBarButton(
                        icon = if (state.isPlaying) Iconsax.SpotifyPause else Iconsax.SpotifyPlay,
                        contentDescription = if (state.isPlaying) {
                            stringResource(Res.string.auto_mix_pause)
                        } else {
                            stringResource(Res.string.auto_mix_play)
                        },
                        onClick = manager::togglePlayPause
                    )
                    SpotifyCircleTopBarButton(
                        icon = Iconsax.SpotifySkipNextGlyph,
                        contentDescription = stringResource(Res.string.auto_mix_next_track),
                        onClick = manager::skipNext
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                val progress = if (state.isTransitioning) {
                    state.transitionProgress
                } else if (state.durationMs > 0L) {
                    (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
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
                        text = formatAutoMixTime(state.positionMs),
                        style = AppTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            color = SpotifyTokens.Muted
                        )
                    )
                    if (state.isTransitioning) {
                        Text(
                            text = stringResource(Res.string.auto_mix_transitioning) + " · " + styleLabel,
                            style = AppTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                color = SpotifyTokens.Green
                            )
                        )
                    }
                    Text(
                        text = formatAutoMixTime(state.durationMs),
                        style = AppTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            color = SpotifyTokens.Muted
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                state.next?.let { next ->
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
                            val nextFeatureLine = listOfNotNull(
                                state.nextBpm?.roundToInt()?.let { "$it BPM" },
                                state.nextKey.takeIf { it.isNotBlank() }
                            ).joinToString(" · ")
                            if (nextFeatureLine.isNotBlank()) {
                                Text(
                                    text = nextFeatureLine,
                                    style = AppTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        color = SpotifyTokens.Muted
                                    )
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

private fun MetadataTrack.artworkUrl(): String? {
    return (album?.thumbnails ?: thumbnails)
        ?.maxByOrNull { it.width * it.height }
        ?.url
}

private fun formatAutoMixTime(value: Long): String {
    val totalSeconds = (value / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
