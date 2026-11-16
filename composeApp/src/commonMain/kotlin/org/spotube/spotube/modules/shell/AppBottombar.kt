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

package org.spotube.spotube.modules.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composeunstyled.Text
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.spotube.core.navigation.NavigationState
import org.spotube.spotube.core.navigation.Navigator
import org.spotube.spotube.core.navigation.Routes
import org.spotube.spotube.core.ui.component.spotify.SpotifyTokens
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.modules.downloads.DownloadBadgeIndicator
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxHome
import org.spotube.spotube.resources.iconsax.IconsaxMusicLibrary
import org.spotube.spotube.resources.iconsax.IconsaxSearch
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.library_toolbar_compact_title
import spotube.composeapp.generated.resources.nav_home
import spotube.composeapp.generated.resources.nav_search

private data class SpotifyTab(
    val label: StringResource,
    val icon: ImageVector,
    val route: Routes
)

/**
 * Spotify bottom navigation: exactly three destinations (Home, Search, Your Library),
 * 56dp tall, no pill/indicator behind the selected item - only the icon and label
 * switch from #B3B3B3 to white. Settings lives in the Home top bar, as on Spotify.
 */
@Composable
fun AppBottombar(
    navigator: Navigator,
    navigationState: NavigationState,
    modifier: Modifier = Modifier,
    libraryBadge: @Composable () -> Unit = { DownloadBadgeIndicator() }
) {
    val density = LocalDensity.current
    val navigationBarInset = with(density) {
        WindowInsets.navigationBars.getBottom(this).toDp()
    }
    val tabs = remember {
        listOf(
            SpotifyTab(Res.string.nav_home, Iconsax.IconsaxHome, Routes.Home),
            SpotifyTab(Res.string.nav_search, Iconsax.IconsaxSearch, Routes.Search),
            SpotifyTab(Res.string.library_toolbar_compact_title, Iconsax.SpotifyLibrary, Routes.Library)
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SpotifyTokens.Base)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(SpotifyTokens.Divider)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val selected = navigationState.topLevelRoute == tab.route
                val tint = if (selected) Color.White else SpotifyTokens.Muted
                val label = stringResource(tab.label)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { navigator.navigate(tab.route) }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(contentAlignment = Alignment.TopEnd) {
                        MillenniumIcon(
                            imageVector = tab.icon,
                            contentDescription = label,
                            modifier = Modifier.size(24.dp),
                            tint = tint
                        )
                        if (tab.route == Routes.Library) {
                            Box(modifier = Modifier.padding(start = 20.dp)) { libraryBadge() }
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = label,
                        style = AppTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            color = tint,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(navigationBarInset))
    }
}
