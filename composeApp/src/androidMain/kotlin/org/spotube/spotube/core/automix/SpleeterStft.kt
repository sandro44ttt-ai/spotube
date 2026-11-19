package org.spotube.spotube.core.automix

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal object SpleeterStft {
    const val N_FFT = 4096
    const val HOP = 1024
    const val BINS = N_FFT / 2 + 1
    const val MODEL_BINS = 1024
    const val FRAMES_PER_SPLIT = 512

    private val window = FloatArray(N_FFT) { index ->
        (0.5 - 0.5 * cos(2.0 * PI * index / N_FFT)).toFloat()
    }

    data class Spectrum(
        val real: Array<FloatArray>,
        val imag: Array<FloatArray>,
        val frames: Int
    )

    fun stft(samples: FloatArray): Spectrum {
        val frames = samples.size / HOP + 1
        val real = Array(frames) { FloatArray(BINS) }
        val imag = Array(frames) { FloatArray(BINS) }
        val frameReal = FloatArray(N_FFT)
        val frameImag = FloatArray(N_FFT)
        for (frame in 0 until frames) {
            val start = frame * HOP
            for (index in 0 until N_FFT) {
                val sample = if (start + index < samples.size) samples[start + index] else 0f
                frameReal[index] = sample * window[index]
                frameImag[index] = 0f
            }
            fft(frameReal, frameImag, inverse = false)
            for (bin in 0 until BINS) {
                real[frame][bin] = frameReal[bin]
                imag[frame][bin] = frameImag[bin]
            }
        }
        return Spectrum(real, imag, frames)
    }

    fun istft(
        real: Array<FloatArray>,
        imag: Array<FloatArray>,
        frames: Int,
        outputLength: Int
    ): FloatArray {
        val fullLength = (frames - 1) * HOP + N_FFT
        val output = FloatArray(fullLength)
        val weights = FloatArray(fullLength)
        val frameReal = FloatArray(N_FFT)
        val frameImag = FloatArray(N_FFT)
        for (frame in 0 until frames) {
            frameReal.fill(0f)
            frameImag.fill(0f)
            for (bin in 0 until BINS) {
                frameReal[bin] = real[frame][bin]
                frameImag[bin] = imag[frame][bin]
            }
            for (bin in 1 until N_FFT / 2) {
                frameReal[N_FFT - bin] = real[frame][bin]
                frameImag[N_FFT - bin] = -imag[frame][bin]
            }
            frameImag[0] = 0f
            frameImag[N_FFT / 2] = 0f
            fft(frameReal, frameImag, inverse = true)
            val start = frame * HOP
            for (index in 0 until N_FFT) {
                val target = start + index
                output[target] += frameReal[index] * window[index]
                weights[target] += window[index] * window[index]
            }
        }
        for (index in output.indices) {
            if (weights[index] > 1e-8f) output[index] /= weights[index]
        }
        val result = FloatArray(outputLength.coerceAtLeast(1))
        val copyLength = minOf(result.size, output.size)
        for (index in 0 until copyLength) {
            result[index] = output[index]
        }
        return result
    }

    private fun fft(real: FloatArray, imag: FloatArray, inverse: Boolean) {
        val size = real.size
        var j = 0
        for (i in 1 until size) {
            var bit = size shr 1
            while ((j and bit) != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                val tempReal = real[i]
                real[i] = real[j]
                real[j] = tempReal
                val tempImag = imag[i]
                imag[i] = imag[j]
                imag[j] = tempImag
            }
        }
        var length = 2
        while (length <= size) {
            val angle = 2.0 * PI / length * if (inverse) 1.0 else -1.0
            val stepReal = cos(angle).toFloat()
            val stepImag = sin(angle).toFloat()
            var start = 0
            while (start < size) {
                var wReal = 1f
                var wImag = 0f
                for (offset in 0 until length / 2) {
                    val even = start + offset
                    val odd = even + length / 2
                    val oddReal = real[odd] * wReal - imag[odd] * wImag
                    val oddImag = real[odd] * wImag + imag[odd] * wReal
                    real[odd] = real[even] - oddReal
                    imag[odd] = imag[even] - oddImag
                    real[even] += oddReal
                    imag[even] += oddImag
                    val nextReal = wReal * stepReal - wImag * stepImag
                    wImag = wReal * stepImag + wImag * stepReal
                    wReal = nextReal
                }
                start += length
            }
            length = length shl 1
        }
        if (inverse) {
            for (index in 0 until size) {
                real[index] /= size
                imag[index] /= size
            }
        }
    }
}
