package org.spotube.spotube.resources.iconsax

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object Iconsax {
    // Spotify 9.2.0.2053 icons mapped to existing Iconsax icons

    /** Exact Spotify play glyph (`encore_icon_play_24`): sharp triangle. */
    val SpotifyPlay: ImageVector
        get() {
            if (_SpotifyPlay != null) return _SpotifyPlay!!
            _SpotifyPlay = ImageVector.Builder(
                name = "SpotifyPlay",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f
            ).apply {
                addPath(
                    pathData = addPathNodes(
                        "m7.05,3.606 l13.49,7.788a0.7,0.7 0,0 1,0 1.212L7.05,20.394A0.7,0.7 0,0 1,6 19.788V4.212a0.7,0.7 0,0 1,1.05 -0.606"
                    ),
                    fill = SolidColor(Color.Black)
                )
            }.build()
            return _SpotifyPlay!!
        }

    @Suppress("ObjectPropertyName")
    private var _SpotifyPlay: ImageVector? = null

    /** Exact Spotify pause glyph (`encore_icon_pause_24`): two sharp bars. */
    val SpotifyPause: ImageVector
        get() {
            if (_SpotifyPause != null) return _SpotifyPause!!
            _SpotifyPause = ImageVector.Builder(
                name = "SpotifyPause",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f
            ).apply {
                addPath(
                    pathData = addPathNodes(
                        "M5.7,3a0.7,0.7 0,0 0,-0.7 0.7v16.6a0.7,0.7 0,0 0,0.7 0.7h2.6a0.7,0.7 0,0 0,0.7 -0.7L9,3.7a0.7,0.7 0,0 0,-0.7 -0.7zM15.7,3a0.7,0.7 0,0 0,-0.7 0.7v16.6a0.7,0.7 0,0 0,0.7 0.7h2.6a0.7,0.7 0,0 0,0.7 -0.7L19,3.7a0.7,0.7 0,0 0,-0.7 -0.7z"
                    ),
                    fill = SolidColor(Color.Black)
                )
            }.build()
            return _SpotifyPause!!
        }

    @Suppress("ObjectPropertyName")
    private var _SpotifyPause: ImageVector? = null

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
        get() = SpotifyHomeGlyph

    val SpotifySearchGlyph: ImageVector
        get() {
            if (_SpotifySearchGlyph != null) return _SpotifySearchGlyph!!
            _SpotifySearchGlyph = ImageVector.Builder(
                name = "SpotifySearchGlyph",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f
            ).apply {
                addPath(
                    pathData = addPathNodes(
                        "M10.533,1.279C5.352,1.279 1.126,5.419 1.126,10.558C1.126,15.697 5.352,19.837 10.533,19.837C12.767,19.837 14.823,19.067 16.44,17.779L20.793,22.132C21.183,22.523 21.817,22.523 22.207,22.132C22.598,21.742 22.598,21.108 22.207,20.718L17.863,16.374C19.162,14.785 19.94,12.763 19.94,10.558C19.94,5.419 15.714,1.279 10.533,1.279ZM3.126,10.558C3.126,6.552 6.428,3.279 10.533,3.279C14.638,3.279 17.94,6.552 17.94,10.558C17.94,14.564 14.638,17.837 10.533,17.837C6.428,17.837 3.126,14.564 3.126,10.558Z"
                    ),
                    fill = SolidColor(Color.Black)
                )
            }.build()
            return _SpotifySearchGlyph!!
        }

    @Suppress("ObjectPropertyName")
    private var _SpotifySearchGlyph: ImageVector? = null

    /** Exact Spotify home glyph (`encore_icon_home_24`). */
    val SpotifyHomeGlyph: ImageVector
        get() {
            if (_SpotifyHomeGlyph != null) return _SpotifyHomeGlyph!!
            _SpotifyHomeGlyph = ImageVector.Builder(
                name = "SpotifyHomeGlyph",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f
            ).apply {
                addPath(
                    pathData = addPathNodes(
                        "M12.5,3.247a1,1 0,0 0,-1 0L4,7.577L4,20h4.5v-6a1,1 0,0 1,1 -1h5a1,1 0,0 1,1 1v6L20,20L20,7.577zM10.5,1.515a3,3 0,0 1,3 0l7.5,4.33a2,2 0,0 1,1 1.732L22,21a1,1 0,0 1,-1 1h-6.5a1,1 0,0 1,-1 -1v-6h-3v6a1,1 0,0 1,-1 1L3,22a1,1 0,0 1,-1 -1L2,7.577a2,2 0,0 1,1 -1.732z"
                    ),
                    fill = SolidColor(Color.Black)
                )
            }.build()
            return _SpotifyHomeGlyph!!
        }

    @Suppress("ObjectPropertyName")
    private var _SpotifyHomeGlyph: ImageVector? = null

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
