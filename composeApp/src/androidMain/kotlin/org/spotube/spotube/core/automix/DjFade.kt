package org.spotube.spotube.core.automix

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal object DjFade {
    private const val HALF_PI = (PI / 2.0).toFloat()

    fun smoothstep(value: Float): Float {
        val x = value.coerceIn(0f, 1f)
        return x * x * (3f - 2f * x)
    }

    fun window(t: Float, start: Float, end: Float): Float {
        if (end <= start) return if (t < start) 0f else 1f
        return ((t - start) / (end - start)).coerceIn(0f, 1f)
    }

    fun fadeOut(t: Float, start: Float, end: Float): Float =
        1f - smoothstep(window(t, start, end))

    fun fadeIn(t: Float, start: Float, end: Float): Float =
        smoothstep(window(t, start, end))

    fun equalPowerOut(t: Float): Float =
        cos(t.coerceIn(0f, 1f) * HALF_PI)

    fun equalPowerIn(t: Float): Float =
        sin(t.coerceIn(0f, 1f) * HALF_PI)

    fun equalPowerOut(t: Float, start: Float, end: Float): Float =
        equalPowerOut(window(t, start, end))

    fun equalPowerIn(t: Float, start: Float, end: Float): Float =
        equalPowerIn(window(t, start, end))
}
