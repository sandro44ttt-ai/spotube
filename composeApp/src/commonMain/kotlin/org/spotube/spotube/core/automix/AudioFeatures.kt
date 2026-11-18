package org.spotube.spotube.core.automix

import kotlinx.serialization.Serializable
import kotlin.math.sqrt

@Serializable
enum class SectionType {
    Intro,
    Verse,
    Chorus,
    Breakdown,
    Drop,
    Outro,
    Unknown
}

@Serializable
data class AudioSection(
    val startFraction: Float,
    val endFraction: Float,
    val type: SectionType,
    val energy: Float,
    val vocalness: Float
)

@Serializable
data class VibeProfile(
    val brightness: Float = 0.5f,
    val density: Float = 0.5f,
    val percussiveness: Float = 0.5f,
    val roughness: Float = 0.5f,
    val warmth: Float = 0.5f,
    val acousticness: Float = 0.5f,
    val dynamicRange: Float = 0.5f,
    val valence: Float = 0.5f,
    val danceability: Float = 0.5f
) {
    fun distance(other: VibeProfile): Float {
        var weighted = 0f
        var weight = 0f
        fun add(first: Float, second: Float, factor: Float) {
            val delta = (first - second).coerceIn(-1f, 1f)
            weighted += delta * delta * factor
            weight += factor
        }
        add(brightness, other.brightness, 1.2f)
        add(density, other.density, 1.0f)
        add(percussiveness, other.percussiveness, 1.0f)
        add(roughness, other.roughness, 0.8f)
        add(warmth, other.warmth, 0.9f)
        add(acousticness, other.acousticness, 0.8f)
        add(dynamicRange, other.dynamicRange, 0.6f)
        add(valence, other.valence, 0.7f)
        add(danceability, other.danceability, 0.7f)
        return sqrt(weighted / weight.coerceAtLeast(1f)).coerceIn(0f, 1f)
    }
}

@Serializable
data class AudioFeatures(
    val bpm: Float = 0f,
    val bpmConfidence: Float = 0.5f,
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
    val swingRatio: Float = 0.5f,
    val transientOffsetMs: Float = 0f,
    val downbeatConfidence: Float = 0.5f,
    val vibe: VibeProfile = VibeProfile(),
    val sections: List<AudioSection> = emptyList(),
    val analysisComplete: Boolean = false,
    val analyzedAtMs: Long = 0L
)
