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

package org.spotube.spotube.modules.home


import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.millennium.theme.MillenniumTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState

import org.spotube.spotube.core.ui.theming.AppTheme
import com.composeunstyled.Text
import org.spotube.millennium.composables.MillenniumPullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composeunstyled.currentWindowWidthBreakpoint
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import org.spotube.plugin_interfaces.plugin_apis.metadata.browse.MetadataBrowseGenre
import org.spotube.plugin_interfaces.plugin_apis.metadata.browse.MetadataBrowseItem
import org.spotube.plugin_interfaces.plugin_apis.metadata.browse.MetadataBrowseSection
import org.spotube.spotube.PlatformType
import org.spotube.spotube.core.ui.component.AlbumCard
import org.spotube.spotube.core.ui.component.ApplicationMainBar
import org.spotube.spotube.core.ui.component.ArtistCard
import org.spotube.spotube.core.ui.component.ErrorDisplay
import org.spotube.spotube.core.ui.component.PlaylistCard
import org.spotube.spotube.core.ui.component.TrackCard
import org.spotube.spotube.core.ui.component.UserCard
import org.spotube.spotube.core.ui.component.FeaturedCarousel
import org.spotube.spotube.core.ui.component.VerticalScrollbar
import org.spotube.spotube.core.ui.component.cards.PlayableCard
import org.spotube.spotube.core.ui.component.dragScrollable
import org.spotube.spotube.core.ui.component.spotify.SpotifyChip
import org.spotube.spotube.core.ui.component.spotify.SpotifyGreetingHeader
import org.spotube.spotube.core.ui.component.spotify.SpotifySectionHeader
import org.spotube.spotube.core.ui.component.spotify.SpotifyShortcutTile
import org.spotube.spotube.core.ui.component.spotify.SpotifyTokens
import org.spotube.spotube.core.ui.component.spotify.SpotifyTopBarIcon
import org.spotube.spotube.core.ui.component.spotify.spotifyGreetingKey
import org.spotube.spotube.core.ui.misc.SkeletonTree
import org.spotube.spotube.core.navigation.NavigationCommands
import org.spotube.spotube.core.navigation.Routes
import org.spotube.spotube.modules.shell.ConnectStatusBanner
import org.spotube.spotube.resources.iconsax.IconsaxSoundTwotone
import org.spotube.spotube.getPlatform
import org.spotube.spotube.modules.shell.LocalAppShellBottomInset
import org.spotube.spotube.modules.support.LocalSupportPrompt
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxHeart2
import org.spotube.spotube.resources.iconsax.IconsaxMirroringScreen
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.support_button_label
import spotube.composeapp.generated.resources.devices_title
import spotube.composeapp.generated.resources.jam_screen_title
import spotube.composeapp.generated.resources.settings_screen_title
import spotube.composeapp.generated.resources.spotify_greeting_afternoon
import spotube.composeapp.generated.resources.spotify_greeting_evening
import spotube.composeapp.generated.resources.spotify_greeting_morning
import spotube.composeapp.generated.resources.spotify_filter_all
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.spotube.millennium.adaptive.WindowBreakpoints
import org.spotube.spotube.resources.iconsax.IconsaxMirrorScreenRegular
import org.spotube.spotube.resources.iconsax.IconsaxSetting2

@Composable
fun HomeScreen(viewModel: HomeScreenViewModel) {
    val navigationCommands = koinInject<NavigationCommands>()
    val openSupport = LocalSupportPrompt.current
    val isSmallScreen = currentWindowWidthBreakpoint() isBelow WindowBreakpoints.Medium
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    val selectedGenreId = when (val s = state) {
        is HomeScreenState.Data -> s.selectedGenreId
        else -> null
    }

    LaunchedEffect(selectedGenreId) {
        listState.scrollToItem(0)
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo }
            .map { layoutInfo ->
                val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index
                val totalItems = layoutInfo.totalItemsCount
                Pair(lastVisibleIndex, totalItems)
            }
            .distinctUntilChanged()
            .collect { (lastVisibleIndex, totalItems) ->
                if (
                    lastVisibleIndex != null &&
                    totalItems > 0 &&
                    lastVisibleIndex >= totalItems - 2
                ) {
                    viewModel.loadMoreData()
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to SpotifyTokens.Elevated,
                    0.35f to SpotifyTokens.Base,
                    1f to SpotifyTokens.Base
                )
            )
    ) {
        val greetingRes = when (
            spotifyGreetingKey(
                Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour
            )
        ) {
            0 -> Res.string.spotify_greeting_morning
            1 -> Res.string.spotify_greeting_afternoon
            else -> Res.string.spotify_greeting_evening
        }
        SpotifyGreetingHeader(greeting = stringResource(greetingRes)) {
            SpotifyTopBarIcon(
                icon = Iconsax.IconsaxSetting2,
                contentDescription = stringResource(Res.string.settings_screen_title),
                onClick = { navigationCommands.navigateTo(Routes.Settings) }
            )
        }
        if (isSmallScreen) {
            ConnectStatusBanner(
                onOpenRemote = { navigationCommands.navigateTo(Routes.RemoteControl) },
                onOpenJam = { navigationCommands.navigateTo(Routes.Jam) }
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            if (!isSmallScreen) {
                HomeContent(
                    listState = listState,
                    state = state,
                    onRetry = { viewModel.refresh() },
                    onGenreSelected = { viewModel.selectGenre(it) })
            } else {
                MillenniumPullToRefreshBox(
                    isRefreshing = state is HomeScreenState.Loading,
                    onRefresh = {
                        viewModel.refresh()
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    HomeContent(
                        listState = listState,
                        state = state,
                        onRetry = { viewModel.refresh() },
                        onGenreSelected = { viewModel.selectGenre(it) })
                }
            }

            VerticalScrollbar(
                listState = listState,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
            )
        }
    }
}

@Composable
private fun HomeContent(
    listState: androidx.compose.foundation.lazy.LazyListState,
    state: HomeScreenState,
    onRetry: () -> Unit,
    onGenreSelected: (String) -> Unit
) {
    val shellBottomInset = LocalAppShellBottomInset.current
    val contentPadding = remember(shellBottomInset) {
        PaddingValues(top = 16.dp, bottom = 16.dp + shellBottomInset)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        when (state) {
            is HomeScreenState.Loading -> {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        repeat(3) {
                            SkeletonTree(true) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .padding(horizontal = 16.dp)
                                    )
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp)
                                    ) {
                                        items(4) {
                                            PlayableCard(
                                                title = "Item Title",
                                                subtitle = "Subtitle",
                                                imageURL = "https://placehold.co/600x400"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            is HomeScreenState.Error -> {
                item {
                    ErrorDisplay(
                        errorMessage = state.message,
                        onRetry = onRetry
                    )
                }
            }

            is HomeScreenState.Data -> {
                val shortcuts = remember(state.featuredItems) { state.featuredItems.take(6) }
                if (shortcuts.isNotEmpty()) {
                    item {
                        SpotifyShortcutGrid(items = shortcuts)
                    }
                }

                if (state.genres.isNotEmpty()) {
                    item {
                        GenreTabs(
                            genres = state.genres,
                            selectedGenreId = state.selectedGenreId,
                            onGenreSelected = onGenreSelected
                        )
                    }
                }
                val currentSections = state.selectedGenreId?.let { state.browseSections[it] }

                if (currentSections == null) {
                    item {
                        SkeletonTree(true) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .padding(horizontal = 16.dp)
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp)
                                ) {
                                    items(4) {
                                        PlayableCard(
                                            title = "Item Title",
                                            subtitle = "Subtitle",
                                            imageURL = "https://placehold.co/600x400"
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(currentSections) { section ->
                        HomeSection(
                            title = section.title,
                            subtitle = section.description,
                            items = section.items
                        )
                    }
                }

                if (state is HomeScreenState.Data.LoadingMore) {
                    item {
                        SkeletonTree(true) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .padding(horizontal = 16.dp)
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp)
                                ) {
                                    items(4) {
                                        PlayableCard(
                                            title = "Item Title",
                                            subtitle = "Subtitle",
                                            imageURL = "https://placehold.co/600x400"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    }
}

@Composable
private fun HomeSection(
    title: String,
    subtitle: String? = null,
    items: List<MetadataBrowseItem>,
    modifier: Modifier = Modifier
) {
    val rowState = rememberLazyListState()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SpotifySectionHeader(title = title, subtitle = subtitle)

        LazyRow(
            state = rowState,
            modifier = Modifier.dragScrollable(rowState),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            items(items) { browseItem ->
                when (browseItem) {
                    is MetadataBrowseItem.Album -> AlbumCard(album = browseItem.data)
                    is MetadataBrowseItem.Artist -> ArtistCard(artist = browseItem.data)
                    is MetadataBrowseItem.Playlist -> PlaylistCard(playlist = browseItem.data)
                    is MetadataBrowseItem.Track -> TrackCard(track = browseItem.data)
                    is MetadataBrowseItem.User -> UserCard(user = browseItem.data)
                }
            }
        }
    }
}

@Composable
private fun GenreTabs(
    genres: List<MetadataBrowseGenre>,
    selectedGenreId: String?,
    onGenreSelected: (String) -> Unit
) {
    val rowState = rememberLazyListState()

    LazyRow(
        state = rowState,
        modifier = Modifier.dragScrollable(rowState),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(genres) { genre ->
            SpotifyChip(
                text = genre.name,
                selected = selectedGenreId == genre.id,
                onClick = { onGenreSelected(genre.id) }
            )
        }
    }
}

/**
 * Spotify home shortcut grid: six tiles, two per row, directly under the greeting.
 */
@Composable
private fun SpotifyShortcutGrid(
    items: List<MetadataBrowseItem>,
    modifier: Modifier = Modifier
) {
    val navigationCommands = koinInject<NavigationCommands>()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                rowItems.forEach { item ->
                    val title = when (item) {
                        is MetadataBrowseItem.Album -> item.data.title
                        is MetadataBrowseItem.Playlist -> item.data.title
                        is MetadataBrowseItem.Track -> item.data.title
                        is MetadataBrowseItem.Artist -> item.data.name
                        is MetadataBrowseItem.User -> item.data.displayName ?: item.data.username
                    }
                    val imageUrl = when (item) {
                        is MetadataBrowseItem.Album -> item.data.thumbnails.firstOrNull()?.url
                        is MetadataBrowseItem.Playlist -> item.data.thumbnails.firstOrNull()?.url
                        is MetadataBrowseItem.Track -> (item.data.album?.thumbnails
                            ?: item.data.thumbnails)?.firstOrNull()?.url

                        is MetadataBrowseItem.Artist -> item.data.thumbnails.firstOrNull()?.url
                        is MetadataBrowseItem.User -> item.data.thumbnails.firstOrNull()?.url
                    }
                    val onClick: (() -> Unit)? = when (item) {
                        is MetadataBrowseItem.Album -> {
                            { navigationCommands.navigateTo(Routes.Album(item.data.id)) }
                        }

                        is MetadataBrowseItem.Playlist -> {
                            { navigationCommands.navigateTo(Routes.Playlist(item.data.id)) }
                        }

                        is MetadataBrowseItem.Artist -> {
                            { navigationCommands.navigateTo(Routes.Artist(item.data.id)) }
                        }

                        else -> null
                    }
                    SpotifyShortcutTile(
                        title = title,
                        imageURL = imageUrl,
                        onClick = onClick,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}
