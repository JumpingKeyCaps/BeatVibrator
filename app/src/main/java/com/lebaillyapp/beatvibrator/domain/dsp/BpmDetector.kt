package com.lebaillyapp.beatvibrator.domain.dsp

import kotlin.math.roundToInt

class BpmDetector {

    /**
     * Estime le BPM d'un signal en utilisant l'autocorrélation.
     * @param signal Les valeurs numériques (ex: RMS, flux spectral).
     * @param sampleRateHz Le taux d'échantillonnage du signal d'entrée (pas celui de l'audio PCM, mais celui des RMS/flux).
     * Ex: si rmsHopSize est 512 et sampleRate audio est 44100, alors sampleRateHz = 44100 / 512.
     * @param minBpm Le BPM minimum à considérer (ex: 60).
     * @param maxBpm Le BPM maximum à considérer (ex: 180).
     * @return Le BPM estimé, ou null si non détecté.
     */
    fun estimateBpm(
        signal: List<Float>,
        sampleRateHz: Float, // Taux d'échantillonnage de ce SIGNAL (ex: RMS frames par seconde)
        minBpm: Int = 60,
        maxBpm: Int = 180
    ): Int? {
        if (signal.size < 2) return null

        val autocorrelation = calculateAutocorrelation(signal)

        // Convertir les BPM min/max en laps de temps (en nombre de frames)
        val minLagFrames = (60f / maxBpm * sampleRateHz).roundToInt()
        val maxLagFrames = (60f / minBpm * sampleRateHz).roundToInt()

        var bestBpm: Int? = null
        var maxCorrelation = -1.0f

        // Recherche du pic d'autocorrélation dans la plage de BPM pertinente
        for (lag in minLagFrames..maxLagFrames) {
            if (lag >= autocorrelation.size) continue // S'assurer que le lag est dans les limites

            val correlationValue = autocorrelation[lag]

            if (correlationValue > maxCorrelation) {
                maxCorrelation = correlationValue
                // Convertir le lag (en frames) en BPM
                val bpm = (60f / (lag.toFloat() / sampleRateHz)).roundToInt()
                bestBpm = bpm
            }
        }
        return bestBpm
    }

    /**
     * Calcule la fonction d'autocorrélation normalisée pour un signal.
     */
    private fun calculateAutocorrelation(signal: List<Float>): FloatArray {
        val n = signal.size
        val result = FloatArray(n)

        // Normaliser le signal pour éviter les valeurs trop grandes
        val maxVal = signal.maxOrNull() ?: 0f
        val normalizedSignal = if (maxVal > 0) signal.map { it / maxVal } else signal

        for (lag in 0 until n) {
            var sum = 0.0f
            for (i in 0 until n - lag) {
                sum += normalizedSignal[i] * normalizedSignal[i + lag]
            }
            // Normalisation par le nombre de termes pour des comparaisons justes
            result[lag] = if (n - lag > 0) sum / (n - lag) else 0f
        }
        return result
    }
}