package org.spotube.spotube.core.automix

import kotlinx.serialization.Serializable

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
    val sections: List<AudioSection> = emptyList(),
    val analysisComplete: Boolean = false,
    val analyzedAtMs: Long = 0L
)
