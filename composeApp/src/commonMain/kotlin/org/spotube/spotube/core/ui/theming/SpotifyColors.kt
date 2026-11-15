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

package org.spotube.spotube.core.ui.theming

import androidx.compose.ui.graphics.Color
import org.spotube.millennium.theme.MillenniumColorSchemes
import org.spotube.millennium.theme.MillenniumColors

/**
 * Spotify's official "Base" token system colors (extracted from Spotify 9.2.0.2053 APK).
 * These are the exact values used by the Spotify Android app.
 * Dark theme uses `dark_base_*`, `dark_brightaccent_*`, `dark_appframe_*` tokens.
 * Light theme uses `light_base_*`, `light_brightaccent_*`, `light_appframe_*` tokens.
 */
val SpotifyDarkColors = MillenniumColors(
    // dark_base_background_base = #121212
    background = Color(0xFF121212),
    // dark_base_background_elevated_base = #1F1F1F (used as surface)
    surface = Color(0xFF1F1F1F),
    // dark_base_background_elevated_highlight = #2A2A2A (used as raised/hover)
    raised = Color(0xFF2A2A2A),
    // dark_base_background_tinted_base = #1AFFFFFF (used as inset/overlay)
    inset = Color(0x1AFFFFFF),
    // dark_base_text_base = #FFFFFFFF
    text = Color(0xFFFFFFFF),
    // dark_base_text_subdued = #B3B3B3
    muted = Color(0xFFB3B3B3),
    // dark_base_decorative_subdued = #292929 (used as border/divider)
    border = Color(0xFF292929),
    // dark_base_background_elevated_highlight = #2A2A2A (used as highlight/focus ring)
    highlight = Color(0xFF2A2A2A),
    // dark_brightaccent_background_base = #1ED760 (Spotify Green - exact)
    accent = Color(0xFF1ED760),
    // dark_brightaccent_background_highlight = #3BE477
    accentTop = Color(0xFF3BE477),
    // dark_brightaccent_background_press = #1ABC54
    accentBottom = Color(0xFF1ABC54),
    // dark_brightaccent_decorative_base = #000000 (text on accent)
    onAccent = Color(0xFF000000),
    // dark_base_essential_positive = #1ED760
    success = Color(0xFF1ED760),
    // dark_base_essential_negative = #ED2C3F
    danger = Color(0xFFED2C3F),
    // dark_base_text_announcement (blue) - not used, fallback to scrim
    scrim = Color(0xCC000000),
)

val SpotifyLightColors = MillenniumColors(
    // light_base_background_base = #FFFFFFFF
    background = Color(0xFFFFFFFF),
    // light_base_background_elevated_base = #FFFFFFFF
    surface = Color(0xFFFFFFFF),
    // light_base_background_elevated_highlight = #F5F5F5
    raised = Color(0xFFF5F5F5),
    // light_base_background_tinted_base = #14000000
    inset = Color(0x14000000),
    // light_base_text_base = #000000
    text = Color(0xFF000000),
    // light_base_text_subdued = #656565
    muted = Color(0xFF656565),
    // light_base_decorative_subdued = #DEDEDE
    border = Color(0xFFDEDEDE),
    // light_base_background_elevated_highlight = #F5F5F5
    highlight = Color(0xFFF5F5F5),
    // light_brightaccent_background_base = #1ED760 (same green)
    accent = Color(0xFF1ED760),
    // light_brightaccent_background_highlight = #3BE477
    accentTop = Color(0xFF3BE477),
    // light_brightaccent_background_press = #1ABC54
    accentBottom = Color(0xFF1ABC54),
    // light_brightaccent_decorative_base = #000000
    onAccent = Color(0xFF000000),
    // light_base_essential_positive = #159542 (darker green for light mode)
    success = Color(0xFF159542),
    // light_base_essential_negative = #E91429
    danger = Color(0xFFE91429),
    scrim = Color(0x66000000),
)

/**
 * Orange and Lime palettes use Spotify's green as bright accent but with
 * different decorative/essential tones if desired. For now they alias to the
 * same base palettes so the "Blue/Orange/Lime" setting only changes non-accent
 * decorative colors. Override here if you want true per-palette theming.
 */
val SpotifyColorSchemes = MillenniumColorSchemes(
    light = SpotifyLightColors,
    dark = SpotifyDarkColors,
    orangeLight = SpotifyLightColors,
    orangeDark = SpotifyDarkColors,
    limeLight = SpotifyLightColors,
    limeDark = SpotifyDarkColors,
)
