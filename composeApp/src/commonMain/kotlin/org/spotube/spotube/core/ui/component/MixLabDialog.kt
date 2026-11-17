package org.spotube.spotube.core.ui.component

import androidx.compose.runtime.Composable

@Composable
expect fun MixLabDialog(
    visible: Boolean,
    onDismiss: () -> Unit
)
