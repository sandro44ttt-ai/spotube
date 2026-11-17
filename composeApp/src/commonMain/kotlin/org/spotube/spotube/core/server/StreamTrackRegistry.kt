package org.spotube.spotube.core.server

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack

/**
 * Tracks that are played outside [org.spotube.spotube.core.audioplayer.AudioPlayerQueue],
 * such as Auto Mix decks. The local stream proxy resolves tracks by id from the main queue,
 * so external players must register their tracks here to obtain a stream URL.
 */
object StreamTrackRegistry {
    private val tracks = MutableStateFlow<Map<String, MetadataTrack>>(emptyMap())

    fun register(track: MetadataTrack) {
        tracks.update { current -> current + (track.id to track) }
    }

    fun registerAll(items: Collection<MetadataTrack>) {
        if (items.isEmpty()) return
        tracks.update { current -> current + items.associateBy { it.id } }
    }

    fun unregister(trackId: String) {
        tracks.update { current -> current - trackId }
    }

    fun unregisterAll(trackIds: Collection<String>) {
        if (trackIds.isEmpty()) return
        tracks.update { current -> current - trackIds.toSet() }
    }

    fun find(trackId: String): MetadataTrack? = tracks.value[trackId]
}
