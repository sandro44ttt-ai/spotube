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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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

    /** dark_base_background_elevated_base: cards and nav surfaces */
    val Surface = Color(0xFF1F1F1F)

    /** bg_page_bottom_sheet_color: bottom sheets, player chrome */
    val Sheet = Color(0xFF242424)

    /** gray_15: floating now playing bar */
    val Bar = Color(0xFF282828)

    /** dark_base_background_elevated_highlight */
    val Highlight = Color(0xFF2A2A2A)

    /** dark_base_background_elevated_press */
    val Press = Color(0xFF191919)

    /** dark_base_background_tinted_base (10% white) */
    val Tinted = Color(0x1AFFFFFF)

    /** dark_base_background_tinted_highlight (14% white) */
    val TintedHighlight = Color(0x24FFFFFF)

    /** dark_base_text_subdued */
    val Muted = Color(0xFFB3B3B3)

    /** dark_base_essential_subdued: inactive icons */
    val IconMuted = Color(0xFF7C7C7C)

    /** gray_50: chip and secondary button outlines */
    val Outline = Color(0xFF7F7F7F)

    /** dark_base_decorative_subdued */
    val Divider = Color(0xFF292929)

    /** dark_brightaccent_background_base */
    val Green = Color(0xFF1ED760)

    /** dark_brightaccent_background_highlight */
    val GreenLight = Color(0xFF3BE477)

    /** green_dark */
    val GreenDark = Color(0xFF14833B)

    /** dark_brightaccent_background_press */
    val GreenPressed = Color(0xFF1ABC54)

    /** dark_base_essential_negative */
    val Negative = Color(0xFFED2C3F)
}

/**
 * Spotify filter chip, from `res/drawable/chip_button_background.xml`:
 * 32dp tall, fully rounded (pill), transparent with a 1dp gray outline when unselected,
 * dark green fill with a bright green 1.5dp outline when selected.
 */
@Composable
fun SpotifyChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderWidth = if (selected) 1.5.dp else 1.dp
    val borderColor = if (selected) SpotifyTokens.Green else SpotifyTokens.Outline
    val background = if (selected) SpotifyTokens.GreenDark else Color.Transparent
    Box(
        modifier = modifier
            .height(32.dp)
            .clip(RoundedCornerShape(1500.dp)) // fully rounded pill
            .background(background)
            .border(borderWidth, borderColor, RoundedCornerShape(1500.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = AppTheme.typography.labelMedium.copy(
                color = Color.White,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}

/**
 * Spotify primary button: solid #1ED760 pill, 48dp tall, black 16sp bold label
 * (`cat_button_primary_*`, radius 24sp, height 48sp).
 */
@Composable
fun SpotifyPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (enabled) SpotifyTokens.Green else SpotifyTokens.Green.copy(alpha = 0.4f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = AppTheme.typography.labelLarge.copy(
                color = Color.Black,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1
        )
    }
}

/**
 * Spotify secondary pill: 32dp tall, transparent with a 1dp #7F7F7F outline and
 * white 13sp bold label (Follow / More on album and playlist pages).
 */
@Composable
fun SpotifySecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, SpotifyTokens.Outline, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = AppTheme.typography.labelLarge.copy(
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1
        )
    }
}

/** Spotify icon button: 24dp glyph in a 48dp touch target, #B3B3B3 by default. */
@Composable
fun SpotifyIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = SpotifyTokens.Muted,
    enabled: Boolean = true
) {
    MillenniumIconButton(
        onClick = onClick,
        enabled = enabled,
        style = MillenniumIconButtonStyle.Quiet,
        modifier = modifier.size(48.dp)
    ) {
        MillenniumIcon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp),
            tint = if (enabled) tint else tint.copy(alpha = 0.4f)
        )
    }
}

/** Spotify's green circular play button: #1ED760 fill, black glyph, soft shadow. */
@Composable
fun SpotifyPlayButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 52.dp
) {
    val glyphSize = when {
        size >= 48.dp -> 24.dp   // large: 24dp glyph in 52dp circle
        size >= 28.dp -> 16.dp   // card: 16dp glyph in 32dp circle
        else -> 12.dp            // list row: 12dp glyph in 23dp circle
    }
    Box(
        modifier = modifier
            .size(size)
            .shadow(6.dp, CircleShape)
            .clip(CircleShape)
            .background(SpotifyTokens.Green)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        MillenniumIcon(
            imageVector = if (isPlaying) Iconsax.SpotifyPause else Iconsax.SpotifyPlay,
            contentDescription = null,
            modifier = Modifier.size(glyphSize),
            tint = Color.Black
        )
    }
}

/** Small play button for cards/grid (32dp container, 16dp glyph). */
@Composable
fun SpotifyPlayButtonSmall(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) = SpotifyPlayButton(isPlaying, onClick, modifier, 32.dp)

/** Very small play button for list rows (23dp container, 12dp glyph, 48dp touch target). */
@Composable
fun SpotifyPlayButtonList(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) = SpotifyPlayButton(isPlaying, onClick, modifier.size(48.dp).clip(CircleShape).background(Color.Transparent), 23.dp)

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
            .windowInsetsPadding(WindowInsets.statusBars)
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
                .background(SpotifyTokens.Surface),
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
                .background(SpotifyTokens.Surface)
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
