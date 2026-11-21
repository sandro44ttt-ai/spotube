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
        recentTrackIds: Set<String>,
        currentPitchSemitones: Int = 0
    ): List<MetadataTrack> {
        if (candidates.size <= 1) return candidates
        val currentArtist = current?.artists?.firstOrNull()?.id.orEmpty()
        return candidates.withIndex()
            .sortedWith(
                compareBy<IndexedValue<MetadataTrack>> {
                    score(
                        current,
                        currentArtist,
                        currentFeatures,
                        currentPitchSemitones,
                        it.value,
                        features[it.value.id],
                        recentTrackIds
                    )
                }.thenBy { it.index }
            )
            .map { it.value }
    }

    fun pairScore(
        current: MetadataTrack?,
        currentFeatures: AudioFeatures?,
        candidate: MetadataTrack,
        candidateFeatures: AudioFeatures?,
        recentTrackIds: Set<String> = emptySet(),
        currentPitchSemitones: Int = 0
    ): Float = score(
        current,
        current?.artists?.firstOrNull()?.id.orEmpty(),
        currentFeatures,
        currentPitchSemitones,
        candidate,
        candidateFeatures,
        recentTrackIds
    )

    private fun score(
        current: MetadataTrack?,
        currentArtist: String,
        currentFeatures: AudioFeatures?,
        currentPitch: Int,
        candidate: MetadataTrack,
        candidateFeatures: AudioFeatures?,
        recentTrackIds: Set<String>
    ): Float {
        if (candidateFeatures == null || currentFeatures == null) return 300f
        val shift = pitchShiftFor(currentFeatures, currentPitch, candidateFeatures) ?: return 900f
        var score = 0f
        val currentKey = camelotFor(
            currentFeatures.keyRoot + currentPitch,
            currentFeatures.keyMinor
        )
        val candidateKey = camelotFor(
            candidateFeatures.keyRoot + shift,
            candidateFeatures.keyMinor
        )
        score += bpmDistance(currentFeatures.bpm, candidateFeatures.bpm) * 55f
        val directBpmDistance = if (currentFeatures.bpm > 0f && candidateFeatures.bpm > 0f) {
            abs(currentFeatures.bpm - candidateFeatures.bpm) / currentFeatures.bpm
        } else {
            0.5f
        }
        if (directBpmDistance > 0.10f) score += 90f
        val harmonicDistance = keyDistance(currentKey, candidateKey)
        score += harmonicDistance * 70f
        if (harmonicDistance == 1) score += 25f
        if (harmonicDistance > 1) score += 600f
        score += abs(shift) * 10f
        val targetEnergy = (currentFeatures.energy + 0.04f).coerceIn(0.15f, 0.95f)
        score += abs(targetEnergy - candidateFeatures.energy) * 18f
        val vibeDistance = currentFeatures.vibe.distance(candidateFeatures.vibe)
        score += vibeDistance * 90f
        if (vibeDistance > 0.32f) score += 55f
        val bassDistance = currentFeatures.bassProfile.distance(candidateFeatures.bassProfile)
        val drumDistance = currentFeatures.drumProfile.distance(candidateFeatures.drumProfile)
        val grooveDistance = currentFeatures.grooveProfile.distance(candidateFeatures.grooveProfile)
        val timbreDistance = currentFeatures.timbreProfile.distance(candidateFeatures.timbreProfile)
        score += bassDistance * 28f
        score += drumDistance * 22f
        score += grooveDistance * 18f
        score += timbreDistance * 16f
        val swingDiff = abs(currentFeatures.swingRatio - candidateFeatures.swingRatio)
        if (swingDiff > 0.18f) score += 120f else score += swingDiff * 40f
        if (tempoRatio(currentFeatures.bpm, candidateFeatures.bpm) == null) score += 80f
        val artist = candidate.artists.firstOrNull()?.id.orEmpty()
        if (artist.isNotEmpty() && artist == currentArtist) score += 28f
        if (candidate.id in recentTrackIds) score += 40f
        val vocalLoad = currentFeatures.vocalness + candidateFeatures.vocalness
        if (vocalLoad > 1.15f) score += 140f
        if (!candidateFeatures.analysisComplete) score += 60f
        score += (1f - minOf(currentFeatures.bpmConfidence, candidateFeatures.bpmConfidence)) * 25f
        score -= candidateFeatures.confidence * 2f
        if (current != null) {
            val plan = AutoMixPlanner.plan(
                current = current,
                next = candidate,
                currentFeatures = currentFeatures,
                nextFeatures = candidateFeatures,
                currentPitchSemitones = currentPitch
            )
            if (plan.level == null) {
                score += 110f
            } else {
                score -= 30f
                if (plan.kind == TransitionKind.Mashup) score -= 25f
                if (plan.earlyCut) score -= 15f
            }
            val currentDurationMs = if (
                currentFeatures.analysisComplete &&
                currentFeatures.durationSeconds > 20f
            ) {
                (currentFeatures.durationSeconds * 1000f).toLong()
            } else {
                current.durationMs
            }
            if (plan.outgoingCueMs + plan.durationMs + 500L > currentDurationMs) {
                score += 180f
            }
        }
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

    fun pitchShiftFor(
        current: AudioFeatures,
        currentPitch: Int,
        candidate: AudioFeatures
    ): Int? {
        val currentKey = camelotFor(current.keyRoot + currentPitch, current.keyMinor)
        val vocalHeavy = current.vocalness + candidate.vocalness > 1.0f
        val maxShift = if (vocalHeavy) 1 else 2
        var bestShift: Int? = null
        var bestDistance = Int.MAX_VALUE
        for (shift in -maxShift..maxShift) {
            val candidateKey = camelotFor(candidate.keyRoot + shift, candidate.keyMinor)
            val distance = keyDistance(currentKey, candidateKey)
            if (distance < bestDistance || (distance == bestDistance && abs(shift) < abs(bestShift ?: 0))) {
                bestDistance = distance
                bestShift = shift
            }
        }
        return if (bestDistance <= 1) bestShift else null
    }

    fun camelotFor(root: Int, minor: Boolean): String {
        val normalized = ((root % 12) + 12) % 12
        return if (minor) {
            "${MINOR_CAMELOT[normalized]}A"
        } else {
            "${MAJOR_CAMELOT[normalized]}B"
        }
    }

    private val MAJOR_CAMELOT = intArrayOf(8, 3, 10, 5, 12, 7, 2, 9, 4, 11, 6, 1)
    private val MINOR_CAMELOT = intArrayOf(5, 12, 7, 2, 9, 4, 11, 6, 1, 8, 3, 10)
}
