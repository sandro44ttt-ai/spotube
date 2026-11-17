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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
    val background = if (selected) SpotifyTokens.Green else SpotifyTokens.Highlight
    Box(
        modifier = modifier
            .height(32.dp)
            .clip(RoundedCornerShape(1500.dp)) // fully rounded pill
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = AppTheme.typography.labelLarge.copy(
                color = if (selected) Color.Black else Color.White,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
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

/** Small play button for cards/grid (48dp container, 24dp glyph), matching Spotify cards. */
@Composable
fun SpotifyPlayButtonSmall(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) = SpotifyPlayButton(isPlaying, onClick, modifier, 48.dp)

/** Very small play button for list rows (23dp container, 12dp glyph, 48dp touch target). */
@Composable
fun SpotifyPlayButtonList(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) = SpotifyPlayButton(isPlaying, onClick, modifier.size(48.dp).clip(CircleShape).background(Color.Transparent), 23.dp)

/** Spotify action pill with a leading glyph: 48dp tall, green fill, black label. */
@Composable
fun SpotifyPlayPillButton(
    text: String,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(SpotifyTokens.Green)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MillenniumIcon(
            imageVector = if (isPlaying) Iconsax.SpotifyPause else Iconsax.SpotifyPlay,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = Color.Black
        )
        Text(
            text = text,
            style = AppTheme.typography.labelLarge.copy(
                color = Color.Black,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Spotify outline pill with a leading glyph: 48dp tall, 1dp #7F7F7F outline, white label. */
@Composable
fun SpotifyOutlinePillButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, SpotifyTokens.Outline, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MillenniumIcon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = Color.White
        )
        Text(
            text = text,
            style = AppTheme.typography.labelLarge.copy(
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Spotify outline circular icon button (shuffle / download actions in collection headers). */
@Composable
fun SpotifyOutlineCircleIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .border(1.dp, SpotifyTokens.Outline, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        MillenniumIcon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp),
            tint = Color.White
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
                fontSize = 28.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            ),
            maxLines = 2,
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

private fun playerTint(seed: String?, lightness: Float): Color {
    val hash = (seed ?: "").hashCode() and 0x7FFFFFFF
    val hue = (hash % 360).toFloat()
    val saturation = 0.38f + ((hash / 360) % 22) / 100f
    return hslColor(hue, saturation.coerceIn(0f, 1f), lightness)
}

private fun hslColor(hue: Float, saturation: Float, lightness: Float): Color {
    val h = ((hue % 360f) + 360f) % 360f / 60f
    val c = (1f - kotlin.math.abs(2f * lightness - 1f)) * saturation
    val x = c * (1f - kotlin.math.abs(h % 2f - 1f))
    val m = lightness - c / 2f
    val (r, g, b) = when (h.toInt()) {
        0 -> Triple(c, x, 0f)
        1 -> Triple(x, c, 0f)
        2 -> Triple(0f, c, x)
        3 -> Triple(0f, x, c)
        4 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(r + m, g + m, b + m)
}

/**
 * Spotify now playing backdrop: a dark tint derived from the current artwork,
 * fading into the page color. The seed is stable per track, so the color changes
 * with the song exactly like the real player.
 */
@Composable
fun spotifyPlayerTintBrush(seed: String?, tint: Color? = null): Brush {
    val top = remember(seed, tint) {
        tint?.let { lerp(SpotifyTokens.Base, it, 0.45f) } ?: playerTint(seed, 0.23f)
    }
    val bottomTint = remember(seed, tint) {
        tint?.let { lerp(SpotifyTokens.Base, it, 0.22f) } ?: playerTint(seed, 0.12f)
    }
    return Brush.verticalGradient(
        0f to top,
        0.42f to bottomTint,
        0.72f to SpotifyTokens.Base,
        1f to SpotifyTokens.Base
    )
}

@Composable
fun spotifyCollectionHeaderBrush(tint: Color?): Brush {
    val top = remember(tint) {
        tint?.let { lerp(SpotifyTokens.Base, it, 0.55f) } ?: SpotifyTokens.Highlight
    }
    val middle = remember(tint) {
        tint?.let { lerp(SpotifyTokens.Base, it, 0.22f) } ?: SpotifyTokens.Base
    }
    return Brush.verticalGradient(
        0f to top,
        0.55f to middle,
        1f to SpotifyTokens.Base
    )
}

/** Exact Spotify explicit pill: light gray fill, dark 9sp bold label. */
@Composable
fun SpotifyExplicitBadge(
    label: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(16.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(SpotifyTokens.Muted)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = AppTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = SpotifyTokens.Base
            ),
            maxLines = 1
        )
    }
}

/** Spotify circular top bar button (back / overflow) over full-bleed artwork. */
@Composable
fun SpotifyCircleTopBarButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        MillenniumIcon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp),
            tint = Color.White
        )
    }
}

/** Spotify profile avatar shown at the top right of the home screen. */
@Composable
fun SpotifyAvatar(
    imageURL: String?,
    name: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(SpotifyTokens.Highlight)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (!imageURL.isNullOrBlank()) {
            AsyncImage(
                model = imageURL,
                contentDescription = name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = name?.trim()?.take(1)?.uppercase().orEmpty().ifBlank { "?" },
                style = AppTheme.typography.labelLarge.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
        }
    }
}

/** Spotify 2026 seek bar: 4dp white track, white thumb, 12sp timestamps. */
@Composable
fun SpotifySeekBar(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var widthPx by remember { mutableFloatStateOf(0f) }
    val trackHeight = 4.dp
    val thumbRadius = 5.dp
    val activeColor = if (enabled) Color.White else Color.White.copy(alpha = 0.35f)
    val inactiveColor = Color.White.copy(alpha = 0.25f)

    Box(
        modifier = modifier
            .height(24.dp)
            .onSizeChanged { widthPx = it.width.toFloat() }
            .pointerInput(enabled, widthPx) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    if (widthPx > 0f) {
                        onValueChange((offset.x / widthPx).coerceIn(0f, 1f))
                        onValueChangeFinished()
                    }
                }
            }
            .pointerInput(enabled, widthPx) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onDragEnd = { onValueChangeFinished() },
                    onDragCancel = { onValueChangeFinished() }
                ) { change, _ ->
                    if (widthPx > 0f) {
                        onValueChange((change.position.x / widthPx).coerceIn(0f, 1f))
                        change.consume()
                    }
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
            val centerY = size.height / 2f
            val w = size.width
            val thumbR = thumbRadius.toPx()
            val progress = value.coerceIn(0f, 1f)
            drawLine(
                color = inactiveColor,
                start = Offset(0f, centerY),
                end = Offset(w, centerY),
                strokeWidth = trackHeight.toPx(),
                cap = StrokeCap.Round
            )
            val activeX = (w * progress).coerceIn(thumbR, (w - thumbR).coerceAtLeast(thumbR))
            drawLine(
                color = activeColor,
                start = Offset(0f, centerY),
                end = Offset(activeX, centerY),
                strokeWidth = trackHeight.toPx(),
                cap = StrokeCap.Round
            )
            drawCircle(color = activeColor, radius = thumbR, center = Offset(activeX, centerY))
        }
    }
}

/** Spotify player bottom action (Lyrics / Share / Queue): 24dp icon with 11sp label. */
@Composable
fun SpotifyPlayerAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MillenniumIcon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = SpotifyTokens.Muted
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = AppTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                color = SpotifyTokens.Muted
            ),
            maxLines = 1
        )
    }
}

/** Small circular outlined "+" next to the track title on the now playing screen. */
@Composable
fun SpotifyCircleAddButton(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.55f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        MillenniumIcon(
            imageVector = Iconsax.SpotifyPlusGlyph,
            contentDescription = contentDescription,
            modifier = Modifier.size(16.dp),
            tint = Color.White
        )
    }
}
