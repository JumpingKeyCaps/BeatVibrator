package com.lebaillyapp.beatvibrator.domain.dsp

/**
 * Résultat d'analyse DSP
 *
 * @property rmsValues Liste des valeurs RMS calculées par fenêtre
 * @property onsets Liste des indices où un onset est détecté
 * @property spectrogram Liste des spectres FFT par fenêtre
 */
data class AnalysisResult(
    val rmsValues: List<Float>,
    val onsets: List<Int>,
    val spectrogram: List<FloatArray>,
    val sampleRate: Int,
    val originalDuration: Float,
    val filteredSamples: FloatArray,        // Signal filtré pour debug
    val config: DspConfig,
    val rmsTimeStamps: List<Float>,         // Timestamps RMS (secondes)
    val onsetTimeStamps: List<Float>        // Timestamps onsets (secondes)
)