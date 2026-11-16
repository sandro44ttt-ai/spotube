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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import org.jetbrains.compose.resources.Font
import spotube.composeapp.generated.resources.Figtree_Variable
import spotube.composeapp.generated.resources.Res
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.spotube.spotube.core.ui.component.spotify.SpotifyTokens
import org.spotube.millennium.composables.MillenniumBottomNavigationBarTheme
import org.spotube.millennium.composables.MillenniumBottomNavigationBarThemeProperties
import org.spotube.millennium.composables.MillenniumBottomNavigationItemThemeProperties
import org.spotube.millennium.composables.MillenniumButtonTheme
import org.spotube.millennium.composables.MillenniumButtonThemeProperties
import org.spotube.millennium.composables.MillenniumCardTheme
import org.spotube.millennium.composables.MillenniumCardThemeProperties
import org.spotube.millennium.composables.MillenniumIconButtonTheme
import org.spotube.millennium.composables.MillenniumIconButtonThemeProperties
import org.spotube.millennium.composables.MillenniumModalBottomSheetTheme
import org.spotube.millennium.composables.MillenniumProgressTheme
import org.spotube.millennium.composables.MillenniumProgressThemeProperties
import org.spotube.millennium.composables.MillenniumTooltipHost
import org.spotube.millennium.composables.MillenniumTopAppBarTheme
import org.spotube.millennium.composables.MillenniumTopAppBarThemeProperties
import org.spotube.millennium.composables.defaultMillenniumModalBottomSheetTheme
import org.spotube.millennium.composables.defaultMillenniumTopAppBarTheme
import org.spotube.millennium.theme.MillenniumColorSchemes
import org.spotube.millennium.theme.MillenniumColors
import org.spotube.millennium.theme.MillenniumControlState
import org.spotube.millennium.theme.MillenniumControlStateProperty
import org.spotube.millennium.theme.MillenniumDarkThemeComponents
import org.spotube.millennium.theme.MillenniumLightThemeComponents
import org.spotube.millennium.theme.MillenniumPalette
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.millennium.theme.MillenniumThemeComponents
import org.spotube.millennium.theme.MillenniumThemeConfig
import org.spotube.spotube.modules.settings.Theme
import org.spotube.spotube.modules.settings.UserSettings

/**
 * Semantic color roles used across the app, resolved from the active Millennium palette.
 *
 * This is not a second palette: every role is derived from [MillenniumColors], so the
 * whole app follows the selected Millennium accent family and light/dark appearance.
 */
@Immutable
data class AppColors(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val inverseSurface: Color,
    val inverseOnSurface: Color,
    val outline: Color,
    val outlineVariant: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val scrim: Color
)

/** Maps Millennium's semantic palette onto the app's role names. */
fun appColorsFrom(m: MillenniumColors): AppColors = AppColors(
    primary = m.accent,
    onPrimary = m.onAccent,
    primaryContainer = m.raised,
    onPrimaryContainer = m.text,
    secondary = m.accent,
    onSecondary = m.onAccent,
    secondaryContainer = m.raised,
    onSecondaryContainer = m.text,
    tertiaryContainer = m.raised,
    onTertiaryContainer = m.text,
    background = m.background,
    onBackground = m.text,
    surface = m.surface,
    onSurface = m.text,
    surfaceVariant = m.inset,
    onSurfaceVariant = m.muted,
    surfaceContainer = m.surface,
    surfaceContainerHigh = m.raised,
    surfaceContainerHighest = m.raised,
    inverseSurface = m.text,
    inverseOnSurface = m.background,
    outline = m.border,
    outlineVariant = m.border,
    error = m.danger,
    onError = m.background,
    errorContainer = m.danger.copy(alpha = 0.16f),
    onErrorContainer = m.danger,
    scrim = m.scrim
)

/** Named text styles used by the app, matching Spotify's type scale exactly. */
@Immutable
data class AppTypography(
    val displaySmall: TextStyle,
    val headlineLarge: TextStyle,
    val headlineMedium: TextStyle,
    val headlineSmall: TextStyle,
    val titleLarge: TextStyle,
    val titleMedium: TextStyle,
    val titleSmall: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,
    val labelLarge: TextStyle,
    val labelMedium: TextStyle,
    val labelSmall: TextStyle
)

private fun spotifyStyle(
    size: Int,
    lineHeight: Int,
    weight: FontWeight = FontWeight.Normal,
    letterSpacing: Float = 0f,
    family: FontFamily = FontFamily.SansSerif
): TextStyle = TextStyle(
    fontFamily = family,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    fontWeight = weight,
    letterSpacing = letterSpacing.sp
)

/**
 * Spotify type scale (from Spotify Base tokens / Encore design system):
 * - Display: 32/40 Bold (headlineLarge), 28/36 Bold (headlineMedium), 24/32 Bold (headlineSmall)
 * - Title: 22/28 SemiBold (titleLarge), 16/24 Medium (titleMedium), 14/20 Medium (titleSmall)
 * - Body: 16/24 Regular (bodyLarge), 14/20 Regular (bodyMedium), 12/16 Regular (bodySmall)
 * - Label: 14/20 Medium (labelLarge), 12/16 Medium (labelMedium), 11/16 Medium (labelSmall)
 * - Button: 14/20 SemiBold (used in MillenniumButtonTheme)
 */
val AppDefaultTypography = spotifyTypography(FontFamily.SansSerif)

/** Spotify type scale rendered with a specific family (the bundled Figtree in the app). */
fun spotifyTypography(family: FontFamily): AppTypography = AppTypography(
    displaySmall = spotifyStyle(36, 44, FontWeight.Bold, family = family),      // not used, kept for compat
    headlineLarge = spotifyStyle(32, 40, FontWeight.Bold, family = family),      // Playlist/Album hero titles
    headlineMedium = spotifyStyle(28, 36, FontWeight.Bold, family = family),     // Section headers large
    headlineSmall = spotifyStyle(24, 32, FontWeight.Bold, family = family),      // Card titles
    titleLarge = spotifyStyle(22, 28, FontWeight.SemiBold, family = family),     // Toolbar titles
    titleMedium = spotifyStyle(16, 24, FontWeight.Medium, family = family),      // List item titles
    titleSmall = spotifyStyle(14, 20, FontWeight.Medium, family = family),       // Small card titles
    bodyLarge = spotifyStyle(16, 24, family = family),                           // Body text large
    bodyMedium = spotifyStyle(14, 20, family = family),                          // Standard body
    bodySmall = spotifyStyle(12, 16, family = family),                           // Captions, metadata
    labelLarge = spotifyStyle(14, 20, FontWeight.Medium, family = family),       // Button labels, tabs
    labelMedium = spotifyStyle(12, 16, FontWeight.Medium, family = family),      // Small labels
    labelSmall = spotifyStyle(11, 16, FontWeight.Medium, family = family)        // Tiny labels
)

/** Corner shapes matching Spotify's radius scale: 8dp (cards), 12dp (sheets), 16dp (modals), 48dp (pill buttons). */
@Immutable
data class AppShapes(
    val small: Shape,
    val medium: Shape,
    val large: Shape,
    val pill: Shape // 48dp = full pill for play buttons
)

val AppDefaultShapes = AppShapes(
    small = RoundedCornerShape(8.dp),    // Cards, chips
    medium = RoundedCornerShape(12.dp),  // Bottom sheets, modals
    large = RoundedCornerShape(16.dp),   // Large containers
    pill = RoundedCornerShape(48.dp)     // Full pill (play button)
)

val LocalAppColors =
    staticCompositionLocalOf { appColorsFrom(org.spotube.millennium.theme.MillenniumLightColors) }
val LocalAppTypography = staticCompositionLocalOf { AppDefaultTypography }
val LocalAppShapes = staticCompositionLocalOf { AppDefaultShapes }

/** Reads the active app theme tokens, all backed by Millennium. */
object AppTheme {
    val colors: AppColors @Composable get() = LocalAppColors.current
    val typography: AppTypography @Composable get() = LocalAppTypography.current
    val shapes: AppShapes @Composable get() = LocalAppShapes.current
}

/**
 * Creates Spotify-exact Millennium component theme overrides.
 * These replace Millennium defaults globally so every screen inherits the look.
 */
private fun spotifyMillenniumComponents(
    colors: MillenniumColors,
    isDark: Boolean
): MillenniumThemeComponents {
    val bg = if (isDark) colors.background else colors.background
    val surface = if (isDark) colors.surface else colors.surface
    val raised = if (isDark) colors.raised else colors.raised
    val border = if (isDark) colors.border else colors.border
    val text = if (isDark) colors.text else colors.text
    val muted = if (isDark) colors.muted else colors.muted
    val accent = colors.accent          // #1ED760
    val accentTop = colors.accentTop    // #3BE477
    val accentPress = colors.accentBottom // #1ABC54
    val onAccent = colors.onAccent      // #000000
    val highlight = colors.highlight
    val scrim = colors.scrim

    // Base theme to copy from
    val base = if (isDark) MillenniumDarkThemeComponents else MillenniumLightThemeComponents

    // --- Bottom Navigation Bar (Spotify: 62dp height, raised surface, pill selected item) ---
    val bnBarProps = MillenniumBottomNavigationBarThemeProperties(
        background = Brush.verticalGradient(0f to surface, 1f to surface),
        foreground = text,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        contentPadding = PaddingValues(0.dp, 0.dp, 8.dp, 0.dp),
        itemSpacing = 0.dp,
        minimumHeight = 62.dp,
        borderColor = border,
        borderWidth = 0.5.dp,
        shadowElevation = 8.dp,
        innerGlowColor = Color.Transparent
    )
    val bnItemDefault = MillenniumBottomNavigationItemThemeProperties(
        background = Brush.verticalGradient(0f to Color.Transparent, 1f to Color.Transparent),
        foreground = muted,
        shape = RoundedCornerShape(48.dp),
        contentPadding = PaddingValues(12.dp, 12.dp, 8.dp, 8.dp),
        minimumHeight = 48.dp,
        minimumWidth = 64.dp,
        iconSize = 24.dp,
        iconLabelSpacing = 4.dp,
        labelStyle = AppDefaultTypography.labelLarge.copy(color = muted),
        borderColor = Color.Transparent,
        borderWidth = 0.dp,
        shadowElevation = 0.dp,
        innerGlowColor = Color.Transparent,
        alpha = 1f,
        animationMillis = 200
    )
    val bnItemSelected = bnItemDefault.copy(
        background = Brush.verticalGradient(0f to accent, 1f to accentPress),
        foreground = onAccent,
        labelStyle = AppDefaultTypography.labelLarge.copy(color = onAccent, fontWeight = FontWeight.SemiBold),
        shadowElevation = 4.dp,
        innerGlowColor = accentTop.copy(alpha = 0.3f)
    )

    // --- Buttons (Spotify: flat green pill, black 16sp bold label, no glow/shadow) ---
    val btnPrimary = MillenniumButtonThemeProperties(
        background = Brush.verticalGradient(0f to accent, 1f to accent),
        foreground = onAccent,
        textStyle = AppDefaultTypography.labelLarge.copy(
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = onAccent
        ),
        shape = AppDefaultShapes.pill,
        padding = PaddingValues(horizontal = 32.dp, vertical = 12.dp),
        contentSpacing = 8.dp,
        minSize = DpSize(64.dp, 48.dp),
        borderWidth = 0.dp,
        borderColor = Color.Transparent,
        shadows = listOf(Color.Transparent),
        shadowElevation = 0.dp,
        innerGlowColor = Color.Transparent,
        innerGlowWidth = 0.dp,
        innerGlowInset = 0.dp,
        innerGlowTop = 0.dp,
        alpha = 1f,
        motion = org.spotube.millennium.composables.MillenniumButtonMotion(),
        innerGlowFollowsShape = true,
        bottomRimColor = Color.Transparent
    )
    val btnSecondary = btnPrimary.copy(
        background = Brush.verticalGradient(0f to Color.Transparent, 1f to Color.Transparent),
        foreground = text,
        textStyle = AppDefaultTypography.labelLarge.copy(
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = text
        ),
        padding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        minSize = DpSize(48.dp, 32.dp),
        borderWidth = 1.dp,
        borderColor = SpotifyTokens.Outline,
        innerGlowColor = Color.Transparent
    )
    val btnQuiet = btnSecondary.copy(
        borderWidth = 0.dp,
        borderColor = Color.Transparent,
        padding = PaddingValues(12.dp, 12.dp, 8.dp, 8.dp)
    )
    val btnDanger = btnPrimary.copy(
        background = Brush.verticalGradient(0f to colors.danger, 1f to colors.danger),
        foreground = colors.background
    )

    // --- Top App Bar (Spotify: 56dp height, surface background, no separator, title 22 SemiBold) ---
    val topAppBarProps = MillenniumTopAppBarThemeProperties(
        titleStyle = AppDefaultTypography.titleLarge,
        subtitleStyle = AppDefaultTypography.bodySmall.copy(color = muted),
        titleForeground = text,
        subtitleForeground = muted,
        navigationForeground = text,
        actionForeground = text,
        background = Brush.verticalGradient(0f to surface, 1f to surface),
        contentPadding = PaddingValues(16.dp, 16.dp, 0.dp, 0.dp),
        minimumHeight = 56.dp,
        navigationSpacing = 8.dp,
        actionSpacing = 4.dp,
        separatorColor = Color.Transparent
    )

    // --- Cards (Spotify: 8dp radius, surface bg, 1dp border on hover/focus, subtle shadow) ---
    val cardDefault = MillenniumCardThemeProperties(
        background = Brush.verticalGradient(0f to surface, 1f to surface),
        foreground = text,
        shape = AppDefaultShapes.small,
        maxWidth = 9999.dp,
        contentPadding = PaddingValues(0.dp),
        contentSpacing = 0.dp,
        contentTextStyle = AppDefaultTypography.bodyMedium,
        shadowElevation = 0.dp,
        shadowColor = Color.Transparent,
        borderColor = border,
        borderWidth = 0.dp,
        innerGlowColor = Color.Transparent,
        alpha = 1f,
        bottomRimColor = Color.Transparent
    )
    val cardHover = cardDefault.copy(
        borderWidth = 1.dp,
        borderColor = accent.copy(alpha = 0.5f),
        shadowElevation = 4.dp,
        shadowColor = scrim
    )
    val cardFocused = cardHover.copy(
        borderWidth = 2.dp,
        borderColor = accent
    )

    // --- Progress (Mini player progress bar: 2dp height, accent color, no track bg) ---
    val progressProps = MillenniumProgressThemeProperties(
        trackBackground = Brush.verticalGradient(0f to Color.Transparent, 1f to Color.Transparent),
        progressBackground = Brush.verticalGradient(0f to accent, 1f to accentTop),
        shape = RoundedCornerShape(1.dp),
        height = 2.dp,
        borderColor = Color.Transparent,
        borderWidth = 0.dp,
        innerGlowColor = accentTop.copy(alpha = 0.3f),
        determinateAnimationMillis = 200,
        indeterminateAnimationMillis = 1000,
        indeterminateFraction = 0.3f,
        alpha = 1f
    )

    // --- Icon Buttons (48dp touch target, 24dp icon, surface bg on hover) ---
    val iconBtnDefault = MillenniumIconButtonThemeProperties(
        background = Brush.verticalGradient(0f to Color.Transparent, 1f to Color.Transparent),
        foreground = text,
        shape = RoundedCornerShape(24.dp),
        size = DpSize(48.dp, 48.dp),
        iconSize = 24.dp,
        contentPadding = PaddingValues(12.dp),
        borderWidth = 0.dp,
        borderColor = Color.Transparent,
        shadows = listOf(Color.Transparent),
        shadowElevation = 0.dp,
        innerGlowColor = Color.Transparent,
        bottomRimColor = Color.Transparent,
        orb = false,
        orbColor = Color.Transparent,
        orbStrength = 0f,
        alpha = 1f,
        motion = org.spotube.millennium.composables.MillenniumButtonMotion()
    )
    val iconBtnHover = iconBtnDefault.copy(
        background = Brush.verticalGradient(0f to highlight, 1f to highlight)
    )
    val iconBtnStates = MillenniumControlStateProperty(
        default = iconBtnDefault, hovered = iconBtnHover, focused = iconBtnHover,
        pressed = iconBtnHover, disabled = iconBtnDefault.copy(alpha = 0.4f),
        selected = iconBtnHover, error = iconBtnDefault
    )

    // --- Modal Bottom Sheet (compact, 16dp horizontal, 10/8dp vertical) ---
    val modalBottomSheetTheme = compactModalBottomSheetTheme()

    // Build component set
    return base.copy(
        bottomNavigationBarTheme = MillenniumBottomNavigationBarTheme(
            states = MillenniumControlStateProperty(
                default = bnBarProps, hovered = bnBarProps, focused = bnBarProps,
                pressed = bnBarProps, disabled = bnBarProps, selected = bnBarProps, error = bnBarProps
            ),
            item = MillenniumControlStateProperty(
                default = bnItemDefault, hovered = bnItemSelected, focused = bnItemSelected,
                pressed = bnItemSelected, disabled = bnItemDefault, selected = bnItemSelected, error = bnItemDefault
            )
        ),
        buttonTheme = MillenniumButtonTheme(
            primary = MillenniumControlStateProperty(
                default = btnPrimary, hovered = btnPrimary.copy(background = Brush.verticalGradient(0f to accentTop, 1f to accent)),
                focused = btnPrimary.copy(borderWidth = 2.dp, borderColor = accent),
                pressed = btnPrimary.copy(background = Brush.verticalGradient(0f to accentPress, 1f to accent)),
                disabled = btnPrimary.copy(alpha = 0.4f),
                selected = btnPrimary, error = btnDanger
            ),
            secondary = MillenniumControlStateProperty(
                default = btnSecondary, hovered = btnSecondary.copy(background = Brush.verticalGradient(0f to highlight, 1f to highlight)),
                focused = btnSecondary.copy(borderWidth = 2.dp, borderColor = accent),
                pressed = btnSecondary.copy(background = Brush.verticalGradient(0f to border, 1f to border)),
                disabled = btnSecondary.copy(alpha = 0.4f),
                selected = btnSecondary, error = btnDanger
            ),
            quiet = MillenniumControlStateProperty(
                default = btnQuiet, hovered = btnQuiet.copy(background = Brush.verticalGradient(0f to highlight, 1f to highlight)),
                focused = btnQuiet.copy(borderWidth = 2.dp, borderColor = accent),
                pressed = btnQuiet.copy(background = Brush.verticalGradient(0f to border, 1f to border)),
                disabled = btnQuiet.copy(alpha = 0.4f),
                selected = btnQuiet, error = btnDanger
            ),
            danger = MillenniumControlStateProperty(
                default = btnDanger, hovered = btnDanger, focused = btnDanger, pressed = btnDanger,
                disabled = btnDanger.copy(alpha = 0.4f), selected = btnDanger, error = btnDanger
            )
        ),
        topAppBarTheme = MillenniumTopAppBarTheme(properties = topAppBarProps),
        cardTheme = MillenniumCardTheme(
            states = MillenniumControlStateProperty(
                default = cardDefault, hovered = cardHover, focused = cardFocused,
                pressed = cardHover, disabled = cardDefault.copy(alpha = 0.5f),
                selected = cardFocused, error = cardDefault.copy(borderColor = colors.danger, borderWidth = 2.dp)
            )
        ),
        progressTheme = MillenniumProgressTheme(states = MillenniumControlStateProperty(
            default = progressProps, hovered = progressProps, focused = progressProps,
            pressed = progressProps, disabled = progressProps.copy(alpha = 0.5f),
            selected = progressProps, error = progressProps
        )),
        iconButtonTheme = MillenniumIconButtonTheme(
            primary = iconBtnStates,
            secondary = iconBtnStates,
            quiet = iconBtnStates,
            danger = iconBtnStates
        ),
        modalBottomSheetTheme = modalBottomSheetTheme,
        // Keep other components from base
        textTheme = base.textTheme,
        textFieldTheme = base.textFieldTheme,
        sliderTheme = base.sliderTheme,
        checkboxTheme = base.checkboxTheme,
        triStateCheckboxTheme = base.triStateCheckboxTheme,
        toggleSwitchTheme = base.toggleSwitchTheme,
        radioGroupTheme = base.radioGroupTheme,
        tabGroupTheme = base.tabGroupTheme,
        avatarTheme = base.avatarTheme,
        iconTheme = base.iconTheme,
        disclosureTheme = base.disclosureTheme,
        dialogTheme = base.dialogTheme,
        dropdownMenuTheme = base.dropdownMenuTheme,
        tooltipTheme = base.tooltipTheme,
        loadingTheme = base.loadingTheme,
        scrollbarTheme = base.scrollbarTheme,
        separatorTheme = base.separatorTheme,
        bottomSheetTheme = base.bottomSheetTheme,
        drawerTheme = base.drawerTheme,
        sidebarTheme = base.sidebarTheme,
        autocompleteTheme = base.autocompleteTheme,
        listTileTheme = base.listTileTheme,
        scaffoldTheme = base.scaffoldTheme,
        segmentedButtonTheme = base.segmentedButtonTheme,
        snackbarTheme = base.snackbarTheme,
        pullToRefreshTheme = base.pullToRefreshTheme
    )
}

/**
 * The app's single source of truth for colour.
 *
 * Installs [MillenniumTheme] with the user's selected [UserSettings.palette] and light/dark
 * [UserSettings.theme], then derives the app's semantic [AppColors], [AppTypography] and
 * [AppShapes] tokens from the active Millennium theme.
 */
@Composable
fun SpotubeTheme(
    settings: UserSettings,
    content: @Composable () -> Unit
) {
    val isDarkTheme = when (settings.theme) {
        Theme.LIGHT -> false
        Theme.DARK -> true
        Theme.SYSTEM -> true // TODO: wire real system dark detection
    }
    val palette: MillenniumPalette = settings.palette
    val appFontFamily = FontFamily(
        Font(Res.font.Figtree_Variable, weight = FontWeight.Normal)
    )
    val appTypography = remember(appFontFamily) { spotifyTypography(appFontFamily) }

    MillenniumTheme(
        dark = isDarkTheme,
        config = MillenniumThemeConfig(
            palette = palette,
            colorSchemes = SpotifyColorSchemes,
            dark = spotifyMillenniumComponents(SpotifyDarkColors, true),
            light = spotifyMillenniumComponents(SpotifyLightColors, false)
        )
    ) {
        val colors = appColorsFrom(MillenniumTheme.colors)
        MillenniumTooltipHost {
            CompositionLocalProvider(
                LocalAppColors provides colors,
                LocalAppTypography provides appTypography,
                LocalAppShapes provides AppDefaultShapes,
                content = content
            )
        }
    }
}

/** Whether the active Millennium appearance is light. */
@Composable
fun isLightAppearance(): Boolean = MillenniumTheme.colors.background.luminance() > 0.5f

/** Library default is 24dp padding + 16dp spacing, which is far too roomy on phones. */
private fun compactModalBottomSheetTheme(): MillenniumModalBottomSheetTheme {
    val padding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 8.dp)
    fun org.spotube.millennium.composables.MillenniumModalBottomSheetThemeProperties.compact() =
        copy(contentPadding = padding, contentSpacing = 8.dp)
    val states = defaultMillenniumModalBottomSheetTheme().states
    return MillenniumModalBottomSheetTheme(
        states = MillenniumControlStateProperty(
            default = states.default.compact(),
            hovered = states.hovered.compact(),
            focused = states.focused.compact(),
            pressed = states.pressed.compact(),
            disabled = states.disabled.compact(),
            selected = states.selected.compact(),
            error = states.error.compact()
        )
    )
}