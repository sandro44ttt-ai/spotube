package org.spotube.spotube.core.ui.media

import androidx.compose.ui.graphics.Color
import coil3.BitmapImage
import coil3.Image
import kotlin.math.min

internal actual fun dominantColorFromImageBlocking(image: Image): Color? =
    runCatching { dominantColorPixels(image) }.getOrNull()

private fun dominantColorPixels(image: Image): Color? {
    val bitmap = (image as? BitmapImage)?.bitmap ?: return null
    val width = bitmap.width
    val height = bitmap.height
    if (width <= 0 || height <= 0) return null

    val step = (min(width, height) / 24).coerceAtLeast(1)
    val buckets = HashMap<Int, LongArray>()
    var y = 0
    while (y < height) {
        var x = 0
        while (x < width) {
            val pixel = bitmap.getPixel(x, y)
            val alpha = (pixel ushr 24) and 0xFF
            val red = (pixel shr 16) and 0xFF
            val green = (pixel shr 8) and 0xFF
            val blue = pixel and 0xFF
            val max = maxOf(red, green, blue)
            val min = minOf(red, green, blue)
            if (alpha >= 128 && max in 40..244 && max - min >= 12) {
                val key = ((red / 32) shl 10) or ((green / 32) shl 5) or (blue / 32)
                val bucket = buckets.getOrPut(key) { LongArray(4) }
                bucket[0] += red
                bucket[1] += green
                bucket[2] += blue
                bucket[3] += 1
            }
            x += step
        }
        y += step
    }

    val best = buckets.values.maxByOrNull { it[3] } ?: return null
    val count = best[3]
    if (count < 8L) return null
    return Color(
        (best[0] / count).toInt().coerceIn(0, 255),
        (best[1] / count).toInt().coerceIn(0, 255),
        (best[2] / count).toInt().coerceIn(0, 255)
    )
}
