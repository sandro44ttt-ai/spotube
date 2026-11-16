/* Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.spotube.spotube.core.ui.component.cards

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composeunstyled.LocalTextStyle
import coil3.compose.AsyncImage
import org.spotube.spotube.core.ui.component.LocalAnimatedVisibilityScope
import org.spotube.spotube.core.ui.component.LocalSharedTransitionScope
import org.spotube.spotube.core.ui.component.sharedElementOrNone
import org.spotube.spotube.core.ui.component.spotify.SpotifyIconButton
import org.spotube.spotube.core.ui.component.spotify.SpotifyPlayButton
import org.spotube.spotube.core.ui.component.spotify.SpotifyTokens
import org.spotube.spotube.core.ui.misc.TextWithShimmer
import org.spotube.spotube.core.ui.misc.shimmerApply
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxAddSquare
import org.spotube.spotube.resources.iconsax.IconsaxMusic
import org.spotube.spotube.resources.iconsax.IconsaxPause
import org.spotube.spotube.resources.iconsax.IconsaxPlay
import org.jetbrains.compose.resources.stringResource
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.millennium.composables.MillenniumListTile
import org.spotube.millennium.theme.MillenniumTheme
import spotube.composeapp.generated.resources.*

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PlayableListTile(
    title: String,
    subtitle: String? = null,
    imageURL: String? = null,
    isPlaying: Boolean = false,
    onClick: (() -> Unit)? = null,
    onPlay: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    sharedElementKey: String? = null,
    playLoading: Boolean = false,
    addToQueueLoading: Boolean = false,
    imageModel: Any? = imageURL,
    imageShape: Shape = RoundedCornerShape(4.dp),
    placeholderIcon: ImageVector = Iconsax.IconsaxMusic,
    artwork: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    isBlacklisted: Boolean = false,
    compact: Boolean = false
) {
    val titleColor = if (isPlaying) SpotifyTokens.Green else Color.White
    val subtitleColor = if (isPlaying) SpotifyTokens.Green else SpotifyTokens.Muted

    MillenniumListTile(
        modifier = modifier,
        onClick = onClick ?: onPlay,
        selected = false,
        background = Brush.solid(Color.Transparent),
        shape = RoundedCornerShape(0.dp),
        borderWidth = 0.dp,
        borderColor = Color.Transparent,
        contentPadding = PaddingValues(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        minimumHeight = 64.dp,
        leading = {
            MediaArtwork(
                imageModel, title,
                Modifier.size(48.dp).sharedElementOrNone(
                    sharedElementKey,
                    LocalSharedTransitionScope.current,
                    LocalAnimatedVisibilityScope.current
                ),
                RoundedCornerShape(4.dp), placeholderIcon, artwork
            )
        },
        title = {
            TextWithShimmer(
                title,
                style = AppTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        subtitle = subtitle?.let { text ->
            {
                TextWithShimmer(
                    text,
                    style = AppTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        color = subtitleColor
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        trailing = if (onPlay != null || onAddToQueue != null || trailingContent != null) {
            {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    trailingContent?.invoke()
                    if (onAddToQueue != null && !isPlaying) {
                        SpotifyIconButton(
                            icon = Iconsax.IconsaxAddSquare,
                            contentDescription = stringResource(Res.string.player_add_to_queue_content_description),
                            onClick = onAddToQueue,
                            modifier = Modifier.size(48.dp),
                            tint = SpotifyTokens.Muted
                        )
                    }
                    if (onPlay != null) {
                        SpotifyPlayButton(
                            isPlaying = isPlaying,
                            onClick = onPlay,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
        } else null
    )
}

@Composable
internal fun MediaArtwork(
    model: Any?,
    title: String,
    modifier: Modifier,
    shape: Shape = RoundedCornerShape(4.dp),
    placeholderIcon: ImageVector = Iconsax.IconsaxMusic,
    artwork: (@Composable () -> Unit)? = null
) {
    Box(
        modifier
            .clip(shape)
            .background(SpotifyTokens.Surface)
            .shimmerApply(),
        contentAlignment = Alignment.Center
    ) {
        if (artwork != null) artwork()
        else if (model != null) AsyncImage(
            model,
            title,
            Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        else MillenniumIcon(
            placeholderIcon,
            null,
            modifier = Modifier.size(24.dp),
            tint = SpotifyTokens.Muted
        )
    }
}
