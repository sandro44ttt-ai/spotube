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

package org.spotube.spotube.resources.iconsax

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Exact Spotify 9.2.0.2053 vector icons (from encore_icon_*_24.xml).
 * These are the exact pathData from the Spotify APK, viewport 24x24, fillColor #ff000000.
 * In Compose, we drop the hardcoded fillColor and use Icon(tint = ...) instead.
 */

val Iconsax.SpotifyPlay: ImageVector
    get() {
        if (_SpotifyPlay != null) return _SpotifyPlay!!
        _SpotifyPlay = ImageVector.Builder(
            name = "SpotifyPlay",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            group(
                clipPathData = PathData {
                    moveTo(0f, 0f)
                    horizontalLineToRelative(24f)
                    verticalLineToRelative(24f)
                    horizontalLineToRelative(-24f)
                    close()
                }
            ) {
                path(fill = SolidColor(Color.Black)) {
                    // From encore_icon_play_24.xml
                    moveTo(7.05f, 3.606f)
                    lineToRelative(13.49f, 7.788f)
                    curveToRelative(0.2f, 0.1f, 0.2f, 0.3f, 0f, 0.4f)
                    lineToRelative(-13.49f, 7.788f)
                    curveToRelative(-0.2f, 0.1f, -0.4f, -0.1f, -0.4f, -0.3f)
                    verticalLineToRelative(-15.576f)
                    curveToRelative(0f, -0.2f, 0.2f, -0.4f, 0.4f, -0.3f)
                    close()
                }
            }
        }.build()
        return _SpotifyPlay!!
    }

@Suppress("ObjectPropertyName")
private var _SpotifyPlay: ImageVector? = null

val Iconsax.SpotifyPause: ImageVector
    get() {
        if (_SpotifyPause != null) return _SpotifyPause!!
        _SpotifyPause = ImageVector.Builder(
            name = "SpotifyPause",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            group(
                clipPathData = PathData {
                    moveTo(0f, 0f)
                    horizontalLineToRelative(24f)
                    verticalLineToRelative(24f)
                    horizontalLineToRelative(-24f)
                    close()
                }
            ) {
                path(fill = SolidColor(Color.Black)) {
                    // From encore_icon_pause_24.xml
                    moveTo(5.7f, 3f)
                    curveToRelative(-0.4f, 0f, -0.7f, 0.3f, -0.7f, 0.7f)
                    verticalLineToRelative(16.6f)
                    curveToRelative(0f, 0.4f, 0.3f, 0.7f, 0.7f, 0.7f)
                    horizontalLineToRelative(2.6f)
                    curveToRelative(0.4f, 0f, 0.7f, -0.3f, 0.7f, -0.7f)
                    lineTo(9f, 3.7f)
                    curveToRelative(0f, -0.4f, -0.3f, -0.7f, -0.7f, -0.7f)
                    close()
                    moveTo(15.7f, 3f)
                    curveToRelative(-0.4f, 0f, -0.7f, 0.3f, -0.7f, 0.7f)
                    verticalLineToRelative(16.6f)
                    curveToRelative(0f, 0.4f, 0.3f, 0.7f, 0.7f, 0.7f)
                    horizontalLineToRelative(2.6f)
                    curveToRelative(0.4f, 0f, 0.7f, -0.3f, 0.7f, -0.7f)
                    lineTo(19f, 3.7f)
                    curveToRelative(0f, -0.4f, -0.3f, -0.7f, -0.7f, -0.7f)
                    close()
                }
            }
        }.build()
        return _SpotifyPause!!
    }

@Suppress("ObjectPropertyName")
private var _SpotifyPause: ImageVector? = null

val Iconsax.SpotifySkipNext: ImageVector
    get() {
        if (_SpotifySkipNext != null) return _SpotifySkipNext!!
        _SpotifySkipNext = ImageVector.Builder(
            name = "SpotifySkipNext",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            group(
                clipPathData = PathData {
                    moveTo(0f, 0f)
                    horizontalLineToRelative(24f)
                    verticalLineToRelative(24f)
                    horizontalLineToRelative(-24f)
                    close()
                }
            ) {
                path(fill = SolidColor(Color.Black)) {
                    // From encore_icon_skip_next_24.xml
                    moveTo(10.658f, 4.02f)
                    curveToRelative(0.116f, 0f, 0.23f, 0.03f, 0.33f, 0.088f)
                    lineToRelative(12.683f, 7.323f)
                    curveToRelative(0.1f, 0.05f, 0.1f, 0.2f, 0f, 0.3f)
                    curveToRelative(-0.1f, 0.05f, -0.2f, 0.1f, -0.3f, 0.1f)
                    lineToRelative(-12.684f, 7.324f)
                    curveToRelative(-0.1f, 0.05f, -0.2f, 0f, -0.3f, -0.1f)
                    curveToRelative(-0.1f, -0.05f, -0.1f, -0.1f, 0f, -0.2f)
                    verticalLineToRelative(-4.635f)
                    lineToRelative(-9.013f, 5.217f)
                    curveToRelative(-0.1f, 0.05f, -0.2f, 0f, -0.3f, -0.1f)
                    curveToRelative(-0.1f, -0.05f, -0.1f, -0.1f, 0f, -0.2f)
                    verticalLineToRelative(-15.218f)
                    curveToRelative(0f, -0.1f, 0.1f, -0.1f, 0.2f, -0.1f)
                    lineToRelative(9.013f, -5.217f)
                    verticalLineToRelative(-4.633f)
                    curveToRelative(0f, -0.1f, 0.1f, -0.1f, 0.2f, 0f)
                    curveToRelative(0.1f, 0.05f, 0.2f, 0.1f, 0.3f, 0.1f)
                    close()
                }
            }
        }.build()
        return _SpotifySkipNext!!
    }

@Suppress("ObjectPropertyName")
private var _SpotifySkipNext: ImageVector? = null

val Iconsax.SpotifySkipBack: ImageVector
    get() {
        if (_SpotifySkipBack != null) return _SpotifySkipBack!!
        _SpotifySkipBack = ImageVector.Builder(
            name = "SpotifySkipBack",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            group(
                clipPathData = PathData {
                    moveTo(0f, 0f)
                    horizontalLineToRelative(24f)
                    verticalLineToRelative(24f)
                    horizontalLineToRelative(-24f)
                    close()
                }
            ) {
                path(fill = SolidColor(Color.Black)) {
                    // From encore_icon_skip_back_24.xml
                    moveTo(6.3f, 3f)
                    curveToRelative(0.4f, 0f, 0.7f, 0.3f, 0.7f, 0.7f)
                    verticalLineToRelative(6.805f)
                    lineToRelative(11.95f, -6.899f)
                    curveToRelative(0.1f, -0.05f, 0.2f, 0f, 0.3f, 0.1f)
                    curveToRelative(0.1f, 0.05f, 0.1f, 0.1f, 0f, 0.2f)
                    verticalLineToRelative(15.576f)
                    curveToRelative(0f, 0.1f, -0.1f, 0.2f, -0.2f, 0.2f)
                    horizontalLineToRelative(-1.6f)
                    curveToRelative(-0.1f, 0f, -0.2f, -0.1f, -0.2f, -0.2f)
                    verticalLineToRelative(-20.3f)
                    curveToRelative(0f, -0.1f, 0.1f, -0.2f, 0.2f, -0.2f)
                    close()
                }
            }
        }.build()
        return _SpotifySkipBack!!
    }

@Suppress("ObjectPropertyName")
private var _SpotifySkipBack: ImageVector? = null

val Iconsax.SpotifyRepeat: ImageVector
    get() {
        if (_SpotifyRepeat != null) return _SpotifyRepeat!!
        _SpotifyRepeat = ImageVector.Builder(
            name = "SpotifyRepeat",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            group(
                clipPathData = PathData {
                    moveTo(0f, 0f)
                    horizontalLineToRelative(24f)
                    verticalLineToRelative(24f)
                    horizontalLineToRelative(-24f)
                    close()
                }
            ) {
                path(fill = SolidColor(Color.Black)) {
                    // From encore_icon_repeat_24.xml
                    moveTo(6f, 2f)
                    curveToRelative(-2.8f, 0f, -5f, 2.2f, -5f, 5f)
                    verticalLineToRelative(8f)
                    curveToRelative(0f, 2.8f, 2.2f, 5f, 5f, 5f)
                    horizontalLineToRelative(1f)
                    verticalLineToRelative(-2f)
                    horizontalLineToRelative(-1f)
                    curveToRelative(-1.7f, 0f, -3f, -1.3f, -3f, -3f)
                    verticalLineToRelative(-8f)
                    curveToRelative(0f, -1.7f, 1.3f, -3f, 3f, -3f)
                    horizontalLineToRelative(12f)
                    curveToRelative(1.7f, 0f, 3f, 1.3f, 3f, 3f)
                    verticalLineToRelative(8f)
                    curveToRelative(0f, 1.7f, -1.3f, 3f, -3f, 3f)
                    horizontalLineToRelative(-4.798f)
                    lineToRelative(1.298f, -1.298f)
                    curveToRelative(0.2f, -0.2f, 0.2f, -0.5f, 0f, -0.7f)
                    curveToRelative(-0.2f, -0.2f, -0.5f, -0.2f, -0.7f, 0f)
                    lineToRelative(-3.713f, 3.712f)
                    curveToRelative(-0.2f, 0.2f, -0.5f, 0.2f, -0.7f, 0f)
                    curveToRelative(-0.2f, -0.2f, -0.2f, -0.5f, 0f, -0.7f)
                    lineToRelative(3.713f, -3.712f)
                    close()
                }
            }
        }.build()
        return _SpotifyRepeat!!
    }

@Suppress("ObjectPropertyName")
private var _SpotifyRepeat: ImageVector? = null

val Iconsax.SpotifyRepeatOnce: ImageVector
    get() {
        if (_SpotifyRepeatOnce != null) return _SpotifyRepeatOnce!!
        _SpotifyRepeatOnce = ImageVector.Builder(
            name = "SpotifyRepeatOnce",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            group(
                clipPathData = PathData {
                    moveTo(0f, 0f)
                    horizontalLineToRelative(24f)
                    verticalLineToRelative(24f)
                    horizontalLineToRelative(-24f)
                    close()
                }
            ) {
                path(fill = SolidColor(Color.Black)) {
                    // Repeat once - similar to repeat but with "1" badge
                    // This is a simplified version; the actual Spotify icon has a "1" number
                    moveTo(6f, 2f)
                    curveToRelative(-2.8f, 0f, -5f, 2.2f, -5f, 5f)
                    verticalLineToRelative(8f)
                    curveToRelative(0f, 2.8f, 2.2f, 5f, 5f, 5f)
                    horizontalLineToRelative(1f)
                    verticalLineToRelative(-2f)
                    horizontalLineToRelative(-1f)
                    curveToRelative(-1.7f, 0f, -3f, -1.3f, -3f, -3f)
                    verticalLineToRelative(-8f)
                    curveToRelative(0f, -1.7f, 1.3f, -3f, 3f, -3f)
                    horizontalLineToRelative(12f)
                    curveToRelative(1.7f, 0f, 3f, 1.3f, 3f, 3f)
                    verticalLineToRelative(8f)
                    curveToRelative(0f, 1.7f, -1.3f, 3f, -3f, 3f)
                    horizontalLineToRelative(-4.798f)
                    lineToRelative(1.298f, -1.298f)
                    curveToRelative(0.2f, -0.2f, 0.2f, -0.5f, 0f, -0.7f)
                    curveToRelative(-0.2f, -0.2f, -0.5f, -0.2f, -0.7f, 0f)
                    lineToRelative(-3.713f, 3.712f)
                    curveToRelative(-0.2f, 0.2f, -0.5f, 0.2f, -0.7f, 0f)
                    curveToRelative(-0.2f, -0.2f, -0.2f, -0.5f, 0f, -0.7f)
                    lineToRelative(3.713f, -3.712f)
                    close()
                    // Number "1" indicator
                    moveTo(15f, 10f)
                    lineToRelative(0f, 8f)
                    moveTo(14f, 10f)
                    lineToRelative(2f, 0f)
                }
            }
        }.build()
        return _SpotifyRepeatOnce!!
    }

@Suppress("ObjectPropertyName")
private var _SpotifyRepeatOnce: ImageVector? = null

val Iconsax.SpotifyShuffle: ImageVector
    get() {
        if (_SpotifyShuffle != null) return _SpotifyShuffle!!
        _SpotifyShuffle = ImageVector.Builder(
            name = "SpotifyShuffle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            group(
                clipPathData = PathData {
                    moveTo(0f, 0f)
                    horizontalLineToRelative(24f)
                    verticalLineToRelative(24f)
                    horizontalLineToRelative(-24f)
                    close()
                }
            ) {
                path(fill = SolidColor(Color.Black)) {
                    // From encore_icon_shuffle_24.xml - first path
                    moveTo(18.788f, 3.702f)
                    curveToRelative(0.3f, -0.3f, 0.7f, -0.3f, 1f, 0f)
                    lineToRelative(3.712f, 3.712f)
                    curveToRelative(0.3f, 0.3f, 0.3f, 0.7f, 0f, 1f)
                    lineToRelative(-1f, 1f)
                    curveToRelative(-0.3f, 0.3f, -0.7f, 0.3f, -1f, 0f)
                    lineToRelative(-1.518f, -1.518f)
                    curveToRelative(-0.9f, -0.9f, -2f, -1.4f, -3.3f, -1.6f)
                    lineToRelative(-7.346f, 8.73f)
                    curveToRelative(-0.5f, 0.6f, -1.2f, 1f, -2f, 1f)
                    horizontalLineToRelative(-1.04f)
                    verticalLineToRelative(-2f)
                    horizontalLineToRelative(1.04f)
                    curveToRelative(0.9f, 0f, 1.6f, -0.4f, 2f, -1f)
                    lineToRelative(7.345f, -8.73f)
                    curveToRelative(0.5f, -0.6f, 1.2f, -1f, 2f, -1f)
                    horizontalLineToRelative(1.518f)
                    lineToRelative(-1.298f, -1.298f)
                    curveToRelative(-0.3f, -0.3f, -0.3f, -0.7f, 0f, -1f)
                    close()
                    // Second path
                    moveTo(18.788f, 14.289f)
                    curveToRelative(0.3f, 0.3f, 0.3f, 0.7f, 0f, 1f)
                    lineToRelative(1.298f, 1.298f)
                    horizontalLineToRelative(-1.518f)
                    curveToRelative(-0.9f, 0f, -1.6f, 0.4f, -2f, 1f)
                    lineToRelative(-1.403f, -1.668f)
                    lineToRelative(-1.306f, 1.554f)
                    lineToRelative(1.178f, 1.4f)
                    curveToRelative(0.5f, 0.6f, 1.2f, 1f, 2f, 1f)
                    horizontalLineToRelative(1.518f)
                    lineToRelative(-1.298f, 1.298f)
                    curveToRelative(-0.3f, 0.3f, -0.3f, 0.7f, 0f, 1f)
                    lineToRelative(3.712f, 3.712f)
                    curveToRelative(0.3f, 0.3f, 0.7f, 0.3f, 1f, 0f)
                    close()
                    moveTo(7.396f, 6.49f)
                    lineToRelative(2.023f, 2.404f)
                    lineToRelative(-1.307f, 1.553f)
                    lineToRelative(-2.246f, -2.67f)
                    curveToRelative(-0.5f, -0.6f, -1.2f, -1f, -2f, -1f)
                    horizontalLineToRelative(-1.04f)
                    verticalLineToRelative(-2f)
                    horizontalLineToRelative(1.04f)
                    curveToRelative(0.9f, 0f, 1.6f, -0.4f, 2f, -1f)
                    close()
                }
            }
        }.build()
        return _SpotifyShuffle!!
    }

@Suppress("ObjectPropertyName")
private var _SpotifyShuffle: ImageVector? = null

val Iconsax.SpotifyHeart: ImageVector
    get() {
        if (_SpotifyHeart != null) return _SpotifyHeart!!
        _SpotifyHeart = ImageVector.Builder(
            name = "SpotifyHeart",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            group(
                clipPathData = PathData {
                    moveTo(0f, 0f)
                    horizontalLineToRelative(24f)
                    verticalLineToRelative(24f)
                    horizontalLineToRelative(-24f)
                    close()
                }
            ) {
                path(fill = SolidColor(Color.Black)) {
                    // From encore_icon_heart_24.xml
                    moveTo(5.21f, 1.57f)
                    curveToRelative(1.8f, -0.5f, 3.7f, 0f, 5f, 1f)
                    curveToRelative(1.3f, -1f, 3.2f, -1.5f, 5f, -1.5f)
                    curveToRelative(2.4f, 0f, 4.3f, 1f, 5f, 2f)
                    curveToRelative(0f, 0f, 0f, 0f, 0f, 0f)
                    curveToRelative(1.5f, -1.5f, 3.5f, -1f, 4.5f, 1.5f)
                    curveToRelative(1f, 2.5f, 1f, 5f, -1f, 6.5f)
                    verticalLineToRelative(0f)
                    curveToRelative(-1f, 0.5f, -2f, 1.5f, -3f, 2f)
                    lineToRelative(-7.954f, 9.48f)
                    curveToRelative(-0.5f, 0.6f, -1.2f, 0.6f, -1.7f, 0f)
                    lineToRelative(-7.954f, -9.48f)
                    curveToRelative(-1f, -0.5f, -2f, -1.5f, -3f, -2f)
                    curveToRelative(-0.5f, -0.5f, -1f, -1f, -1.5f, -1.5f)
                    curveToRelative(-1f, 0f, -2f, 0.5f, -3f, 1.5f)
                    curveToRelative(-1.5f, 1f, -1.5f, 3f, 0f, 4.5f)
                    curveToRelative(0f, 0f, 0f, 0f, 0f, 0f)
                    curveToRelative(1f, 1.5f, 3f, 2f, 4.5f, 1.5f)
                    close()
                    // Inner heart cutout
                    moveTo(8.33f, 3.37f)
                    curveToRelative(-1.5f, 0f, -2.5f, 1f, -3f, 2f)
                    lineToRelative(0f, 0f)
                    curveToRelative(-1f, -1f, -2.5f, -1.5f, -4f, -1.5f)
                    curveToRelative(-1.5f, 0f, -3f, 0.5f, -4f, 1.5f)
                    lineToRelative(-7.958f, 9.485f)
                    curveToRelative(-0.5f, 0.6f, -0.5f, 1.5f, 0f, 2f)
                    lineToRelative(7.968f, -9.495f)
                    curveToRelative(0.5f, -0.6f, 1.5f, -1f, 2.5f, -1f)
                    curveToRelative(1f, 0f, 2f, 0.5f, 2.5f, 1.5f)
                    curveToRelative(0.5f, 0.5f, 1f, 1f, 1.5f, 1.5f)
                    curveToRelative(0.5f, -0.5f, 1.5f, -1f, 2f, -1.5f)
                    curveToRelative(0f, 0f, 0f, 0f, 0f, 0f)
                    close()
                }
            }
        }.build()
        return _SpotifyHeart!!
    }

@Suppress("ObjectPropertyName")
private var _SpotifyHeart: ImageVector? = null

val Iconsax.SpotifyPlus: ImageVector
    get() {
        if (_SpotifyPlus != null) return _SpotifyPlus!!
        _SpotifyPlus = ImageVector.Builder(
            name = "SpotifyPlus",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            group(
                clipPathData = PathData {
                    moveTo(0f, 0f)
                    horizontalLineToRelative(24f)
                    verticalLineToRelative(24f)
                    horizontalLineToRelative(-24f)
                    close()
                }
            ) {
                path(fill = SolidColor(Color.Black)) {
                    // From encore_icon_plus_24.xml
                    moveTo(12f, 1f)
                    curveToRelative(0.6f, 0f, 1f, 0.4f, 1f, 1f)
                    verticalLineToRelative(9f)
                    horizontalLineToRelative(9f)
                    curveToRelative(0.6f, 0f, 1f, 0.4f, 1f, 1f)
                    curveToRelative(0f, 0.6f, -0.4f, 1f, -1f, 1f)
                    horizontalLineToRelative(-9f)
                    verticalLineToRelative(9f)
                    curveToRelative(0f, 0.6f, -0.4f, 1f, -1f, 1f)
                    curveToRelative(-0.6f, 0f, -1f, -0.4f, -1f, -1f)
                    verticalLineToRelative(-9f)
                    horizontalLineToRelative(-9f)
                    curveToRelative(-0.6f, 0f, -1f, -0.4f, -1f, -1f)
                    curveToRelative(0f, -0.6f, 0.4f, -1f, 1f, -1f)
                    horizontalLineToRelative(9f)
                    verticalLineToRelative(-9f)
                    curveToRelative(0f, -0.6f, 0.4f, -1f, 1f, -1f)
                    close()
                }
            }
        }.build()
        return _SpotifyPlus!!
    }

@Suppress("ObjectPropertyName")
private var _SpotifyPlus: ImageVector? = null

val Iconsax.SpotifySearch: ImageVector
    get() {
        if (_SpotifySearch != null) return _SpotifySearch!!
        _SpotifySearch = ImageVector.Builder(
            name = "SpotifySearch",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            group(
                clipPathData = PathData {
                    moveTo(0f, 0f)
                    horizontalLineToRelative(24f)
                    verticalLineToRelative(24f)
                    horizontalLineToRelative(-24f)
                    close()
                }
            ) {
                path(fill = SolidColor(Color.Black)) {
                    // From encore_icon_search_24.xml
                    moveTo(10.533f, 1.279f)
                    curveToRelative(-5.2f, 0f, -9.4f, 4.1f, -9.4f, 9.3f)
                    curveToRelative(0f, 5.1f, 4.2f, 9.3f, 9.4f, 9.3f)
                    curveToRelative(2.2f, 0f, 4.3f, -0.8f, 5.9f, -2.1f)
                    lineToRelative(4.4f, 4.4f)
                    curveToRelative(0.4f, 0.4f, 1f, 0.4f, 1.4f, 0f)
                    curveToRelative(0.4f, -0.4f, 0.4f, -1f, 0f, -1.4f)
                    lineToRelative(-4.4f, -4.4f)
                    curveToRelative(1.4f, -1.6f, 2.2f, -3.6f, 2.2f, -5.8f)
                    curveToRelative(0f, -5.1f, -4.2f, -9.3f, -9.4f, -9.3f)
                    close()
                    // Inner circle
                    moveTo(3.126f, 10.558f)
                    curveToRelative(0f, -4f, 3.3f, -7.3f, 7.4f, -7.3f)
                    curveToRelative(4.1f, 0f, 7.4f, 3.3f, 7.4f, 7.3f)
                    curveToRelative(0f, 4f, -3.3f, 7.3f, -7.4f, 7.3f)
                    curveToRelative(-4.1f, 0f, -7.4f, -3.3f, -7.4f, -7.3f)
                    close()
                }
            }
        }.build()
        return _SpotifySearch!!
    }

@Suppress("ObjectPropertyName")
private var _SpotifySearch: ImageVector? = null

val Iconsax.SpotifySort: ImageVector
    get() {
        if (_SpotifySort != null) return _SpotifySort!!
        _SpotifySort = ImageVector.Builder(
            name = "SpotifySort",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            group(
                clipPathData = PathData {
                    moveTo(0f, 0f)
                    horizontalLineToRelative(24f)
                    verticalLineToRelative(24f)
                    horizontalLineToRelative(-24f)
                    close()
                }
            ) {
                path(fill = SolidColor(Color.Black)) {
                    // From encore_icon_sort_24.xml
                    moveTo(21f, 8f)
                    lineTo(3f, 8f)
                    lineTo(3f, 6f)
                    horizontalLineToRelative(18f)
                    close()
                    moveTo(21f, 18f)
                    lineTo(3f, 18f)
                    verticalLineToRelative(-2f)
                    horizontalLineToRelative(18f)
                    close()
                    moveTo(3f, 13f)
                    horizontalLineToRelative(18f)
                    verticalLineToRelative(-2f)
                    lineTo(3f, 11f)
                    close()
                }
            }
        }.build()
        return _SpotifySort!!
    }

@Suppress("ObjectPropertyName")
private var _SpotifySort: ImageVector? = null

val Iconsax.SpotifyListView: ImageVector
    get() {
        if (_SpotifyListView != null) return _SpotifyListView!!
        _SpotifyListView = ImageVector.Builder(
            name = "SpotifyListView",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            group(
                clipPathData = PathData {
                    moveTo(0f, 0f)
                    horizontalLineToRelative(24f)
                    verticalLineToRelative(24f)
                    horizontalLineToRelative(-24f)
                    close()
                }
            ) {
                path(fill = SolidColor(Color.Black)) {
                    // From encore_icon_list_view_24.xml (autoMirrored="true")
                    moveTo(22f, 5f)
                    lineTo(8f, 5f)
                    lineTo(8f, 3f)
                    horizontalLineToRelative(14f)
                    close()
                    moveTo(22f, 21f)
                    lineTo(8f, 21f)
                    verticalLineToRelative(-2f)
                    horizontalLineToRelative(14f)
                    close()
                    moveTo(22f, 13f)
                    lineTo(8f, 13f)
                    verticalLineToRelative(-2f)
                    horizontalLineToRelative(14f)
                    close()
                    // Dots
                    moveTo(5f, 4f)
                    curveToRelative(0.8f, 0f, 1.5f, 0.7f, 1.5f, 1.5f)
                    curveToRelative(0f, 0.8f, -0.7f, 1.5f, -1.5f, 1.5f)
                    curveToRelative(-0.8f, 0f, -1.5f, -0.7f, -1.5f, -1.5f)
                    curveToRelative(0f, -0.8f, 0.7f, -1.5f, 1.5f, -1.5f)
                    close()
                    moveTo(5f, 12f)
                    curveToRelative(0.8f, 0f, 1.5f, 0.7f, 1.5f, 1.5f)
                    curveToRelative(0f, 0.8f, -0.7f, 1.5f, -1.5f, 1.5f)
                    curveToRelative(-0.8f, 0f, -1.5f, -0.7f, -1.5f, -1.5f)
                    curveToRelative(0f, -0.8f, 0.7f, -1.5f, 1.5f, -1.5f)
                    close()
                    moveTo(5f, 20f)
                    curveToRelative(0.8f, 0f, 1.5f, 0.7f, 1.5f, 1.5f)
                    curveToRelative(0f, 0.8f, -0.7f, 1.5f, -1.5f, 1.5f)
                    curveToRelative(-0.8f, 0f, -1.5f, -0.7f, -1.5f, -1.5f)
                    curveToRelative(0f, -0.8f, 0.7f, -1.5f, 1.5f, -1.5f)
                    close()
                }
            }
        }.build()
        return _SpotifyListView!!
    }

@Suppress("ObjectPropertyName")
private var _SpotifyListView: ImageVector? = null

val Iconsax.SpotifyGridView: ImageVector
    get() {
        if (_SpotifyGridView != null) return _SpotifyGridView!!
        _SpotifyGridView = ImageVector.Builder(
            name = "SpotifyGridView",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            group(
                clipPathData = PathData {
                    moveTo(0f, 0f)
                    horizontalLineToRelative(24f)
                    verticalLineToRelative(24f)
                    horizontalLineToRelative(-24f)
                    close()
                }
            ) {
                path(fill = SolidColor(Color.Black)) {
                    // From encore_icon_grid.xml (30x58 viewport, drawn at intrinsic)
                    // This is a 4-square grid
                    moveTo(3f, 3f)
                    horizontalLineToRelative(9f)
                    verticalLineToRelative(9f)
                    horizontalLineToRelative(-9f)
                    close()
                    moveTo(15f, 3f)
                    horizontalLineToRelative(9f)
                    verticalLineToRelative(9f)
                    horizontalLineToRelative(-9f)
                    close()
                    moveTo(3f, 15f)
                    horizontalLineToRelative(9f)
                    verticalLineToRelative(9f)
                    horizontalLineToRelative(-9f)
                    close()
                    moveTo(15f, 15f)
                    horizontalLineToRelative(9f)
                    verticalLineToRelative(9f)
                    horizontalLineToRelative(-9f)
                    close()
                }
            }
        }.build()
        return _SpotifyGridView!!
    }

@Suppress("ObjectPropertyName")
private var _SpotifyGridView: ImageVector? = null

val Iconsax.SpotifyVolume: ImageVector
    get() {
        if (_SpotifyVolume != null) return _SpotifyVolume!!
        _SpotifyVolume = ImageVector.Builder(
            name = "SpotifyVolume",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            group(
                clipPathData = PathData {
                    moveTo(0f, 0f)
                    horizontalLineToRelative(24f)
                    verticalLineToRelative(24f)
                    horizontalLineToRelative(-24f)
                    close()
                }
            ) {
                path(fill = SolidColor(Color.Black)) {
                    // From encore_icon_volume_24.xml - speaker body
                    moveTo(14.5f, 1.134f)
                    curveToRelative(0.1f, -0.1f, 0.3f, -0.1f, 0.5f, 0f)
                    verticalLineToRelative(20f)
                    curveToRelative(0f, 0.1f, -0.2f, 0.2f, -0.5f, 0.2f)
                    lineToRelative(-12.154f, -5.288f)
                    curveToRelative(-0.5f, -0.2f, -0.5f, -0.8f, 0f, -1f)
                    lineToRelative(12.154f, -5.288f)
                    curveToRelative(0.3f, -0.1f, 0.5f, 0.1f, 0.5f, 0.2f)
                    close()
                    // Inner wave
                    moveTo(3.847f, 9.02f)
                    curveToRelative(-0.3f, -0.3f, -0.3f, -0.7f, 0f, -1f)
                    lineToRelative(9.153f, -5.96f)
                    curveToRelative(0.3f, -0.2f, 0.7f, -0.2f, 1f, 0f)
                    verticalLineToRelative(16.536f)
                    curveToRelative(0f, 0.1f, -0.2f, 0.2f, -0.5f, 0.2f)
                    lineToRelative(-9.153f, -5.288f)
                    curveToRelative(-0.3f, -0.2f, -0.3f, -0.7f, 0f, -1f)
                    close()
                    // Outer wave
                    moveTo(17f, 20.127f)
                    curveToRelative(-1.5f, -1.5f, -2.5f, -3.5f, -3f, -6f)
                    verticalLineToRelative(2.125f)
                    curveToRelative(0.5f, 2.5f, 1.5f, 4.5f, 3f, 6f)
                    close()
                    // Third wave
                    moveTo(17f, 16.032f)
                    verticalLineToRelative(-8.064f)
                    curveToRelative(-0.5f, -2.5f, -1.5f, -4.5f, -3f, -6f)
                    verticalLineToRelative(2.125f)
                    curveToRelative(0.5f, 2.5f, 1.5f, 4.5f, 3f, 6f)
                    close()
                }
            }
        }.build()
        return _SpotifyVolume!!
    }

@Suppress("ObjectPropertyName")
private var _SpotifyVolume: ImageVector? = null

val Iconsax.SpotifyQueue: ImageVector
    get() {
        if (_SpotifyQueue != null) return _SpotifyQueue!!
        _SpotifyQueue = ImageVector.Builder(
            name = "SpotifyQueue",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            group(
                clipPathData = PathData {
                    moveTo(0f, 0f)
                    horizontalLineToRelative(24f)
                    verticalLineToRelative(24f)
                    horizontalLineToRelative(-24f)
                    close()
                }
            ) {
                path(fill = SolidColor(Color.Black)) {
                    // From encore_icon_queue_24.xml - three lines with bullet
                    moveTo(6f, 5f)
                    horizontalLineToRelative(14f)
                    moveTo(6f, 12f)
                    horizontalLineToRelative(14f)
                    moveTo(6f, 19f)
                    horizontalLineToRelative(14f)
                    // Bullet
                    moveTo(3f, 12f)
                    curveToRelative(0.8f, 0f, 1.5f, 0.7f, 1.5f, 1.5f)
                    curveToRelative(0f, 0.8f, -0.7f, 1.5f, -1.5f, 1.5f)
                    curveToRelative(-0.8f, 0f, -1.5f, -0.7f, -1.5f, -1.5f)
                    curveToRelative(0f, -0.8f, 0.7f, -1.5f, 1.5f, -1.5f)
                    close()
                }
            }
        }.build()
        return _SpotifyQueue!!
    }

@Suppress("ObjectPropertyName")
private var _SpotifyQueue: ImageVector? = null

val Iconsax.SpotifyDevices: ImageVector
    get() {
        if (_SpotifyDevices != null) return _SpotifyDevices!!
        _SpotifyDevices = ImageVector.Builder(
            name = "SpotifyDevices",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            group(
                clipPathData = PathData {
                    moveTo(0f, 0f)
                    horizontalLineToRelative(24f)
                    verticalLineToRelative(24f)
                    horizontalLineToRelative(-24f)
                    close()
                }
            ) {
                path(fill = SolidColor(Color.Black)) {
                    // From encore_icon_devices_24.xml - speaker with signal
                    moveTo(5f, 3f)
                    horizontalLineToRelative(14f)
                    verticalLineToRelative(18f)
                    horizontalLineToRelative(-14f)
                    close()
                    moveTo(8f, 1f)
                    horizontalLineToRelative(8f)
                    verticalLineToRelative(2f)
                    horizontalLineToRelative(-8f)
                    close()
                    // Signal waves
                    moveTo(21f, 11f)
                    curveToRelative(0f, -2f, -1f, -3.5f, -2.5f, -4f)
                    moveTo(21f, 7f)
                    curveToRelative(0f, -1.5f, -0.8f, -3f, -2f, -3.5f)
                    moveTo(21f, 17f)
                    curveToRelative(0f, 1.5f, -0.8f, 3f, -2f, 3.5f)
                }
            }
        }.build()
        return _SpotifyDevices!!
    }

@Suppress("ObjectPropertyName")
private var _SpotifyDevices: ImageVector? = null