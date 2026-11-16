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

package org.spotube.spotube.modules.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composeunstyled.Text
import com.composeunstyled.currentWindowWidthBreakpoint
import org.spotube.spotube.getPlatform
import org.spotube.spotube.isDesktop
import org.spotube.spotube.core.ui.component.ApplicationMainBar
import org.spotube.spotube.core.ui.component.spotify.SpotifyChip
import org.spotube.spotube.core.ui.component.spotify.SpotifyIconButton
import org.spotube.spotube.core.ui.component.spotify.SpotifyTokens
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.modules.downloads.DownloadsScreen
import org.spotube.spotube.modules.library.album.LibraryAlbumsScreen
import org.spotube.spotube.modules.library.artist.LibraryArtistsScreen
import org.spotube.spotube.modules.library.local_tracks.LibraryLocalTracksScreen
import org.spotube.spotube.modules.library.playlist.LibraryPlaylistsScreen
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxAddSquare
import org.spotube.spotube.resources.iconsax.IconsaxCloseSquare
import org.spotube.spotube.resources.iconsax.IconsaxRefreshRight
import org.spotube.spotube.resources.iconsax.IconsaxSearchBroken
import org.jetbrains.compose.resources.StringResource
import org.spotube.spotube.modules.plugin.guard.MetadataPluginGuard
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.spotube.millennium.adaptive.WindowBreakpoints
import org.spotube.millennium.composables.MillenniumButton
import org.spotube.millennium.composables.MillenniumButtonStyle
import org.spotube.millennium.composables.MillenniumTextField
import compose.icons.FeatherIcons
import compose.icons.feathericons.Grid
import compose.icons.feathericons.List
import spotube.composeapp.generated.resources.*

@Composable
fun LibraryScreen(libraryState: LibraryState = koinInject()) {
    val state by libraryState.uiState.collectAsStateWithLifecycle()
    val expandedDesktop = getPlatform().isDesktop() &&
            (currentWindowWidthBreakpoint() isAtLeast WindowBreakpoints.Expanded)
    val isCompact = currentWindowWidthBreakpoint() isBelow WindowBreakpoints.Expanded
    val compactDesktop = getPlatform().isDesktop() && isCompact
    val viewMode = state.viewModeOverride ?: defaultLibraryViewMode(isCompact)
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    if (state.currentTab == LibraryTab.Downloads && !isCompact) {
        DownloadsScreen(
            headerInAppBar = true,
            topContent = { if (!expandedDesktop) LibraryTabs(state, libraryState::onTabSelected) }
        )
        return
    }

    Column(Modifier.fillMaxSize()) {
        ApplicationMainBar(
            backButton = false,
            title = { LibraryToolbarTitle(state, expandedDesktop, compactDesktop) },
            actions = {
                LibraryToolbarActions(
                    state = state,
                    expandedDesktop = expandedDesktop,
                    onOpenSearch = libraryState::openSearch,
                    onCloseSearch = {
                        focusManager.clearFocus()
                        keyboard?.hide()
                        libraryState.closeSearch()
                    },
                    onCreatePlaylist = libraryState::showCreatePlaylist,
                    onRefresh = libraryState::refreshLocalTracks,
                    onToggleView = { libraryState.toggleViewMode(isCompact) },
                    viewMode = viewMode,
                    compactDesktop = compactDesktop
                )
            }
        )
        if (!expandedDesktop) {
            LibraryTabs(state, libraryState::onTabSelected)
        }
        Column(Modifier.weight(1f)) {
            when (state.currentTab) {
                LibraryTab.Playlists -> MetadataPluginGuard(showTopBar = false, requireLogin = true) {
                    LibraryPlaylistsScreen(
                        showCreatePlaylist = state.showCreatePlaylist,
                        viewMode = viewMode
                    )
                }

                LibraryTab.Albums -> MetadataPluginGuard(showTopBar = false, requireLogin = true) {
                    LibraryAlbumsScreen(viewMode = viewMode)
                }
                LibraryTab.Artists -> MetadataPluginGuard(showTopBar = false, requireLogin = true) {
                    LibraryArtistsScreen(viewMode = viewMode)
                }
                LibraryTab.LocalTracks -> LibraryLocalTracksScreen()
                LibraryTab.Downloads -> DownloadsScreen()
            }
        }
    }
}

@Composable
private fun LibraryTabs(state: LibraryToolbarUiState, onTabSelected: (LibraryTab) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(LibraryTab.entries) { tab ->
            SpotifyChip(
                text = stringResource(tab.titleResource),
                selected = state.currentTab == tab,
                onClick = { onTabSelected(tab) }
            )
        }
    }
}

@Composable
internal fun LibraryToolbarTitle(
    state: LibraryToolbarUiState,
    expandedDesktop: Boolean,
    compactDesktop: Boolean = false
) {
    if (!expandedDesktop && state.searchMode) {
        LibraryFilterField(
            state.searchState,
            state.currentTab.filterPlaceholder,
            Modifier.fillMaxWidth(),
            autoFocus = true
        )
    } else if (!expandedDesktop) {
        Text(
            text = stringResource(if (compactDesktop) Res.string.library_toolbar_compact_title else Res.string.library_screen_title),
            style = AppTheme.typography.headlineSmall.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun RowScope.LibraryToolbarActions(
    state: LibraryToolbarUiState,
    expandedDesktop: Boolean,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onCreatePlaylist: () -> Unit,
    onRefresh: () -> Unit,
    onToggleView: () -> Unit = {},
    viewMode: LibraryViewMode = state.viewModeOverride ?: defaultLibraryViewMode(!expandedDesktop),
    compactDesktop: Boolean = false
) {
    if (!expandedDesktop && state.searchMode) {
        SpotifyIconButton(
            icon = Iconsax.IconsaxCloseSquare,
            contentDescription = stringResource(Res.string.library_action_close_search),
            onClick = onCloseSearch,
            modifier = Modifier.size(48.dp),
            tint = SpotifyTokens.Muted
        )
    } else {
        if (state.canFilter) {
            if (expandedDesktop) {
                LibraryFilterField(
                    state.searchState,
                    state.currentTab.filterPlaceholder,
                    Modifier.width(300.dp)
                )
            } else {
                SpotifyIconButton(
                    icon = Iconsax.IconsaxSearchBroken,
                    contentDescription = stringResource(Res.string.library_action_search),
                    onClick = onOpenSearch,
                    modifier = Modifier.size(48.dp),
                    tint = SpotifyTokens.Muted
                )
            }
        }
        if (state.canCreatePlaylist) {
            SpotifyIconButton(
                icon = Iconsax.IconsaxAddSquare,
                contentDescription = stringResource(Res.string.library_action_create_playlist),
                onClick = onCreatePlaylist,
                modifier = Modifier.size(48.dp),
                tint = SpotifyTokens.Muted
            )
        }
        if (state.canRefresh) {
            SpotifyIconButton(
                icon = Iconsax.IconsaxRefreshRight,
                contentDescription = stringResource(Res.string.library_action_refresh_local_tracks),
                onClick = onRefresh,
                modifier = Modifier.size(48.dp),
                tint = SpotifyTokens.Muted
            )
        }
        if (state.canChangeView) {
            SpotifyIconButton(
                icon = if (viewMode == LibraryViewMode.List) FeatherIcons.Grid else FeatherIcons.List,
                contentDescription = stringResource(if (viewMode == LibraryViewMode.List) Res.string.library_action_grid_view else Res.string.library_action_list_view),
                onClick = onToggleView,
                modifier = Modifier.size(48.dp),
                tint = SpotifyTokens.Muted
            )
        }
    }
}

@Composable
private fun LibraryFilterField(
    state: TextFieldState,
    placeholder: StringResource,
    modifier: Modifier,
    autoFocus: Boolean = false
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(autoFocus) {
        if (autoFocus) focusRequester.requestFocus()
    }
    MillenniumTextField(
        state = state,
        placeholder = stringResource(placeholder),
        minimumHeight = 36.dp,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        modifier = modifier.padding(vertical = 4.dp).focusRequester(focusRequester)
    )
}
