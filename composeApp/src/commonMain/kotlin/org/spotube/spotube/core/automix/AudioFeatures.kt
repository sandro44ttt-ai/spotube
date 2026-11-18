package org.spotube.spotube.core.automix

import kotlinx.serialization.Serializable

@Serializable
data class AudioFeatures(
    val bpm: Float = 0f,
    val beatOffsetSeconds: Float = 0f,
    val key: String = "",
    val keyRoot: Int = 0,
    val keyMinor: Boolean = false,
    val energy: Float = 0.5f,
    val mixInFraction: Float = 0.12f,
    val mixOutFraction: Float = 0.82f,
    val introEndFraction: Float = 0.08f,
    val outroStartFraction: Float = 0.78f,
    val dropFraction: Float = 0.28f,
    val vocalness: Float = 0.5f,
    val confidence: Float = 0.5f,
    val durationSeconds: Float = 0f,
    val analyzedAtMs: Long = 0L
)
