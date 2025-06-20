package com.lebaillyapp.beatvibrator.domain.dsp

/**
 * Filtre Butterworth passe-bas (squelette)
 */
class ButterworthFilter {
    /**
     * Applique un filtre passe-bas sur un signal PCM
     * @param samples Signal audio en float[]
     * @param sampleRate Fréquence d'échantillonnage en Hz
     * @return Signal filtré
     */
    fun apply(samples: FloatArray, sampleRate: Int): FloatArray {
        // TODO: implémenter le filtre
        return samples
    }
}