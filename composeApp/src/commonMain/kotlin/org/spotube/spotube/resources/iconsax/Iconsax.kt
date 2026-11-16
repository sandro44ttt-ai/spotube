package org.spotube.spotube.resources.iconsax

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object Iconsax {
    // Spotify 9.2.0.2053 icons mapped to existing Iconsax icons
    val SpotifyPlay: ImageVector
        get() = IconsaxPlay

    val SpotifyPause: ImageVector
        get() = IconsaxPause

    val SpotifySkipNext: ImageVector
        get() = IconsaxNext

    val SpotifySkipBack: ImageVector
        get() = IconsaxPrevious

    val SpotifyRepeat: ImageVector
        get() = IconsaxRepeatMusic

    val SpotifyRepeatOnce: ImageVector
        get() = IconsaxRepeateOne

    val SpotifyShuffle: ImageVector
        get() = IconsaxShuffle

    val SpotifyHeart: ImageVector
        get() = IconsaxHeart

    val SpotifyPlus: ImageVector
        get() = IconsaxAdd

    val SpotifyAddSquare: ImageVector
        get() = IconsaxAddSquare

    val SpotifySearch: ImageVector
        get() = IconsaxSearch

    val SpotifySort: ImageVector
        get() = IconsaxSort

    val SpotifyVolume: ImageVector
        get() = IconsaxVolumeHigh

    val SpotifyVolumeLow: ImageVector
        get() = IconsaxVolumeLow

    val SpotifyVolumeCross: ImageVector
        get() = IconsaxVolumeCross

    val SpotifyQueue: ImageVector
        get() = IconsaxMusicPlaylist // queue uses playlist icon

    val SpotifyShare: ImageVector
        get() = IconsaxShare

    val SpotifyMore: ImageVector
        get() = Iconsax3DotsMore

    val SpotifyClose: ImageVector
        get() = IconsaxCloseSquare

    val SpotifyRefresh: ImageVector
        get() = IconsaxRefreshRight

    val SpotifyHome: ImageVector
        get() = IconsaxHome

    val SpotifySettings: ImageVector
        get() = IconsaxSetting

    /**
     * Exact "Your Library" glyph from Spotify 9.2 (`encore_icon_collection_active_24`):
     * a bookshelf/stack of three items.
     */
    val SpotifyLibrary: ImageVector
        get() {
            if (_SpotifyLibrary != null) return _SpotifyLibrary!!
            _SpotifyLibrary = ImageVector.Builder(
                name = "SpotifyLibrary",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f
            ).apply {
                addPath(
                    pathData = addPathNodes(
                        "M3,22a1,1 0,0 1,-1 -1V3a1,1 0,0 1,2 0v18a1,1 0,0 1,-1 1" +
                            "M15.5,2.134A1,1 0,0 0,14 3v18a1,1 0,0 0,1 1h6a1,1 0,0 0,1 -1V6.464a1,1 0,0 0,-0.5 -0.866z" +
                            "M9,2a1,1 0,0 0,-1 1v18a1,1 0,1 0,2 0V3a1,1 0,0 0,-1 -1"
                    ),
                    fill = SolidColor(Color.Black)
                )
            }.build()
            return _SpotifyLibrary!!
        }

    @Suppress("ObjectPropertyName")
    private var _SpotifyLibrary: ImageVector? = null
}
