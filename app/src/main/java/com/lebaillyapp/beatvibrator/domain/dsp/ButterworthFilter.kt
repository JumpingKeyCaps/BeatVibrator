package com.lebaillyapp.beatvibrator.domain.dsp

import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.sqrt

/**
 * Implémente un filtre passe-bas Butterworth biquad de 2e ordre.
 *
 * Ce filtre atténue les hautes fréquences au-delà de la fréquence de coupure,
 * permettant de conserver les basses fréquences (ex : percussions, beat).
 *
 * Utilisé pour prétraiter le signal audio avant calcul RMS ou détection d'onsets.
 *
 * @property cutoffFreqHz Fréquence de coupure (Hz) en dessous de laquelle les fréquences sont conservées.
 */
class ButterworthFilter(
    private val cutoffFreqHz: Float
) {
    private var a0 = 0f
    private var a1 = 0f
    private var a2 = 0f
    private var b1 = 0f
    private var b2 = 0f

    // États précédents : entrées (x1,x2) et sorties (y1,y2)
    private var x1 = 0f
    private var x2 = 0f
    private var y1 = 0f
    private var y2 = 0f

    /**
     * Applique le filtre passe-bas sur un tableau de samples PCM mono.
     *
     * @param samples Tableau de samples audio (float PCM mono)
     * @param sampleRate Fréquence d’échantillonnage en Hz (ex: 44100)
     * @return Signal filtré
     */
    fun apply(samples: FloatArray, sampleRate: Int): FloatArray {
        computeCoefficients(sampleRate)

        val output = FloatArray(samples.size)
        for (i in samples.indices) {
            val x0 = samples[i]

            // Équation du filtre biquad (Direct Form I)
            val y0 = a0 * x0 + a1 * x1 + a2 * x2 - b1 * y1 - b2 * y2

            // Mise à jour des états
            x2 = x1
            x1 = x0
            y2 = y1
            y1 = y0

            output[i] = y0
        }
        return output
    }

    /**
     * Calcule les coefficients du filtre Butterworth passe-bas (bilinear transform)
     *
     * @param sampleRate Fréquence d’échantillonnage en Hz
     */
    private fun computeCoefficients(sampleRate: Int) {
        val omega = 2 * PI * cutoffFreqHz / sampleRate
        val sinOmega = sin(omega)
        val cosOmega = cos(omega)
        val q = sqrt(2.0) / 2.0 // Q-factor pour Butterworth 2e ordre (≈ 0.7071)
        val alpha = sinOmega / (2.0 * q)

        val a0Inv = 1.0 / (1.0 + alpha)

        a0 = ((1.0 - cosOmega) / 2.0 * a0Inv).toFloat()
        a1 = ((1.0 - cosOmega) * a0Inv).toFloat()
        a2 = a0
        b1 = (-2.0 * cosOmega * a0Inv).toFloat()
        b2 = ((1.0 - alpha) * a0Inv).toFloat()
    }

    /**
     * Réinitialise l’état interne du filtre (utile entre plusieurs appels).
     */
    fun reset() {
        x1 = 0f
        x2 = 0f
        y1 = 0f
        y2 = 0f
    }
}