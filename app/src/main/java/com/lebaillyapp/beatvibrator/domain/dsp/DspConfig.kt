package com.lebaillyapp.beatvibrator.domain.dsp

/**
 * Configuration centralisée des paramètres DSP
 */
data class DspConfig(
    val lowPassCutoff: Float = 200f,        // Fréquence de coupure (Hz)
    val fftSize: Int = 1024,                // Taille FFT (puissance de 2)
    val hopSize: Int = 512,                 // Pas FFT (50% overlap)
    val rmsWindowSize: Int = 512,           // Fenêtre RMS
    val rmsHopSize: Int = 256,              // Pas RMS (50% overlap)
    val onsetThreshold: Float = 0.3f,       // Seuil détection onset
    val onsetMinInterval: Float = 50f       // Intervalle min entre onsets (ms)
) {
    init {
        require(fftSize > 0 && (fftSize and (fftSize - 1)) == 0) {
            "fftSize doit être une puissance de 2, reçu: $fftSize"
        }
        require(hopSize > 0 && hopSize <= fftSize) {
            "hopSize doit être > 0 et <= fftSize, reçu: $hopSize"
        }
        require(rmsWindowSize > 0 && rmsHopSize > 0) {
            "Tailles de fenêtres RMS doivent être > 0"
        }
        require(lowPassCutoff > 0f) {
            "Fréquence de coupure doit être > 0, reçu: $lowPassCutoff"
        }
        require(onsetThreshold > 0f) {
            "Seuil onset doit être > 0, reçu: $onsetThreshold"
        }
        require(onsetMinInterval > 0f) {
            "Intervalle min onset doit être > 0, reçu: $onsetMinInterval"
        }
    }
}