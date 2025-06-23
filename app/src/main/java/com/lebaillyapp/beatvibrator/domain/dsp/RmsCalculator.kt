package com.lebaillyapp.beatvibrator.domain.dsp


/**
 * Calculateur RMS (Root Mean Square) pour l’analyse d'énergie d'un signal audio.
 *
 * Le RMS est utile pour détecter les variations d’amplitude (volume), par exemple pour suivre
 * les beats ou les percussions après filtrage.
 */
class RmsCalculator {

    /**
     * Calcule les valeurs RMS sur des fenêtres glissantes du signal.
     *
     * @param samples Signal audio brut (mono, normalisé entre -1.0f et 1.0f)
     * @param windowSize Taille de chaque fenêtre (en échantillons)
     * @param hopSize Pas entre chaque fenêtre (overlap = windowSize - hopSize)
     * @param normalize Si vrai, normalise les valeurs RMS sur [0, 1] en divisant par le max
     * @return Liste des valeurs RMS (normalisées ou brutes)
     */
    fun computeRmsOverWindows(
        samples: FloatArray,
        windowSize: Int,
        hopSize: Int,
        normalize: Boolean = false
    ): List<Float> {
        val rmsValues = mutableListOf<Float>()
        var start = 0

        while (start + windowSize <= samples.size) {
            var sumSquares = 0.0f

            for (i in start until start + windowSize) {
                val sample = samples[i]
                sumSquares += sample * sample
            }

            val meanSquare = sumSquares / windowSize
            val rms = kotlin.math.sqrt(meanSquare)

            rmsValues.add(rms)
            start += hopSize
        }

        return if (normalize) normalizeRms(rmsValues) else rmsValues
    }

    /**
     * Normalise une liste de valeurs RMS en les ramenant sur l'intervalle [0, 1].
     * @param rmsList Liste de valeurs RMS
     * @return Liste normalisée
     */
    private fun normalizeRms(rmsList: List<Float>): List<Float> {
        val max = rmsList.maxOrNull() ?: return rmsList
        return if (max > 0f) rmsList.map { it / max } else rmsList
    }
}