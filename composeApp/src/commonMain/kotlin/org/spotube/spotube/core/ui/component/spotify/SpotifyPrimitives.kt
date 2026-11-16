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

package org.spotube.spotube.core.ui.component.spotify

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
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
import coil3.compose.AsyncImage
import com.composeunstyled.Text
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxMusic

/**
 * Raw values taken from the Spotify 9.2.0.2053 resource table
 * (`dark_base_*`, `bg_page_bottom_sheet_color`, `chip_button_corner_radius`, ...).
 * They are only used where the app chrome must be pixel-identical to Spotify,
 * everything else follows the selected palette.
 */
object SpotifyTokens {
    /** dark_base_background_base */
    val Base = Color(0xFF121212)

    /** bg_page_bottom_sheet_color / now playing bar + sheets */
    val Elevated = Color(0xFF242424)

    /** dark_base_background_elevated_highlight, shortcut tiles + chips */
    val Highlight = Color(0xFF2A2A2A)

    /** dark_base_text_subdued */
    val Muted = Color(0xFFB3B3B3)

    /** dark_base_decorative_subdued */
    val Divider = Color(0xFF292929)

    /** dark_brightaccent_background_base */
    val Green = Color(0xFF1ED760)
}

/** Spotify filter chip: 32dp tall, 16dp radius, white when selected. */
@Composable
fun SpotifyChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background = if (selected) Color.White else SpotifyTokens.Highlight
    val foreground = if (selected) Color.Black else Color.White
    Box(
        modifier = modifier
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = AppTheme.typography.labelMedium.copy(
                color = foreground,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            ),
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}

/** Spotify shelf title: 24sp bold, 16dp leading padding. */
@Composable
fun SpotifySectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        Text(
            text = title,
            style = AppTheme.typography.headlineSmall.copy(
                fontSize = 24.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = AppTheme.typography.bodySmall.copy(color = SpotifyTokens.Muted),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Spotify home greeting row: title on the left, action icons on the right. */
@Composable
fun SpotifyGreetingHeader(
    greeting: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = greeting,
            style = AppTheme.typography.headlineSmall.copy(
                fontSize = 24.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/** Small round icon button used by the Spotify top bars. */
@Composable
fun SpotifyTopBarIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    onClick: () -> Unit
) {
    MillenniumIconButton(
        onClick = onClick,
        style = MillenniumIconButtonStyle.Quiet,
        modifier = Modifier.size(32.dp)
    ) {
        MillenniumIcon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp),
            tint = Color.White
        )
    }
}

/**
 * Spotify home shortcut tile: 56dp tall, 4dp radius, 2 columns.
 * Artwork on the left, bold 13sp title on the right.
 */
@Composable
fun SpotifyShortcutTile(
    title: String,
    imageURL: String?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(SpotifyTokens.Highlight)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(56.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp))
                .background(SpotifyTokens.Elevated),
            contentAlignment = Alignment.Center
        ) {
            if (!imageURL.isNullOrBlank()) {
                AsyncImage(
                    model = imageURL,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                MillenniumIcon(
                    imageVector = Iconsax.IconsaxMusic,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = SpotifyTokens.Muted
                )
            }
        }
        Text(
            text = title,
            style = AppTheme.typography.labelLarge.copy(
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

/**
 * Spotify shelf card: square artwork with 8dp radius, bold title and muted subtitle.
 * There is no card background, exactly like the current Spotify home.
 */
@Composable
fun SpotifyMediaCard(
    title: String,
    subtitle: String?,
    imageURL: String?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    playOverlay: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .width(160.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SpotifyTokens.Elevated)
        ) {
            if (!imageURL.isNullOrBlank()) {
                AsyncImage(
                    model = imageURL,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            if (playOverlay != null) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    playOverlay()
                }
            }
        }
        Text(
            text = title,
            style = AppTheme.typography.titleSmall.copy(
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp)
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = AppTheme.typography.bodySmall.copy(color = SpotifyTokens.Muted),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

/** Spotify's time-of-day greeting. */
fun spotifyGreetingKey(hour: Int): Int = when (hour) {
    in 5..11 -> 0
    in 12..17 -> 1
    else -> 2
}

/** Background gradient used by the now playing screen: dark gray fading into the page color. */
@Composable
fun spotifyPlayerGradient(): Brush = Brush.verticalGradient(
    0f to SpotifyTokens.Highlight,
    1f to SpotifyTokens.Base
)
