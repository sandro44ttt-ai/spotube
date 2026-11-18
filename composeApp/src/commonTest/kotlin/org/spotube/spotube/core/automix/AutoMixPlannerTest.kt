package org.spotube.spotube.core.automix

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.spotube.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtist
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack

class AutoMixPlannerTest {
    @Test
    fun compatibleTracksBecomeMashup() {
        val current = track("a", 180_000L, "artist-a")
        val next = track("b", 180_000L, "artist-b")
        val plan = AutoMixPlanner.plan(
            current,
            next,
            features(120f, "8A", energy = 0.6f),
            features(122f, "8A", energy = 0.62f)
        )

        assertEquals(TransitionKind.Mashup, plan.kind)
        assertTrue(plan.bassSwap)
        assertTrue(plan.tempoRatio in 0.9f..1.1f)
        assertTrue(plan.durationMs >= 12_000L)
    }

    @Test
    fun incompatibleTracksUseSafeBlend() {
        val current = track("a", 180_000L, "artist-a")
        val next = track("b", 180_000L, "artist-b")
        val plan = AutoMixPlanner.plan(
            current,
            next,
            features(120f, "8A", energy = 0.6f),
            features(155f, "3B", energy = 0.6f)
        )

        assertEquals(TransitionKind.SafeBlend, plan.kind)
        assertEquals(false, plan.hardCut)
        assertEquals(1f, plan.tempoRatio)
    }

    @Test
    fun queuePrefersCompatibleBpm() {
        val current = track("a", 180_000L, "artist-a")
        val closer = track("b", 180_000L, "artist-b")
        val farther = track("c", 180_000L, "artist-c")
        val features = mapOf(
            "a" to features(120f, "8A"),
            "b" to features(122f, "8A"),
            "c" to features(150f, "3B")
        )

        val ordered = AutoMixQueueSelector.orderUpcoming(
            current = current,
            currentFeatures = features["a"],
            candidates = listOf(farther, closer),
            features = features,
            recentTrackIds = emptySet()
        )

        assertEquals("b", ordered.first().id)
    }

    private fun features(
        bpm: Float,
        key: String,
        energy: Float = 0.5f
    ) = AudioFeatures(
        bpm = bpm,
        key = key,
        energy = energy,
        mixInFraction = 0.2f,
        mixOutFraction = 0.8f,
        dropFraction = 0.25f,
        outroStartFraction = 0.8f,
        vocalness = 0.4f,
        confidence = 0.7f,
        analysisComplete = true
    )

    private fun track(id: String, durationMs: Long, artistId: String) = MetadataTrack(
        id = id,
        title = id,
        durationMs = durationMs,
        trackNumber = null,
        discNumber = null,
        artists = listOf(
            MetadataArtist.Basic(
                id = artistId,
                name = artistId,
                thumbnails = emptyList(),
                externalUri = null
            )
        ),
        album = null,
        thumbnails = null,
        explicit = null,
        popularity = null,
        isrcCode = null,
        externalUri = null
    )
}
