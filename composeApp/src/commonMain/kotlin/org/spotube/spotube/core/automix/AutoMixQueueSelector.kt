package org.spotube.spotube.core.automix

import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import kotlin.math.abs
import kotlin.math.min

object AutoMixQueueSelector {
    fun orderUpcoming(
        current: MetadataTrack?,
        currentFeatures: AudioFeatures?,
        candidates: List<MetadataTrack>,
        features: Map<String, AudioFeatures>,
        recentTrackIds: Set<String>
    ): List<MetadataTrack> {
        if (candidates.size <= 1) return candidates
        val currentArtist = current?.artists?.firstOrNull()?.id.orEmpty()
        return candidates.withIndex()
            .sortedWith(
                compareBy<IndexedValue<MetadataTrack>> {
                    score(
                        currentArtist,
                        currentFeatures,
                        it.value,
                        features[it.value.id],
                        recentTrackIds
                    )
                }.thenBy { it.index }
            )
            .map { it.value }
    }

    private fun score(
        currentArtist: String,
        currentFeatures: AudioFeatures?,
        candidate: MetadataTrack,
        candidateFeatures: AudioFeatures?,
        recentTrackIds: Set<String>
    ): Float {
        if (candidateFeatures == null || currentFeatures == null) return 300f
        var score = 0f
        score += bpmDistance(currentFeatures.bpm, candidateFeatures.bpm) * 55f
        score += keyDistance(currentFeatures.key, candidateFeatures.key) * 12f
        val targetEnergy = (currentFeatures.energy + 0.04f).coerceIn(0.15f, 0.95f)
        score += abs(targetEnergy - candidateFeatures.energy) * 18f
        val artist = candidate.artists.firstOrNull()?.id.orEmpty()
        if (artist.isNotEmpty() && artist == currentArtist) score += 28f
        if (candidate.id in recentTrackIds) score += 40f
        val vocalLoad = currentFeatures.vocalness + candidateFeatures.vocalness
        if (vocalLoad > 1.15f) score += 10f
        score -= candidateFeatures.confidence * 2f
        return score
    }

    fun bpmDistance(first: Float, second: Float): Float {
        if (first <= 0f || second <= 0f) return 0.5f
        val direct = abs(first - second) / first
        val half = abs(first - second * 2f) / first
        val double = abs(first - second / 2f) / first
        return min(direct, min(half, double))
    }

    fun keyDistance(first: String, second: String): Int {
        if (first.isBlank() || second.isBlank()) return 2
        val firstNumber = first.dropLast(1).toIntOrNull() ?: return 2
        val secondNumber = second.dropLast(1).toIntOrNull() ?: return 2
        val firstMode = first.last()
        val secondMode = second.last()
        if (firstMode == secondMode) {
            val difference = abs(firstNumber - secondNumber)
            return min(difference, 12 - difference)
        }
        return if (firstNumber == secondNumber) 0 else 2
    }

    fun tempoRatio(currentBpm: Float, nextBpm: Float): Float? {
        if (currentBpm <= 0f || nextBpm <= 0f) return null
        val candidates = listOf(
            currentBpm / nextBpm,
            currentBpm / (nextBpm * 2f),
            (currentBpm * 2f) / nextBpm
        )
        return candidates
            .filter { it in 0.9f..1.1f }
            .minByOrNull { abs(1f - it) }
    }
}
