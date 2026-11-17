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


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.composeunstyled.Text
import com.composeunstyled.currentWindowWidthBreakpoint
import org.spotube.millennium.adaptive.WindowBreakpoints
import org.spotube.millennium.composables.MillenniumDropdownMenu
import org.spotube.millennium.composables.MillenniumDropdownMenuItem
import org.koin.compose.koinInject
import org.spotube.spotube.core.navigation.NavigationCommands
import org.spotube.spotube.core.share.ShareService
import org.spotube.spotube.core.ui.component.spotify.SpotifyCircleTopBarButton
import org.spotube.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbum
import org.spotube.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtist
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import org.spotube.spotube.core.ui.misc.TextWithShimmer
import org.spotube.spotube.core.ui.misc.shimmerApply
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxAddSquare
import org.spotube.spotube.resources.iconsax.IconsaxPauseCircle
import org.spotube.spotube.resources.iconsax.IconsaxPlayCircle2
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.jetbrains.compose.resources.stringResource
import spotube.composeapp.generated.resources.*

@Composable
fun CollectionView(
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
    isFollowing: Boolean = false,
    isFollowLoading: Boolean = false,
    isPlayLoading: Boolean = false,
    isShuffleLoading: Boolean = false,
    isAddToQueueLoading: Boolean = false,
    onFollowClick: () -> Unit = {},
    showFollowButton: Boolean = true,
    onEdit: (() -> Unit)? = null,
    sharedElementKey: String? = null,
    isLoading: Boolean = false,
    error: String? = null,
    onRetry: (() -> Unit)? = null,
    tracks: List<MetadataTrack> = emptyList(),
    hasMore: Boolean = false,
    isLoadingNextPage: Boolean = false,
    currentTrackId: String? = null,
    isCurrentTrackPlaying: Boolean = false,
    onTrackClick: (MetadataTrack) -> Unit = {},
    onLoadNextPage: () -> Unit = {},
    onTrackOptionsAction: (MetadataTrack, TrackOptionsAction) -> Unit = { _, _ -> },
    onArtistClick: (MetadataArtist.Basic) -> Unit = {},
    onAlbumClick: (MetadataAlbum.Detailed) -> Unit = {},
    onArtistsOverflowClick: (MetadataTrack) -> Unit = {},
    onBulkDownload: (List<MetadataTrack>) -> Unit = {},
    onBulkAddToQueue: (List<MetadataTrack>) -> Unit = {},
    onBulkPlayNext: (List<MetadataTrack>) -> Unit = {},
    onBulkAddToPlaylist: (List<MetadataTrack>) -> Unit = {},
    onBulkAddToJam: (List<MetadataTrack>) -> Unit = {},
    isInJam: Boolean = false,
    isJamGuest: Boolean = false,
    onAddToJam: (List<MetadataTrack>) -> Unit = {},
    trackOptionsState: (MetadataTrack) -> TrackOptionsState = { TrackOptionsState() },
    trackCount: Int = 0,
    totalDurationMs: Long = 0L,
    shareUrl: String? = null,
    onAutoMix: (() -> Unit)? = null,
    footerContent: (@Composable () -> Unit)? = null,
    trailingContent: @Composable () -> Unit = {}) {
    val listState = rememberLazyListState()
    val navigationCommands: NavigationCommands = koinInject()
    val shareService: ShareService = koinInject()
    var showCollectionMenu by remember { mutableStateOf(false) }
    val isCollapsed by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 ||
                listState.firstVisibleItemScrollOffset > 400
        }
    }
    val isCompact = currentWindowWidthBreakpoint() isBelow WindowBreakpoints.Medium
    val durationText = when {
        totalDurationMs <= 0L -> null
        else -> {
            val totalMinutes = (totalDurationMs / 60_000L).toInt()
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            if (hours > 0) {
                stringResource(
                    Res.string.collection_details_duration_hours_minutes,
                    hours,
                    minutes)
            } else {
                stringResource(
                    Res.string.collection_details_duration_minutes,
                    minutes.coerceAtLeast(1))
            }
        }
    }
    val statsText = run {
        val countText = if (trackCount > 0) {
            pluralStringResource(
                Res.plurals.collection_details_track_count,
                trackCount,
                trackCount)
        } else {
            null
        }
        when {
            countText != null && durationText != null ->
                stringResource(Res.string.collection_details_stats_join, countText, durationText)
            else -> countText ?: durationText
        }
    }
    val shareTarget = shareUrl

    val details: @Composable (Boolean) -> Unit = { skeleton ->
        CollectionDetails(
            title = if (skeleton) title.ifBlank { stringResource(Res.string.common_loading) } else title,
            description = description,
            imageURL = imageURL,
            imageResource = imageResource,
            ownerName = if (skeleton) ownerName.ifBlank { stringResource(Res.string.common_unknown) } else ownerName,
            ownerImageURL = ownerImageURL,
            onOwnerClick = onOwnerClick,
            onPlay = onPlay,
            onShufflePlay = onShufflePlay,
            onAddToQueue = onAddToQueue,
            isPlaying = isPlaying,
            isFollowing = isFollowing,
            isFollowLoading = isFollowLoading,
            isPlayLoading = isPlayLoading,
            isShuffleLoading = isShuffleLoading,
            isAddToQueueLoading = isAddToQueueLoading,
            onFollowClick = onFollowClick,
            showFollowButton = showFollowButton,
            onEdit = onEdit,
            statsText = if (skeleton) null else statsText,
            onDownload = if (!skeleton && tracks.isNotEmpty()) {
                { onBulkDownload(tracks) }
            } else {
                null
            },
            onShare = if (!skeleton && shareTarget != null) {
                { shareService.share(shareTarget, title) }
            } else {
                null
            },
            onAutoMix = if (!skeleton) onAutoMix else null,
            sharedElementKey = sharedElementKey,
            isJamGuest = isJamGuest,
            onAddToJam = { onAddToJam(tracks) })
    }

    val collectionBar: @Composable () -> Unit = {
        ApplicationMainBar(
            backButton = true,
            transparent = isCompact && !isCollapsed,
            title = if (isCollapsed) {
                {
                    CollapsedCollectionTitle(
                        title = title,
                        imageURL = imageURL,
                        imageResource = imageResource)
                }
            } else {
                {}
            },
            actions = if (isCollapsed && !isJamGuest) {
                {
                    MillenniumIconButton(
                        onClick = onPlay,
                        loading = isPlayLoading,
                        style = MillenniumIconButtonStyle.Quiet) {
                        MillenniumIcon(
                            imageVector = if (isPlaying) Iconsax.IconsaxPauseCircle else Iconsax.IconsaxPlayCircle2,
                            contentDescription = if (isPlaying) stringResource(Res.string.player_pause_content_description) else stringResource(Res.string.player_play_content_description))
                    }
                }
            } else {
                {}
            })
    }

    val listContent: @Composable () -> Unit = {
        if (error != null && tracks.isEmpty() && !isLoading) {
            ErrorDisplay(
                errorMessage = error,
                onRetry = onRetry ?: {},
                modifier = Modifier.fillMaxSize())
        } else {
            TrackList(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                headerContent = {
                    if (isLoading && tracks.isEmpty()) {
                        org.spotube.spotube.core.ui.misc.SkeletonTree(true) {
                            details(true)
                        }
                    } else {
                        details(false)
                    }
                },
                footerContent = footerContent,
                tracks = tracks,
                error = error,
                hasMore = hasMore,
                isLoading = isLoading,
                isLoadingNextPage = isLoadingNextPage,
                currentTrackId = currentTrackId,
                isCurrentTrackPlaying = isCurrentTrackPlaying,
                onTrackClick = onTrackClick,
                onLoadNextPage = onLoadNextPage,
                onTrackOptionsAction = onTrackOptionsAction,
                onArtistClick = onArtistClick,
                onAlbumClick = onAlbumClick,
                onArtistsOverflowClick = onArtistsOverflowClick,
                onBulkDownload = onBulkDownload,
                onBulkAddToQueue = onBulkAddToQueue,
                onBulkPlayNext = onBulkPlayNext,
                onBulkAddToPlaylist = onBulkAddToPlaylist,
                onBulkAddToJam = onBulkAddToJam,
                isInJam = isInJam,
                isJamGuest = isJamGuest,
                simplified = isCompact,
                flat = isCompact,
                trackOptionsState = trackOptionsState)
        }
    }

    if (isCompact) {
        Box(modifier = Modifier.fillMaxSize()) {
            listContent()
            if (isCollapsed) {
                collectionBar()
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    SpotifyCircleTopBarButton(
                        icon = Iconsax.SpotifyArrowLeft,
                        contentDescription = stringResource(Res.string.common_back),
                        onClick = { navigationCommands.pop() })
                    MillenniumDropdownMenu(
                        expanded = showCollectionMenu,
                        onExpandedChange = { showCollectionMenu = it },
                        anchor = {
                            SpotifyCircleTopBarButton(
                                icon = Iconsax.SpotifyMoreVertical,
                                contentDescription = stringResource(Res.string.player_more_options_content_description),
                                onClick = { showCollectionMenu = true })
                        }) {
                        MillenniumDropdownMenuItem(
                            label = stringResource(Res.string.player_add_to_queue_content_description),
                            icon = Iconsax.SpotifyQueueGlyph,
                            onClick = {
                                showCollectionMenu = false
                                onAddToQueue()
                            })
                        if (onAutoMix != null) {
                            MillenniumDropdownMenuItem(
                                label = stringResource(Res.string.auto_mix_title),
                                icon = Iconsax.SpotifyPlay,
                                onClick = {
                                    showCollectionMenu = false
                                    onAutoMix()
                                })
                        }
                        if (shareUrl != null) {
                            MillenniumDropdownMenuItem(
                                label = stringResource(Res.string.track_options_share),
                                icon = Iconsax.SpotifyShareGlyph,
                                onClick = {
                                    showCollectionMenu = false
                                    shareService.share(shareUrl, title)
                                })
                        }
                    }
                }
            }
            trailingContent()
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            collectionBar()
            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                listContent()
                trailingContent()
            }
        }
    }
}

@Composable
private fun CollapsedCollectionTitle(
    title: String,
    imageURL: String,
    imageResource: DrawableResource? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(6.dp))
                .shimmerApply(),
            contentAlignment = Alignment.Center) {
            if (imageURL.isNotBlank()) {
                AsyncImage(
                    model = imageURL,
                    contentDescription = title,
                    modifier = Modifier.size(36.dp),
                    contentScale = ContentScale.Crop)
            } else if (imageResource != null) {
                androidx.compose.foundation.Image(
                    painter = painterResource(imageResource),
                    contentDescription = title,
                    modifier = Modifier.size(36.dp),
                    contentScale = ContentScale.Crop)
            } else {
                Text(
                    text = title.take(1).ifBlank { "?" }.uppercase(),
                    style = AppTheme.typography.labelMedium,
                    color = AppTheme.colors.onSurfaceVariant)
            }
        }
        TextWithShimmer(
            text = title,
            style = AppTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 10.dp))
    }
}
