package org.spotube.spotube.core.automix

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack

data class AutoMixSession(
    val title: String,
    val tracks: List<MetadataTrack>
)

object AutoMixController {
    private val _visible = MutableStateFlow(false)
    val visible: StateFlow<Boolean> = _visible.asStateFlow()

    private val _session = MutableStateFlow<AutoMixSession?>(null)
    val session: StateFlow<AutoMixSession?> = _session.asStateFlow()

    private val _playbackActive = MutableStateFlow(false)
    val playbackActive: StateFlow<Boolean> = _playbackActive.asStateFlow()

    fun open(title: String, tracks: List<MetadataTrack>) {
        if (tracks.isEmpty()) return
        _session.value = AutoMixSession(title, tracks)
        _visible.value = true
    }

    fun show() {
        if (_session.value != null) _visible.value = true
    }

    fun hide() {
        _visible.value = false
    }

    fun clear() {
        _visible.value = false
        _session.value = null
        _playbackActive.value = false
    }

    fun setPlaybackActive(active: Boolean) {
        _playbackActive.value = active
    }
}
