package com.lebaillyapp.beatvibrator.data.service


import com.lebaillyapp.beatvibrator.domain.dsp.ButterworthFilter
import com.lebaillyapp.beatvibrator.domain.DecodingResult
import com.lebaillyapp.beatvibrator.domain.dsp.FFT
import com.lebaillyapp.beatvibrator.domain.dsp.OnsetDetector
import com.lebaillyapp.beatvibrator.domain.dsp.RmsCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ## Service d'analyse DSP audio
 *
 * Ce service traite un résultat de décodage audio (PCM) en extrayant des
 * métriques DSP avancées comme RMS, détection d'onsets, et spectre FFT.
 *
 * Il s'appuie sur les composants du domaine (`ButterworthFilter`, `RmsCalculator`, etc.)
 * pour produire des données exploitables pour la génération de patterns de vibration.
 *
 * ### Fonctionnalités principales :
 * - Application d'un filtre passe-bas
 * - Calcul de RMS sur fenêtres glissantes
 * - Détection d'onsets (transitoires)
 * - Calcul d'un spectrogramme via FFT
 */
@Singleton
class DspAnalysisService @Inject constructor(
    private val butterworthFilter: ButterworthFilter,
    private val rmsCalculator: RmsCalculator,
    private val onsetDetector: OnsetDetector,
    private val fft: FFT
) {

    /**
     * Résultat d'analyse DSP
     *
     * @param rmsValues Liste des valeurs RMS calculées par fenêtre
     * @param onsets Liste des indices où un onset est détecté
     * @param spectrogram Liste des spectres FFT par fenêtre
     */
    data class AnalysisResult(
        val rmsValues: List<Float>,
        val onsets: List<Int>,
        val spectrogram: List<FloatArray>
    )

    /**
     * Analyse le signal PCM contenu dans un [DecodingResult] pour extraire
     * ses caractéristiques DSP.
     *
     * @param decodingResult Résultat de décodage audio (PCM float mono)
     * @return [AnalysisResult] contenant RMS, onsets et spectrogramme
     */
    suspend fun analyseAudio(decodingResult: DecodingResult): AnalysisResult =
        withContext(Dispatchers.Default) {

            val samples = decodingResult.getAllSamplesFlat()

            // Filtrage passe-bas pour éliminer bruit haute fréquence (exemple)
            val filteredSamples = butterworthFilter.apply(samples, decodingResult.sampleRate)

            // RMS calculé sur fenêtres de 512 échantillons, avec recouvrement 50%
            val rmsValues = rmsCalculator.computeRmsOverWindows(filteredSamples, windowSize = 512, hopSize = 256)

            // Détection des onsets (transitoires percussifs)
            val onsets = onsetDetector.detectOnsets(filteredSamples)

            // Calcul du spectrogramme FFT (1024 points, 50% overlap)
            val spectrogram = fft.computeSpectrogram(filteredSamples, fftSize = 1024, hopSize = 512)

            AnalysisResult(
                rmsValues = rmsValues,
                onsets = onsets,
                spectrogram = spectrogram
            )
        }
}