package org.spotube.spotube.resources.iconsax

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
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

    /** Exact Spotify explicit badge (`ic_explicit`): 16dp gray square with a cut-out E. */
    val SpotifyExplicit: ImageVector
        get() {
            if (_SpotifyExplicit != null) return _SpotifyExplicit!!
            _SpotifyExplicit = ImageVector.Builder(
                name = "SpotifyExplicit",
                defaultWidth = 16.dp,
                defaultHeight = 16.dp,
                viewportWidth = 32f,
                viewportHeight = 32f
            ).apply {
                addPath(
                    pathData = addPathNodes(
                        "M4,0C1.791,0 0,1.791 0,4V28C0,30.209 1.791,32 4,32H28C30.209,32 32,30.209 32,28V4C32,1.791 30.209,0 28,0H4ZM22.222,22.192V25.778H10.667V6.222H22.222V9.808H14.258V14.276H21.467V17.669H14.258V22.192H22.222Z"
                    ),
                    pathFillType = PathFillType.EvenOdd,
                    fill = SolidColor(Color.Black)
                )
            }.build()
            return _SpotifyExplicit!!
        }

    @Suppress("ObjectPropertyName")
    private var _SpotifyExplicit: ImageVector? = null

    /** Exact Spotify logo glyph (`encore_icon_spotify_logo_24`). */
    val SpotifyLogo: ImageVector
        get() {
            if (_SpotifyLogo != null) return _SpotifyLogo!!
            _SpotifyLogo = ImageVector.Builder(
                name = "SpotifyLogo",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f
            ).apply {
                addPath(
                    pathData = addPathNodes(
                        "M12.438,1.009C6.368,0.769 1.251,5.494 1.008,11.565c-0.24,6.07 4.485,11.186 10.556,11.426 6.07,0.242 11.185,-4.484 11.427,-10.554S18.507,1.251 12.438,1.009m4.644,16.114a0.657,0.657 0,0 1,-0.897 0.246,13.2 13.2 0,0 0,-4.71 -1.602 13.2,13.2 0,0 0,-4.968 0.242,0.658 0.658 0,0 1,-0.31 -1.278 14.5,14.5 0,0 1,5.46 -0.265c1.837,0.257 3.579,0.851 5.177,1.76 0.315,0.178 0.425,0.58 0.246,0.896zM18.527,14.236a0.853,0.853 0,0 1,-1.158 0.344,16.2 16.2 0,0 0,-5.475 -1.797 16.2,16.2 0,0 0,-5.758 0.219,0.855 0.855 0,0 1,-1.018 -0.65 0.85,0.85 0,0 1,0.65 -1.018,17.9 17.9 0,0 1,6.362 -0.241 17.9,17.9 0,0 1,6.049 1.985c0.415,0.224 0.57,0.743 0.344,1.158zM20.129,10.981a1.05,1.05 0,0 1,-1.418 0.448,19.7 19.7 0,0 0,-6.341 -2.025 19.6,19.6 0,0 0,-6.655 0.199,1.05 1.05 0,1 1 -0.417,-2.06 21.7,21.7 0,0 1,7.364 -0.22,21.7 21.7 0,0 1,7.019 2.24c0.515,0.268 0.715,0.903 0.448,1.418"
                    ),
                    fill = SolidColor(Color.Black)
                )
            }.build()
            return _SpotifyLogo!!
        }

    @Suppress("ObjectPropertyName")
    private var _SpotifyLogo: ImageVector? = null

    /** Exact Spotify pin glyph (`encore_icon_pin_24`). */
    val SpotifyPin: ImageVector
        get() {
            if (_SpotifyPin != null) return _SpotifyPin!!
            _SpotifyPin = ImageVector.Builder(
                name = "SpotifyPin",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f
            ).apply {
                addPath(
                    pathData = addPathNodes(
                        "M13.158,2.089a3.72,3.72 0,0 1,5.258 0l3.495,3.495a3.72,3.72 0,0 1,0 5.258l-5.08,5.08 -1.83,6.831 -6.17,-6.17 -6.124,6.124a1,1 0,0 1,-1.414 -1.414l6.124,-6.124L1.247,9l6.83,-1.83z"
                    ),
                    fill = SolidColor(Color.Black)
                )
            }.build()
            return _SpotifyPin!!
        }

    @Suppress("ObjectPropertyName")
    private var _SpotifyPin: ImageVector? = null

    /** Exact Spotify mic glyph (`encore_icon_mic_24`), used for the Lyrics action. */
    val SpotifyLyrics: ImageVector
        get() {
            if (_SpotifyLyrics != null) return _SpotifyLyrics!!
            _SpotifyLyrics = ImageVector.Builder(
                name = "SpotifyLyrics",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f
            ).apply {
                addPath(
                    pathData = addPathNodes(
                        "M7,5a5,5 0,0 1,10 0v6a5,5 0,0 1,-10 0zM12,2a3,3 0,0 0,-3 3v6a3,3 0,1 0,6 0L15,5a3,3 0,0 0,-3 -3"
                    ),
                    fill = SolidColor(Color.Black)
                )
                addPath(
                    pathData = addPathNodes(
                        "M3,7a1,1 0,0 1,1 1v3a8,8 0,1 0,16 0V8a1,1 0,1 1,2 0v3c0,5.185 -3.947,9.449 -9,9.95V24h-2v-3.05c-5.053,-0.5 -9,-4.764 -9,-9.95V8a1,1 0,0 1,1 -1"
                    ),
                    fill = SolidColor(Color.Black)
                )
            }.build()
            return _SpotifyLyrics!!
        }

    @Suppress("ObjectPropertyName")
    private var _SpotifyLyrics: ImageVector? = null

    /** Exact Spotify share glyph (`encore_icon_share_android_24`). */
    val SpotifyShareGlyph: ImageVector
        get() {
            if (_SpotifyShareGlyph != null) return _SpotifyShareGlyph!!
            _SpotifyShareGlyph = ImageVector.Builder(
                name = "SpotifyShareGlyph",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f
            ).apply {
                addPath(
                    pathData = addPathNodes(
                        "M18.5,4a1.5,1.5 0,1 0,0 3,1.5 1.5,0 0,0 0,-3M15,5.5a3.5,3.5 0,1 1,1.006 2.455L9,12l7.006,4.045a3.5,3.5 0,1 1,-0.938 1.768l-6.67,-3.85a3.5,3.5 0,1 1,0 -3.924l6.67,-3.852A3.5,3.5 0,0 1,15 5.5m-9.5,5a1.5,1.5 0,1 0,0 3,1.5 1.5,0 0,0 0,-3m13,6.5a1.5,1.5 0,1 0,-0.001 3,1.5 1.5,0 0,0 0.001,-3"
                    ),
                    fill = SolidColor(Color.Black)
                )
            }.build()
            return _SpotifyShareGlyph!!
        }

    @Suppress("ObjectPropertyName")
    private var _SpotifyShareGlyph: ImageVector? = null

    /** Exact Spotify queue glyph (`encore_icon_queue_24`). */
    val SpotifyQueueGlyph: ImageVector
        get() {
            if (_SpotifyQueueGlyph != null) return _SpotifyQueueGlyph!!
            _SpotifyQueueGlyph = ImageVector.Builder(
                name = "SpotifyQueueGlyph",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f
            ).apply {
                addPath(
                    pathData = addPathNodes(
                        "M21,22L3,22v-2h18zM21,16L3,16v-2h18zM2.05,6.546a3.5,3.5 0,0 1,3.5 -3.5h13a3.5,3.5 0,0 1,0 7h-13a3.5,3.5 0,0 1,-3.5 -3.5m3.5,-1.5a1.5,1.5 0,0 0,0 3h13a1.5,1.5 0,0 0,0 -3z"
                    ),
                    fill = SolidColor(Color.Black)
                )
            }.build()
            return _SpotifyQueueGlyph!!
        }

    @Suppress("ObjectPropertyName")
    private var _SpotifyQueueGlyph: ImageVector? = null

    /** Exact Spotify back arrow (`encore_icon_arrow_left_24`). */
    val SpotifyArrowLeft: ImageVector
        get() {
            if (_SpotifyArrowLeft != null) return _SpotifyArrowLeft!!
            _SpotifyArrowLeft = ImageVector.Builder(
                name = "SpotifyArrowLeft",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f
            ).apply {
                addPath(
                    pathData = addPathNodes(
                        "M13.414,3.5a0.999,0.999 0,0 0,-1.707 -0.707l-9.207,9.2 9.207,9.202a1,1 0,1 0,1.414 -1.413L6.335,13H20.5a1,1 0,0 0,0 -2H6.322l6.799,-6.794a1,1 0,0 0,0.293 -0.707z"
                    ),
                    fill = SolidColor(Color.Black)
                )
            }.build()
            return _SpotifyArrowLeft!!
        }

    @Suppress("ObjectPropertyName")
    private var _SpotifyArrowLeft: ImageVector? = null

    /** Exact Spotify vertical overflow glyph (`encore_icon_more_android_24`). */
    val SpotifyMoreVertical: ImageVector
        get() {
            if (_SpotifyMoreVertical != null) return _SpotifyMoreVertical!!
            _SpotifyMoreVertical = ImageVector.Builder(
                name = "SpotifyMoreVertical",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f
            ).apply {
                addPath(
                    pathData = addPathNodes(
                        "M10.5,4.5a1.5,1.5 0,1 0,3 0,1.5 1.5,0 0,0 -3,0m0,15a1.5,1.5 0,1 0,3 0,1.5 1.5,0 0,0 -3,0m0,-7.5a1.5,1.5 0,1 0,3 0,1.5 1.5,0 0,0 -3,0"
                    ),
                    fill = SolidColor(Color.Black)
                )
            }.build()
            return _SpotifyMoreVertical!!
        }

    @Suppress("ObjectPropertyName")
    private var _SpotifyMoreVertical: ImageVector? = null

    /** Exact Spotify check glyph (`encore_icon_check_24`). */
    val SpotifyCheck: ImageVector
        get() {
            if (_SpotifyCheck != null) return _SpotifyCheck!!
            _SpotifyCheck = ImageVector.Builder(
                name = "SpotifyCheck",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f
            ).apply {
                addPath(
                    pathData = addPathNodes(
                        "M21.707,4.805a1,1 0,0 1,0 1.414L8.024,19.902l-5.731,-5.73a1,1 0,1 1,1.414 -1.415l4.317,4.317L20.293,4.805a1,1 0,0 1,1.414 0"
                    ),
                    fill = SolidColor(Color.Black)
                )
            }.build()
            return _SpotifyCheck!!
        }

    @Suppress("ObjectPropertyName")
    private var _SpotifyCheck: ImageVector? = null

    /** Exact Spotify download glyph (`encore_icon_download_24`). */
    val SpotifyDownload: ImageVector
        get() {
            if (_SpotifyDownload != null) return _SpotifyDownload!!
            _SpotifyDownload = ImageVector.Builder(
                name = "SpotifyDownload",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f
            ).apply {
                addPath(
                    pathData = addPathNodes(
                        "M12,3a9,9 0,1 0,0 18,9,9 0,0,0 0,-18M1,12C1,5.925 5.925,1 12,1s11,4.925 11,11 -4.925,11 -11,11S1,18.075 1,12"
                    ),
                    fill = SolidColor(Color.Black)
                )
                addPath(
                    pathData = addPathNodes(
                        "M12,6.05a1,1 0,0 1,1 1v7.486l1.793,-1.793a1,1 0,1 1,1.414 1.414L12,18.364l-4.207,-4.207a1,1 0,1 1,1.414 -1.414L11,14.536V7.05a1,1 0,0 1,1 -1"
                    ),
                    fill = SolidColor(Color.Black)
                )
            }.build()
            return _SpotifyDownload!!
        }

    @Suppress("ObjectPropertyName")
    private var _SpotifyDownload: ImageVector? = null
}
