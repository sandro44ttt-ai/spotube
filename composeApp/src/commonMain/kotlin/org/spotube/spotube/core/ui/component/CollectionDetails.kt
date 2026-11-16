/*
 * Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.spotube.spotube.core.ui.component


import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.height
import org.spotube.millennium.composables.MillenniumButton
import org.spotube.millennium.composables.MillenniumButtonStyle
import org.spotube.millennium.composables.MillenniumCard
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.millennium.composables.MillenniumSeparator
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import com.composeunstyled.currentWindowWidthBreakpoint
import org.spotube.millennium.adaptive.WindowBreakpoints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

import org.spotube.spotube.core.ui.theming.AppTheme
import com.composeunstyled.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import org.spotube.spotube.core.ui.media.rememberDominantColor
import org.spotube.spotube.core.ui.misc.TextWithShimmer
import org.spotube.spotube.core.ui.misc.shimmerApply
import org.spotube.spotube.core.ui.component.spotify.SpotifyOutlineCircleIconButton
import org.spotube.spotube.core.ui.component.spotify.SpotifyPlayPillButton
import org.spotube.spotube.core.ui.component.spotify.SpotifyTokens
import org.spotube.spotube.core.ui.component.spotify.spotifyCollectionHeaderBrush
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxAddSquare
import org.spotube.spotube.resources.iconsax.IconsaxEdit
import org.spotube.spotube.resources.iconsax.IconsaxHeart
import org.spotube.spotube.resources.iconsax.IconsaxHeart2
import org.spotube.spotube.resources.iconsax.IconsaxPauseCircle
import org.spotube.spotube.resources.iconsax.IconsaxPlayCircle2
import org.spotube.spotube.resources.iconsax.IconsaxShuffle
import org.spotube.spotube.resources.iconsax.User
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import spotube.composeapp.generated.resources.*

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CollectionDetails(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    imageURL: String,
    imageResource: DrawableResource? = null,
    ownerName: String,
    ownerImageURL: String?,
    onOwnerClick: () -> Unit,
    onPlay: () -> Unit,
    onShufflePlay: () -> Unit,
    onAddToQueue: () -> Unit,
    isPlaying: Boolean = false,
    /** Guest in a jam session: play/shuffle are replaced by Add to Jam. */
    isJamGuest: Boolean = false,
    onAddToJam: (() -> Unit)? = null,
    isFollowing: Boolean = false,
    isFollowLoading: Boolean = false,
    isPlayLoading: Boolean = false,
    isShuffleLoading: Boolean = false,
    isAddToQueueLoading: Boolean = false,
    onFollowClick: () -> Unit = { },
    showFollowButton: Boolean = true,
    onEdit: (() -> Unit)? = null,
    statsText: String? = null,
    onDownload: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    sharedElementKey: String? = null) {
    val isCompact = currentWindowWidthBreakpoint() isBelow WindowBreakpoints.Medium
    val headerPainter = rememberAsyncImagePainter(model = imageURL)
    val dominantColor = rememberDominantColor(
        (headerPainter.state as? AsyncImagePainter.State.Success)?.result?.image
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (isCompact) {
                    spotifyCollectionHeaderBrush(dominantColor)
                } else {
                    Brush.verticalGradient(
                        listOf(Color(0xFF2A2A2A), Color(0xFF121212))
                    )
                }
            )
    ) {
        val sharedTransitionScope = LocalSharedTransitionScope.current
        val animatedVisibilityScope = LocalAnimatedVisibilityScope.current

        val playPauseButton = @Composable {
            if (isJamGuest) {
                if (onAddToJam != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MillenniumButton(
                            modifier = if (isCompact) Modifier.weight(1f) else Modifier,
                            onClick = onAddToJam!!) {
                            MillenniumIcon(
                                imageVector = Iconsax.IconsaxAddSquare,
                                contentDescription = stringResource(Res.string.track_options_add_to_jam))
                            TextWithShimmer(text = stringResource(Res.string.track_options_add_to_jam))
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val modifier = if (isCompact) {
                        Modifier.weight(1f)
                    } else {
                        Modifier
                    }

                    SpotifyPlayPillButton(
                        text = if (isPlaying) stringResource(Res.string.player_pause_content_description) else stringResource(Res.string.player_play_content_description),
                        isPlaying = isPlaying,
                        onClick = onPlay,
                        modifier = modifier
                    )
                    SpotifyOutlineCircleIconButton(
                        icon = Iconsax.SpotifyShuffle,
                        contentDescription = stringResource(Res.string.collection_details_shuffle_play),
                        onClick = onShufflePlay
                    )
                }
            }
        }

        val actions = @Composable {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MillenniumIconButton(
                    onClick = onAddToQueue,
                    loading = isAddToQueueLoading,
                    style = MillenniumIconButtonStyle.Quiet) {
                    MillenniumIcon(
                        imageVector = Iconsax.IconsaxAddSquare,
                        contentDescription = stringResource(Res.string.player_add_to_queue_content_description)
                    )
                }
                MillenniumSeparator(modifier = Modifier.height(24.dp), vertical = true)
                if (showFollowButton) {
                    if (isFollowing) {
                        MillenniumIconButton(
                            onClick = onFollowClick,
                            loading = isFollowLoading,
                            style = MillenniumIconButtonStyle.Quiet) {
                            MillenniumIcon(
                                imageVector = Iconsax.IconsaxHeart2,
                                contentDescription = stringResource(Res.string.collection_details_unfollow))
                        }
                    } else {
                        MillenniumIconButton(
                            onClick = onFollowClick,
                            loading = isFollowLoading,
                            style = MillenniumIconButtonStyle.Quiet) {
                            MillenniumIcon(
                                imageVector = Iconsax.IconsaxHeart,
                                contentDescription = stringResource(Res.string.collection_details_follow),
                                tint = AppTheme.colors.onSurfaceVariant)
                        }
                    }
                }
                if (onEdit != null) {
                    MillenniumSeparator(modifier = Modifier.height(24.dp), vertical = true)
                    MillenniumIconButton(
                        onClick = onEdit,
                        style = MillenniumIconButtonStyle.Quiet) {
                        MillenniumIcon(
                            imageVector = Iconsax.IconsaxEdit,
                            contentDescription = stringResource(Res.string.collection_details_edit_playlist))
                    }
                }
            }
        }
        val ownerInfo = @Composable {
            Row(
                modifier = Modifier.clickable(onClick = onOwnerClick),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (ownerImageURL != null) {
                    AsyncImage(
                        model = ownerImageURL,
                        contentDescription = ownerName,
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .shimmerApply(),
                        contentScale = ContentScale.Crop)
                } else {
                    MillenniumIcon(
                        imageVector = Iconsax.User,
                        contentDescription = ownerName,
                        modifier = Modifier
                            .size(16.dp)
                            .background(
                                AppTheme.colors.primaryContainer,
                                CircleShape
                            )
                            .padding(6.dp),
                        tint = AppTheme.colors.onPrimaryContainer)
                }
                TextWithShimmer(
                    text = stringResource(Res.string.collection_details_by_owner, ownerName),
                    style = AppTheme.typography.labelSmall,
                    color = AppTheme.colors.onSurfaceVariant)
            }
        }

        val artwork = @Composable {
            Box(
                modifier = Modifier
                    .size(if (isCompact) 192.dp else 200.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .sharedElementOrNone(
                        key = sharedElementKey,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope)
                    .shimmerApply(),
                contentAlignment = Alignment.Center) {
                if (imageURL.isNotBlank()) {
                    AsyncImage(
                        model = imageURL,
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop)
                } else if (imageResource != null) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(imageResource),
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop)
                } else {
                    Text(
                        text = title.take(1).ifBlank { "?" }.uppercase(),
                        style = AppTheme.typography.displaySmall,
                        color = AppTheme.colors.onSurfaceVariant)
                }
            }
        }

        val metadata = @Composable {
            Column {
                TextWithShimmer(
                    text = title,
                    style = if (isCompact) {
                        AppTheme.typography.headlineSmall.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    } else {
                        AppTheme.typography.headlineLarge
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis)

                if (description.isNotBlank()) {
                    TextWithShimmer(
                        text = description,
                        style = if (isCompact) {
                            AppTheme.typography.bodyMedium.copy(fontSize = 13.sp)
                        } else {
                            AppTheme.typography.bodyMedium
                        },
                        color = AppTheme.colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)) {
            if (isCompact) {
                // Phone layout: full-bleed artwork with the title over it, then the
                // description / owner / actions block, exactly like the Spotify app.
                Column(Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp),
                        contentAlignment = Alignment.BottomStart) {
                        if (imageURL.isNotBlank()) {
                            androidx.compose.foundation.Image(
                                painter = headerPainter,
                                contentDescription = title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop)
                        } else if (imageResource != null) {
                            androidx.compose.foundation.Image(
                                painter = painterResource(imageResource),
                                contentDescription = title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop)
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF2A2A2A)))
                        }
                        // Scrim keeps the oversized title legible over any artwork
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        0.45f to Color.Transparent,
                                        1f to Color(0xFF121212))))
                        Text(
                            text = title,
                            style = AppTheme.typography.headlineLarge.copy(
                                fontSize = 40.sp,
                                lineHeight = 44.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(
                                start = 16.dp,
                                end = 16.dp,
                                bottom = 12.dp))
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (description.isNotBlank()) {
                            Text(
                                text = description,
                                style = AppTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                color = SpotifyTokens.Muted,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis)
                        }
                        if (ownerName.trim().equals("Spotify", ignoreCase = true)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                MillenniumIcon(
                                    imageVector = Iconsax.SpotifyLogo,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = SpotifyTokens.Green)
                                Text(
                                    text = stringResource(Res.string.collection_details_spotify_verified),
                                    style = AppTheme.typography.bodyMedium.copy(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold),
                                    color = SpotifyTokens.Green,
                                    maxLines = 1)
                            }
                        } else {
                            Text(
                                text = ownerName,
                                style = AppTheme.typography.bodyMedium.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold),
                                color = Color.White,
                                maxLines = 1)
                        }
                        if (!statsText.isNullOrBlank()) {
                            Text(
                                text = statsText,
                                style = AppTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = SpotifyTokens.Muted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .border(
                                        1.dp,
                                        Color.White.copy(alpha = 0.22f),
                                        RoundedCornerShape(4.dp))
                                    .clickable(onClick = onPlay),
                                contentAlignment = Alignment.Center) {
                                if (imageURL.isNotBlank()) {
                                    androidx.compose.foundation.Image(
                                        painter = headerPainter,
                                        contentDescription = title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop)
                                } else if (imageResource != null) {
                                    androidx.compose.foundation.Image(
                                        painter = painterResource(imageResource),
                                        contentDescription = title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop)
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            if (showFollowButton) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .then(
                                            if (isFollowing) {
                                                Modifier.background(SpotifyTokens.Green)
                                            } else {
                                                Modifier.border(1.dp, SpotifyTokens.Outline, CircleShape)
                                            }
                                        )
                                        .clickable(onClick = onFollowClick),
                                    contentAlignment = Alignment.Center) {
                                    MillenniumIcon(
                                        imageVector = if (isFollowing) Iconsax.SpotifyCheck else Iconsax.SpotifyPlus,
                                        contentDescription = stringResource(
                                            if (isFollowing) {
                                                Res.string.collection_details_unfollow
                                            } else {
                                                Res.string.collection_details_follow
                                            }),
                                        modifier = Modifier.size(22.dp),
                                        tint = if (isFollowing) SpotifyTokens.Base else Color.White)
                                }
                                Spacer(Modifier.width(10.dp))
                            }
                            if (onDownload != null) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, SpotifyTokens.Outline, CircleShape)
                                        .clickable(onClick = onDownload),
                                    contentAlignment = Alignment.Center) {
                                    MillenniumIcon(
                                        imageVector = Iconsax.SpotifyDownload,
                                        contentDescription = stringResource(Res.string.player_download),
                                        modifier = Modifier.size(22.dp),
                                        tint = Color.White)
                                }
                                Spacer(Modifier.width(10.dp))
                            }
                            if (onShare != null) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .clickable(onClick = onShare),
                                    contentAlignment = Alignment.Center) {
                                    MillenniumIcon(
                                        imageVector = Iconsax.SpotifyShareGlyph,
                                        contentDescription = stringResource(Res.string.track_options_share),
                                        modifier = Modifier.size(22.dp),
                                        tint = Color.White)
                                }
                            }
                            Spacer(Modifier.weight(1f))
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .clickable(onClick = onShufflePlay),
                                contentAlignment = Alignment.Center) {
                                MillenniumIcon(
                                    imageVector = Iconsax.SpotifyShuffle,
                                    contentDescription = stringResource(Res.string.collection_details_shuffle_play),
                                    modifier = Modifier.size(24.dp),
                                    tint = SpotifyTokens.Green)
                            }
                            Spacer(Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(SpotifyTokens.Green)
                                    .clickable(onClick = onPlay),
                                contentAlignment = Alignment.Center) {
                                MillenniumIcon(
                                    imageVector = if (isPlaying) Iconsax.SpotifyPause else Iconsax.SpotifyPlay,
                                    contentDescription = stringResource(
                                        if (isPlaying) {
                                            Res.string.player_pause_content_description
                                        } else {
                                            Res.string.player_play_content_description
                                        }),
                                    modifier = Modifier.size(24.dp),
                                    tint = Color.Black)
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top) {
                    artwork()

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        metadata()
                        ownerInfo()
                        actions()
                        playPauseButton()
                    }
                }
            }
        }
    }
}

@Composable
@androidx.compose.ui.tooling.preview.Preview
fun CollectionDetailsPreview() {
    CollectionDetails(
        title = "My Playlist",
        description = "A collection of my favorite songs.",
        imageURL = "https://example.com/playlist.jpg",
        ownerName = "John Doe",
        ownerImageURL = "https://example.com/john.jpg",
        onOwnerClick = {},
        onPlay = {},
        onShufflePlay = {},
        onAddToQueue = {})
}
