package org.spotube.spotube.core.ui.component

import androidx.compose.runtime.Composable
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack

@Composable
expect fun AutoMixDialog(
    visible: Boolean,
    title: String,
    tracks: List<MetadataTrack>,
    onDismiss: () -> Unit
)
