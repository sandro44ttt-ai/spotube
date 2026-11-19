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
    val vibeDistance: Float = 1f,
    val earlyCut: Boolean = false,
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
        val currentDuration = analyzedDurationMs(current, currentFeatures)
        val nextDuration = analyzedDurationMs(next, nextFeatures)
        val seed = (((current.id + next.id).hashCode() and 0x7FFFFFFF) / 10) % 4

        var outgoingCue = outgoingCueMs(currentDuration, currentFeatures, barMs)
        val nextBarMs = if (nextBpm > 0f) 60_000f / nextBpm * 4f else barMs
        var incomingCue = incomingCueMs(nextDuration, nextFeatures, nextBarMs)
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
        val vibeDistance = if (currentFeatures != null && nextFeatures != null) {
            currentFeatures.vibe.distance(nextFeatures.vibe)
        } else {
            1f
        }
        val bothLong = currentDuration > 60_000L && nextDuration > 60_000L
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
                vibeDistance <= 0.12f &&
                swingDiff <= 0.18f &&
                energyDistance < 0.5f &&
                (absPitch <= 2) -> AutoMixLevel.Bold

            compatibleBase &&
                vibeDistance <= 0.20f &&
                swingDiff <= 0.14f &&
                absPitch <= 1 -> AutoMixLevel.Mid

            compatibleBase &&
                vibeDistance <= 0.30f &&
                swingDiff <= 0.08f &&
                absPitch == 0 -> AutoMixLevel.Safe

            else -> null
        }

        if (level != null) {
            val hotEntry = if (
                level == AutoMixLevel.Bold &&
                seed % 3 == 0 &&
                vibeDistance <= 0.18f &&
                energyDistance < 0.40f
            ) {
                hotEntryCue(nextDuration, nextFeatures, nextBarMs)
            } else {
                null
            }
            if (hotEntry != null) {
                val cutCue = alignToBar(
                    outgoingCue,
                    currentDuration,
                    currentFeatures,
                    barMs * 8f,
                    down = true
                )
                return TransitionPlan(
                    kind = TransitionKind.HardCut,
                    outgoingCueMs = cutCue,
                    incomingCueMs = hotEntry,
                    durationMs = 1_000L,
                    tempoRatio = tempoRatio ?: 1f,
                    pitchSemitones = pitchShift ?: 0,
                    hardCut = true,
                    bassSwap = false,
                    bassOwnerIsA = true,
                    headroomDb = -3f,
                    bars = 0,
                    level = AutoMixLevel.Bold,
                    vibeDistance = vibeDistance,
                    reason = "hot entry: drop/chorus ${hotEntry}ms after phrase cut $cutCue ms"
                )
            }
            val bars = when (level) {
                AutoMixLevel.Safe -> 16
                AutoMixLevel.Mid -> 16
                AutoMixLevel.Bold -> if (seed % 2 == 0) 16 else 24
            }
            val duration = (bars * barMs).coerceIn(
                16_000f,
                24_000f
            ).coerceAtMost((currentDuration - 6_000L).toFloat().coerceAtLeast(8_000f))
            val durationMs = duration.toLong()
            val early = if (vibeDistance <= 0.30f) {
                earlyCutCues(
                    currentDurationMs = currentDuration,
                    currentFeatures = currentFeatures,
                    nextDurationMs = nextDuration,
                    nextFeatures = nextFeatures,
                    currentBarMs = barMs,
                    nextBarMs = nextBarMs
                )
            } else {
                null
            }
            val useEarlyCut = early != null &&
                early.first + durationMs + 500L <= currentDuration
            val startCue = if (useEarlyCut) {
                early!!.first
            } else {
                fitTransitionStart(outgoingCue, currentDuration, durationMs)
            }
            val entryCue = if (useEarlyCut) early!!.second else incomingCue
            outgoingCue = startCue
            incomingCue = entryCue
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
                durationMs = durationMs,
                tempoRatio = tempoRatio ?: 1f,
                pitchSemitones = pitchShift ?: 0,
                bassSwap = true,
                bassOwnerIsA = ownerIsA,
                secondaryHighPassHz = if (level == AutoMixLevel.Safe) 110f else 140f,
                midDipDb = midDip,
                centerAttenuationDb = center,
                headroomDb = if (level == AutoMixLevel.Safe) -2f else -3f,
                incomingOffsetMs = transientOffsetMs(currentFeatures, nextFeatures),
                bars = bars,
                level = level,
                vibeDistance = vibeDistance,
                earlyCut = useEarlyCut,
                loopBeats = loopBeats,
                loopRoll = level == AutoMixLevel.Bold && seed % 2 == 1,
                loopBpm = nextBpm * (tempoRatio ?: 1f),
                reason = "mashup ${level.name.lowercase()} ${bars}b: " +
                    "${currentBpm.roundToInt()}->${nextBpm.roundToInt()} BPM, " +
                    "pitch ${pitchShift ?: 0}, key $keyDistance, vibe ${(vibeDistance * 100).roundToInt()}%, " +
                    "bass ${if (ownerIsA) "A" else "B"}, " +
                    "swing ${(swingDiff * 100).roundToInt()}%" +
                    if (useEarlyCut) " early-cut" else ""
            )
        }
        val canBlend = tempoRatio != null &&
            pitchShift != null &&
            keyDistance <= 1 &&
            bpmConfidence >= 0.4f &&
            energyDistance < 0.65f
        if (canBlend) {
            val bars = if (seed % 2 == 0) 16 else 24
            val duration = (bars * barMs).coerceIn(16_000f, 24_000f)
                .coerceAtMost((currentDuration - 6_000L).toFloat().coerceAtLeast(8_000f))
            val durationMs = duration.toLong()
            val startCue = fitTransitionStart(outgoingCue, currentDuration, durationMs)
            val ownerIsA = vocalA < vocalB
            return TransitionPlan(
                kind = if (tempoRatio != null) TransitionKind.TempoBlend else TransitionKind.BeatBlend,
                outgoingCueMs = startCue,
                incomingCueMs = incomingCue,
                durationMs = durationMs,
                tempoRatio = tempoRatio ?: 1f,
                pitchSemitones = pitchShift ?: 0,
                bassSwap = energyDistance > 0.3f,
                bassOwnerIsA = ownerIsA,
                secondaryHighPassHz = if (energyDistance > 0.3f) 130f else 80f,
                midDipDb = if (vocalConflict) 3f else 0f,
                centerAttenuationDb = if (vocalConflict) 2f else 0f,
                headroomDb = -3f,
                incomingOffsetMs = transientOffsetMs(currentFeatures, nextFeatures),
                bars = bars,
                vibeDistance = vibeDistance,
                reason = "blend ${bars}b: ${currentBpm.roundToInt()}->${nextBpm.roundToInt()} BPM, " +
                    "pitch ${pitchShift ?: 0}, key $keyDistance, vibe ${(vibeDistance * 100).roundToInt()}%, " +
                    "bass ${if (ownerIsA) "A" else "B"}"
            )
        }

        val bars = 16
        val durationMs = (bars * barMs).coerceIn(16_000f, 24_000f)
            .coerceAtMost((currentDuration - 6_000L).toFloat().coerceAtLeast(8_000f))
            .toLong()
        return TransitionPlan(
            kind = TransitionKind.SafeBlend,
            outgoingCueMs = fitTransitionStart(outgoingCue, currentDuration, durationMs),
            incomingCueMs = incomingCue,
            durationMs = durationMs,
            tempoRatio = 1f,
            pitchSemitones = 0,
            bassSwap = true,
            bassOwnerIsA = vocalA < vocalB,
            secondaryHighPassHz = 150f,
            midDipDb = 5f,
            centerAttenuationDb = 4f,
            headroomDb = -3f,
            incomingOffsetMs = transientOffsetMs(currentFeatures, nextFeatures),
            bars = bars,
            level = AutoMixLevel.Safe,
            vibeDistance = vibeDistance,
            reason = "safe blend ${bars}b: key $keyDistance, vibe ${(vibeDistance * 100).roundToInt()}%, BPM " +
                "${currentBpm.roundToInt()}->${nextBpm.roundToInt()}"
        )
    }

    private fun analyzedDurationMs(track: MetadataTrack, features: AudioFeatures?): Long {
        val decoded = features?.durationSeconds ?: 0f
        val duration = if (features?.analysisComplete == true && decoded > 20f) {
            (decoded * 1000f).toLong()
        } else {
            track.durationMs
        }
        return duration.coerceAtLeast(1L)
    }

    private fun fitTransitionStart(
        cueMs: Long,
        trackDurationMs: Long,
        transitionDurationMs: Long
    ): Long {
        val latest = (trackDurationMs - transitionDurationMs - 500L).coerceAtLeast(0L)
        return cueMs.coerceIn(0L, latest)
    }

    private fun earlyCutCues(
        currentDurationMs: Long,
        currentFeatures: AudioFeatures?,
        nextDurationMs: Long,
        nextFeatures: AudioFeatures?,
        currentBarMs: Float,
        nextBarMs: Float
    ): Pair<Long, Long>? {
        if (currentDurationMs < 90_000L || nextDurationMs < 45_000L) return null
        val current = currentFeatures ?: return null
        val next = nextFeatures ?: return null
        if (!current.analysisComplete || !next.analysisComplete) return null
        val breakdown = current.sections
            .filter {
                it.type == SectionType.Breakdown &&
                    it.startFraction in 0.28f..0.78f &&
                    ((it.endFraction - it.startFraction) * currentDurationMs /
                        currentBarMs.coerceAtLeast(1f)) >= 6f
            }
            .maxByOrNull { section ->
                (section.endFraction - section.startFraction) +
                    (1f - section.energy) * 0.4f
            } ?: return null
        val entry = next.sections.firstOrNull {
            it.type == SectionType.Drop && it.energy >= 0.40f
        } ?: next.sections.firstOrNull {
            it.type == SectionType.Chorus && it.energy >= 0.45f
        } ?: return null
        if (entry.startFraction > 0.55f) return null
        val exitMs = (currentDurationMs * breakdown.startFraction).toLong()
        val entryMs = (nextDurationMs * entry.startFraction).toLong()
        return alignToBar(
            exitMs,
            currentDurationMs,
            current,
            currentBarMs * 8f,
            down = true
        ) to alignToBar(
            entryMs,
            nextDurationMs,
            next,
            nextBarMs * 8f,
            down = true
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

    private fun hotEntryCue(
        durationMs: Long,
        features: AudioFeatures?,
        barMs: Float
    ): Long? {
        if (durationMs < 45_000L || features?.analysisComplete != true) return null
        val section = features.sections.firstOrNull {
            it.type == SectionType.Drop || it.type == SectionType.Chorus
        } ?: return null
        val target = (durationMs * section.startFraction).toLong()
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
