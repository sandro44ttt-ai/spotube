package org.spotube.spotube.core.automix

import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class AutoMixLevel { Safe, Mid, Bold }

enum class TransitionKind {
    HardCut,
    SafeBlend,
    BeatBlend,
    TempoBlend,
    BassSwap,
    LoopBridge,
    Mashup
}

data class LoopSpec(
    val startMs: Long,
    val endMs: Long,
    val bars: Int
)

data class TransitionPlan(
    val kind: TransitionKind,
    val outgoingCueMs: Long,
    val incomingCueMs: Long,
    val durationMs: Long,
    val tempoRatio: Float,
    val pitchSemitones: Int = 0,
    val hardCut: Boolean = false,
    val bassSwap: Boolean = false,
    val bassOwnerIsA: Boolean = true,
    val secondaryHighPassHz: Float = 140f,
    val midDipDb: Float = 0f,
    val centerAttenuationDb: Float = 0f,
    val headroomDb: Float = -6f,
    val incomingOffsetMs: Long = 0L,
    val loopIn: LoopSpec? = null,
    val bars: Int = 8,
    val level: AutoMixLevel = AutoMixLevel.Safe,
    val loopBeats: Int = 0,
    val loopRoll: Boolean = false,
    val loopBpm: Float = 0f,
    val reason: String = ""
)

object AutoMixPlanner {
    fun plan(
        current: MetadataTrack,
        next: MetadataTrack,
        currentFeatures: AudioFeatures?,
        nextFeatures: AudioFeatures?,
        activeTempo: Float = 1f,
        currentPitchSemitones: Int = 0
    ): TransitionPlan {
        val currentBpm = currentFeatures?.bpm?.times(activeTempo) ?: 0f
        val nextBpm = nextFeatures?.bpm ?: 0f
        val beatMs = if (currentBpm > 0f) 60_000f / currentBpm else 500f
        val barMs = beatMs * 4f
        val currentDuration = current.durationMs.coerceAtLeast(1L)
        val nextDuration = next.durationMs.coerceAtLeast(1L)
        val seed = (((current.id + next.id).hashCode() and 0x7FFFFFFF) / 10) % 4

        val outgoingCue = outgoingCueMs(currentDuration, currentFeatures, barMs)
        val nextBarMs = if (nextBpm > 0f) 60_000f / nextBpm * 4f else barMs
        val incomingCue = incomingCueMs(nextDuration, nextFeatures, nextBarMs)
        val tempoRatio = AutoMixQueueSelector.tempoRatio(currentBpm, nextBpm)
        val pitchShift = if (currentFeatures != null && nextFeatures != null) {
            AutoMixQueueSelector.pitchShiftFor(
                currentFeatures,
                currentPitchSemitones,
                nextFeatures
            )
        } else {
            null
        }
        val currentKey = if (currentFeatures != null) {
            AutoMixQueueSelector.camelotFor(
                currentFeatures.keyRoot + currentPitchSemitones,
                currentFeatures.keyMinor
            )
        } else {
            ""
        }
        val nextKey = if (nextFeatures != null) {
            AutoMixQueueSelector.camelotFor(
                nextFeatures.keyRoot + (pitchShift ?: 0),
                nextFeatures.keyMinor
            )
        } else {
            ""
        }
        val keyDistance = AutoMixQueueSelector.keyDistance(currentKey, nextKey)
        val energyDistance = if (currentFeatures != null && nextFeatures != null) {
            abs(currentFeatures.energy - nextFeatures.energy)
        } else {
            1f
        }
        val vocalA = currentFeatures?.vocalness ?: 0.5f
        val vocalB = nextFeatures?.vocalness ?: 0.5f
        val swingDiff = if (currentFeatures != null && nextFeatures != null) {
            abs(currentFeatures.swingRatio - nextFeatures.swingRatio)
        } else {
            1f
        }
        val bothLong = current.durationMs > 60_000L && next.durationMs > 60_000L
        val fullFeatures = currentFeatures?.analysisComplete == true &&
            nextFeatures?.analysisComplete == true
        val vocalConflict = vocalA > 0.65f && vocalB > 0.65f
        val bpmConfidence = minOf(
            currentFeatures?.bpmConfidence ?: 0f,
            nextFeatures?.bpmConfidence ?: 0f
        )
        val compatibleBase = bothLong &&
            fullFeatures &&
            tempoRatio != null &&
            pitchShift != null &&
            keyDistance <= 1 &&
            energyDistance < 0.55f &&
            bpmConfidence >= 0.55f &&
            !vocalConflict
        val absPitch = abs(pitchShift ?: 0)

        val level = when {
            compatibleBase &&
                swingDiff <= 0.18f &&
                energyDistance < 0.5f &&
                (absPitch <= 2) -> AutoMixLevel.Bold

            compatibleBase &&
                swingDiff <= 0.14f &&
                absPitch <= 1 -> AutoMixLevel.Mid

            compatibleBase &&
                swingDiff <= 0.08f &&
                absPitch == 0 -> AutoMixLevel.Safe

            else -> null
        }

        if (level != null) {
            val bars = when (level) {
                AutoMixLevel.Safe -> 8
                AutoMixLevel.Mid -> 16
                AutoMixLevel.Bold -> if (seed % 2 == 0) 16 else 32
            }
            val duration = (bars * barMs).coerceIn(
                if (level == AutoMixLevel.Safe) 12_000f else 16_000f,
                if (level == AutoMixLevel.Bold) 40_000f else 32_000f
            )
            val ownerIsA = vocalA < vocalB
            val loopBeats = when (level) {
                AutoMixLevel.Safe -> 0
                AutoMixLevel.Mid -> 8
                AutoMixLevel.Bold -> 4
            }
            val midDip = when (level) {
                AutoMixLevel.Safe -> 0f
                AutoMixLevel.Mid -> 4.5f
                AutoMixLevel.Bold -> 5.5f
            }
            val center = when (level) {
                AutoMixLevel.Safe -> 0f
                AutoMixLevel.Mid -> 3f
                AutoMixLevel.Bold -> 4.5f
            }
            return TransitionPlan(
                kind = TransitionKind.Mashup,
                outgoingCueMs = outgoingCue,
                incomingCueMs = incomingCue,
                durationMs = duration.toLong(),
                tempoRatio = tempoRatio ?: 1f,
                pitchSemitones = pitchShift ?: 0,
                bassSwap = true,
                bassOwnerIsA = ownerIsA,
                secondaryHighPassHz = if (level == AutoMixLevel.Safe) 110f else 140f,
                midDipDb = midDip,
                centerAttenuationDb = center,
                headroomDb = if (level == AutoMixLevel.Safe) -4f else -6f,
                incomingOffsetMs = transientOffsetMs(currentFeatures, nextFeatures),
                bars = bars,
                level = level,
                loopBeats = loopBeats,
                loopRoll = level == AutoMixLevel.Bold && seed % 2 == 1,
                loopBpm = nextBpm * (tempoRatio ?: 1f),
                reason = "mashup ${level.name.lowercase()} ${bars}b: " +
                    "${currentBpm.roundToInt()}?${nextBpm.roundToInt()} BPM, " +
                    "pitch ${pitchShift ?: 0}, key $keyDistance, " +
                    "bass ${if (ownerIsA) "A" else "B"}, " +
                    "swing ${(swingDiff * 100).roundToInt()}%"
            )
        }
        val canBlend = tempoRatio != null &&
            pitchShift != null &&
            keyDistance <= 1 &&
            bpmConfidence >= 0.4f &&
            energyDistance < 0.65f
        if (canBlend) {
            val bars = if (seed % 2 == 0) 8 else 16
            val duration = (bars * barMs).coerceIn(16_000f, 32_000f)
            val ownerIsA = vocalA < vocalB
            return TransitionPlan(
                kind = if (tempoRatio != null) TransitionKind.TempoBlend else TransitionKind.BeatBlend,
                outgoingCueMs = outgoingCue,
                incomingCueMs = incomingCue,
                durationMs = duration.toLong(),
                tempoRatio = tempoRatio ?: 1f,
                pitchSemitones = pitchShift ?: 0,
                bassSwap = energyDistance > 0.3f,
                bassOwnerIsA = ownerIsA,
                secondaryHighPassHz = if (energyDistance > 0.3f) 130f else 80f,
                midDipDb = if (vocalConflict) 3f else 0f,
                centerAttenuationDb = if (vocalConflict) 2f else 0f,
                headroomDb = -5f,
                incomingOffsetMs = transientOffsetMs(currentFeatures, nextFeatures),
                bars = bars,
                reason = "blend ${bars}b: ${currentBpm.roundToInt()}→${nextBpm.roundToInt()} BPM, " +
                    "pitch ${pitchShift ?: 0}, key $keyDistance, " +
                    "bass ${if (ownerIsA) "A" else "B"}"
            )
        }

        val bars = 8
        return TransitionPlan(
            kind = TransitionKind.SafeBlend,
            outgoingCueMs = outgoingCue,
            incomingCueMs = incomingCue,
            durationMs = (bars * barMs).coerceIn(12_000f, 24_000f).toLong(),
            tempoRatio = 1f,
            pitchSemitones = 0,
            bassSwap = true,
            bassOwnerIsA = vocalA < vocalB,
            secondaryHighPassHz = 150f,
            midDipDb = 5f,
            centerAttenuationDb = 4f,
            headroomDb = -6f,
            incomingOffsetMs = transientOffsetMs(currentFeatures, nextFeatures),
            bars = bars,
            reason = "safe blend ${bars}b: key $keyDistance, BPM " +
                "${currentBpm.roundToInt()}→${nextBpm.roundToInt()}"
        )
    }

    private fun transientOffsetMs(
        current: AudioFeatures?,
        next: AudioFeatures?
    ): Long {
        if (current == null || next == null) return 0L
        return (next.transientOffsetMs - current.transientOffsetMs)
            .coerceIn(-35f, 35f)
            .toLong()
    }

    private fun outgoingCueMs(
        durationMs: Long,
        features: AudioFeatures?,
        barMs: Float
    ): Long {
        if (durationMs <= 30_000L) {
            return max(0L, durationMs - 6_000L)
        }
        val target = if (features != null) {
            val outro = features.sections
                .firstOrNull { it.type == SectionType.Outro }
                ?.startFraction
            if (outro != null) {
                (durationMs * outro).toLong()
            } else {
                val lastHigh = features.sections
                    .filter { it.type == SectionType.Chorus || it.type == SectionType.Drop }
                    .maxByOrNull { it.endFraction }
                if (lastHigh != null) {
                    val raw = (durationMs * lastHigh.endFraction).toLong()
                    max(raw - (2 * barMs).toLong(), (durationMs * 0.65f).toLong())
                } else {
                    (durationMs * features.mixOutFraction).toLong()
                }
            }
        } else {
            (durationMs * 0.82f).toLong()
        }
        return alignToBar(target, durationMs, features, barMs * 8f, down = true)
    }

    private fun incomingCueMs(
        durationMs: Long,
        features: AudioFeatures?,
        barMs: Float
    ): Long {
        if (durationMs <= 30_000L) return 0L
        val section = features?.sections?.firstOrNull {
            it.type == SectionType.Breakdown ||
                (it.type == SectionType.Intro && it.endFraction < 0.35f) ||
                it.type == SectionType.Verse
        } ?: features?.sections?.firstOrNull {
            it.type == SectionType.Chorus || it.type == SectionType.Drop
        }
        val target = if (section != null) {
            (durationMs * section.startFraction).toLong()
        } else {
            (durationMs * (features?.mixInFraction ?: 0.1f)).toLong()
        }
        return alignToBar(target, durationMs, features, barMs * 8f, down = false)
    }

    private fun alignToBar(
        targetMs: Long,
        durationMs: Long,
        features: AudioFeatures?,
        barMs: Float,
        down: Boolean
    ): Long {
        val bar = barMs.toLong().coerceAtLeast(1L)
        val offset = ((features?.beatOffsetSeconds ?: 0f) * 1000f).toLong()
        val relative = (targetMs - offset).coerceAtLeast(0L)
        val bars = if (down) relative / bar else (relative + bar - 1) / bar
        val aligned = offset + bars * bar
        return aligned.coerceIn(0L, durationMs - 2_000L)
    }
}
