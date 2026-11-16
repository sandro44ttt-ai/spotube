package org.spotube.spotube.core.ui.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import coil3.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal expect fun dominantColorFromImageBlocking(image: Image): Color?

@Composable
fun rememberDominantColor(image: Image?): Color? {
    var color by remember(image) { mutableStateOf<Color?>(null) }
    LaunchedEffect(image) {
        color = if (image == null) {
            null
        } else {
            withContext(Dispatchers.Default) {
                dominantColorFromImageBlocking(image)
            }
        }
    }
    return color
}
