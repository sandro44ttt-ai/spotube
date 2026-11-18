package org.spotube.spotube.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.composeunstyled.Text
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.spotube.spotube.core.automix.AutoMixPlaybackManager
import org.spotube.spotube.core.ui.component.spotify.SpotifyTokens
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.resources.iconsax.Iconsax
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.auto_mix_pause
import spotube.composeapp.generated.resources.auto_mix_play
import spotube.composeapp.generated.resources.auto_mix_title

@Composable
actual fun AutoMixMiniPlayer(modifier: Modifier) {
    val manager: AutoMixPlaybackManager = koinInject()
    val state by manager.state.collectAsStateWithLifecycle()
    if (!state.isActive || state.current == null) return
    val track = state.current ?: return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SpotifyTokens.Surface)
            .clickable { manager.togglePlayPause() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = track.artworkUrl(),
            contentDescription = track.title,
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(SpotifyTokens.Highlight),
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp)
        ) {
            Text(
                text = stringResource(Res.string.auto_mix_title).uppercase(),
                style = AppTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = SpotifyTokens.Green
                )
            )
            Text(
                text = track.title,
                style = AppTheme.typography.titleSmall.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artists.joinToString(", ") { it.name },
                style = AppTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    color = SpotifyTokens.Muted
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = if (state.isPlaying) "??" else "?",
            style = AppTheme.typography.titleLarge.copy(
                fontSize = 20.sp,
                color = Color.White
            ),
            modifier = Modifier
                .clickable { manager.togglePlayPause() }
                .padding(horizontal = 6.dp)
        )
    }
}

private fun org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack.artworkUrl(): String? {
    return (album?.thumbnails ?: thumbnails)
        ?.maxByOrNull { it.width * it.height }
        ?.url
}
