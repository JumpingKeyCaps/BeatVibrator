package com.lebaillyapp.beatvibrator.domain.dsp

/**
 * Détecteur d'onsets basé sur le spectral flux.
 *
 * Le spectral flux mesure la différence positive entre la magnitude
 * spectrale de la trame courante et la trame précédente.
 * Cela permet de détecter les transitoires typiques des percussions ou
 * des attaques dans la musique.
 */
class OnsetDetector {
    fun detectOnsets(
        magnitudes: List<FloatArray>,
        threshold: Float = 0.3f,
        minIntervalMs: Float = 50f, // Délai minimum entre onsets
        sampleRate: Int = 44100,
        hopSize: Int = 512
    ): List<Int> {
        if (magnitudes.size < 2) return emptyList()

        val fluxValues = computeSpectralFlux(magnitudes)
        return peakPicking(fluxValues, threshold, minIntervalMs, sampleRate, hopSize)
    }

    private fun computeSpectralFlux(magnitudes: List<FloatArray>): FloatArray {
        val flux = FloatArray(magnitudes.size - 1)

        for (i in 1 until magnitudes.size) {
            val prevFrame = magnitudes[i - 1]
            val currFrame = magnitudes[i]

            var frameFlux = 0f
            for (j in currFrame.indices) {
                val diff = currFrame[j] - prevFrame[j]
                if (diff > 0) frameFlux += diff
            }
            flux[i - 1] = frameFlux / currFrame.size
        }
        return flux
    }

    private fun peakPicking(
        flux: FloatArray,
        threshold: Float,
        minIntervalMs: Float,
        sampleRate: Int,
        hopSize: Int
    ): List<Int> {
        val onsets = mutableListOf<Int>()
        val minIntervalFrames = (minIntervalMs * sampleRate / (1000f * hopSize)).toInt()

        var lastOnset = -minIntervalFrames

        for (i in 1 until flux.size - 1) {
            if (flux[i] > threshold &&
                flux[i] > flux[i-1] &&
                flux[i] > flux[i+1] &&
                i - lastOnset >= minIntervalFrames) {
                onsets.add(i)
                lastOnset = i
            }
        }
        return onsets
    }
}