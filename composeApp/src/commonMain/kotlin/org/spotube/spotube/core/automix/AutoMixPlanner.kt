package org.spotube.spotube.core.automix

import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class TransitionKind {
    HardCut,
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
    val hardCut: Boolean,
    val bassSwap: Boolean,
    val loopIn: LoopSpec?,
    val bars: Int,
    val reason: String
)

object AutoMixPlanner {
    fun plan(
        current: MetadataTrack,
        next: MetadataTrack,
        currentFeatures: AudioFeatures?,
        nextFeatures: AudioFeatures?,
        activeTempo: Float = 1f
    ): TransitionPlan {
        val currentBpm = currentFeatures?.bpm?.times(activeTempo) ?: 0f
        val nextBpm = nextFeatures?.bpm ?: 0f
        val beatMs = if (currentBpm > 0f) 60_000f / currentBpm else 500f
        val barMs = beatMs * 4f
        val currentDuration = current.durationMs.coerceAtLeast(1L)
        val nextDuration = next.durationMs.coerceAtLeast(1L)
        val seed = (((current.id + next.id).hashCode() and 0x7FFFFFFF) / 10) % 4

        val outgoingCue = outgoingCueMs(currentDuration, currentFeatures, barMs)
        val incomingCue = incomingCueMs(nextDuration, nextFeatures, barMs)
        val tempoRatio = AutoMixQueueSelector.tempoRatio(currentBpm, nextBpm)
        val keyDistance = AutoMixQueueSelector.keyDistance(
            currentFeatures?.key.orEmpty(),
            nextFeatures?.key.orEmpty()
        )
        val energyDistance = if (currentFeatures != null && nextFeatures != null) {
            abs(currentFeatures.energy - nextFeatures.energy)
        } else {
            1f
        }
        val vocalLoad = (currentFeatures?.vocalness ?: 0.5f) +
            (nextFeatures?.vocalness ?: 0.5f)

        val bothLong = current.durationMs > 60_000L && next.durationMs > 60_000L
        val canMashup = bothLong &&
            tempoRatio != null &&
            keyDistance <= 1 &&
            energyDistance < 0.5f &&
            vocalLoad < 1.25f
        if (canMashup) {
            val bars = if (seed % 2 == 0) 24 else 32
            val duration = (bars * barMs).coerceIn(12_000f, 30_000f)
            return TransitionPlan(
                kind = TransitionKind.Mashup,
                outgoingCueMs = outgoingCue,
                incomingCueMs = incomingCue,
                durationMs = duration.toLong(),
                tempoRatio = tempoRatio ?: 1f,
                hardCut = false,
                bassSwap = true,
                loopIn = null,
                bars = bars,
                reason = "mashup: ${currentBpm.roundToInt()}→${nextBpm.roundToInt()} BPM, " +
                    "$keyDistance key, vocal $vocalLoad"
            )
        }

        val canBlend = (tempoRatio != null || currentBpm <= 0f || nextBpm <= 0f) &&
            keyDistance <= 2 &&
            energyDistance < 0.65f
        if (canBlend) {
            val bars = if (seed % 2 == 0) 8 else 12
            val duration = (bars * barMs).coerceIn(6_000f, 18_000f)
            val loop = if (
                nextFeatures != null &&
                nextFeatures.dropFraction > 0.22f &&
                bars >= 8
            ) {
                val loopStart = incomingCue
                val loopEnd = loopStart + (4 * barMs).toLong()
                if (loopEnd < nextDuration) {
                    LoopSpec(loopStart, loopEnd, 4)
                } else {
                    null
                }
            } else {
                null
            }
            return TransitionPlan(
                kind = if (loop != null) TransitionKind.LoopBridge else {
                    if (tempoRatio != null) TransitionKind.TempoBlend else TransitionKind.BeatBlend
                },
                outgoingCueMs = outgoingCue,
                incomingCueMs = incomingCue,
                durationMs = duration.toLong(),
                tempoRatio = tempoRatio ?: 1f,
                hardCut = false,
                bassSwap = energyDistance > 0.3f,
                loopIn = loop,
                bars = bars,
                reason = "blend: ${currentBpm.roundToInt()}→${nextBpm.roundToInt()} BPM, " +
                    "ratio ${tempoRatio ?: 1f}, key $keyDistance"
            )
        }

        return TransitionPlan(
            kind = TransitionKind.HardCut,
            outgoingCueMs = outgoingCue,
            incomingCueMs = incomingCue,
            durationMs = 45L,
            tempoRatio = 1f,
            hardCut = true,
            bassSwap = false,
            loopIn = null,
            bars = 0,
            reason = "hard cut: key $keyDistance, BPM " +
                "${currentBpm.roundToInt()}→${nextBpm.roundToInt()}"
        )
    }

    private fun outgoingCueMs(
        durationMs: Long,
        features: AudioFeatures?,
        barMs: Float
    ): Long {
        if (durationMs <= 25_000L) {
            return max(0L, durationMs - 5_000L)
        }
        val fraction = features?.mixOutFraction ?: 0.82f
        val target = (durationMs * fraction).toLong()
        val bar = barMs.toLong().coerceAtLeast(1L)
        val offset = ((features?.beatOffsetSeconds ?: 0f) * 1000f).toLong()
        val bars = ((target - offset).coerceAtLeast(0L) / bar)
        val aligned = offset + bars * bar
        return aligned.coerceIn(0L, durationMs - 2_500L)
    }

    private fun incomingCueMs(
        durationMs: Long,
        features: AudioFeatures?,
        barMs: Float
    ): Long {
        if (durationMs <= 25_000L) return 0L
        val fraction = features?.mixInFraction ?: 0.08f
        val target = (durationMs * fraction).toLong()
        val bar = barMs.toLong().coerceAtLeast(1L)
        val offset = ((features?.beatOffsetSeconds ?: 0f) * 1000f).toLong()
        val bars = ((target - offset).coerceAtLeast(0L) + bar - 1) / bar
        val aligned = offset + bars * bar
        return aligned.coerceIn(0L, min(durationMs - 3_000L, durationMs - 1_000L))
    }
}
