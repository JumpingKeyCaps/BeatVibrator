package com.lebaillyapp.beatvibrator.data.service


import com.lebaillyapp.beatvibrator.domain.dsp.ButterworthFilter
import com.lebaillyapp.beatvibrator.domain.audioProcess.DecodingResult
import com.lebaillyapp.beatvibrator.domain.dsp.AnalysisResult
import com.lebaillyapp.beatvibrator.domain.dsp.AnalysisStats
import com.lebaillyapp.beatvibrator.domain.dsp.BpmDetector
import com.lebaillyapp.beatvibrator.domain.dsp.DspConfig
import com.lebaillyapp.beatvibrator.domain.dsp.FFT
import com.lebaillyapp.beatvibrator.domain.dsp.OnsetDetector
import com.lebaillyapp.beatvibrator.domain.dsp.RmsCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
 * ### Pipeline DSP optimisé :
 * 1. Filtrage passe-bas Butterworth (200Hz) pour isoler les basses fréquences
 * 2. Calcul RMS sur le signal filtré (pour l'énergie temporelle)
 * 3. Détection de BPM (pour la synchronisation temporelle en post-processing)
 * 4. Spectrogramme FFT sur le signal filtré (pour l'analyse fréquentielle)
 * 5. Détection d'onsets avec peak picking (pour les transitoires)
 *
 * ### Avantages :
 * - Suivi de progression en temps réel
 * - Configuration centralisée des paramètres DSP
 * - Gestion d'erreurs robuste
 * - Pipeline optimisé pour la détection de beats/percussions
 */
@Singleton
class DspAnalysisService @Inject constructor(
    private val butterworthFilter: ButterworthFilter,
    private val rmsCalculator: RmsCalculator,
    private val onsetDetector: OnsetDetector,
    private val fft: FFT,
    private val bpmDetector: BpmDetector,
    private val config: DspConfig = DspConfig()
) {

    companion object {
        private const val TAG = "DspAnalysisService"
    }

    // === ÉTAT DE L'ANALYSE ===

    /**
     * Représente l'état global de l'analyse DSP
     */
    sealed class AnalysisState {
        object Idle : AnalysisState()
        data class Processing(val step: String, val progress: Float) : AnalysisState()
        data class Completed(val result: AnalysisResult) : AnalysisState()
        data class Error(val message: String) : AnalysisState()
    }

    private val _analysisState = MutableStateFlow<AnalysisState>(AnalysisState.Idle)

    /**
     * Flux d'état observable depuis le ViewModel ou l'UI
     */
    val analysisState: StateFlow<AnalysisState> = _analysisState.asStateFlow()

    // === ANALYSE PRINCIPALE ===

    /**
     * Analyse complète d'un signal audio décodé
     *
     * @param decodingResult Résultat du décodage MP3 → PCM
     * @return Résultat d'analyse contenant RMS, onsets, et spectrogramme
     * @throws Exception si l'analyse échoue
     */
    suspend fun analyseAudio(decodingResult: DecodingResult): AnalysisResult =
        withContext(Dispatchers.Default) {
            _analysisState.value = AnalysisState.Processing("Initialisation", 0f)

            try {
                val samples = decodingResult.getAllSamplesFlat()
                val sampleRate = decodingResult.sampleRate

                // Validation des données d'entrée
                if (samples.isEmpty()) {
                    throw IllegalArgumentException("Échantillons audio vides")
                }
                if (sampleRate <= 0) {
                    throw IllegalArgumentException("Sample rate invalide: $sampleRate")
                }

                // === 1. FILTRAGE PASSE-BAS ===
                _analysisState.value = AnalysisState.Processing("Filtrage passe-bas", 0.2f)

                // Réinitialiser le filtre pour éviter les artefacts entre analyses
                butterworthFilter.reset()
                val filteredSamples = butterworthFilter.apply(samples, sampleRate)

                // === 2. CALCUL RMS (sur signal filtré) ===
                _analysisState.value = AnalysisState.Processing("Calcul RMS", 0.4f)

                val rmsValues = rmsCalculator.computeRmsOverWindows(
                    samples = filteredSamples,
                    windowSize = config.rmsWindowSize,
                    hopSize = config.rmsHopSize,
                    normalize = true
                )

                // === 3. ESTIMATION BPM ===
                // Calcul du sampleRate du signal RMS
                val rmsSampleRateHz = decodingResult.sampleRate.toFloat() / config.rmsHopSize
                _analysisState.value = AnalysisState.Processing("Détection BPM", 0.7f)
                val detectedBpm = bpmDetector.estimateBpm(rmsValues, rmsSampleRateHz)


                // === 4. SPECTROGRAMME FFT ===
                _analysisState.value = AnalysisState.Processing("Analyse spectrale FFT", 0.6f)
                val spectrogram = fft.computeSpectrogram(
                    samples = filteredSamples,
                    fftSize = config.fftSize,
                    hopSize = config.hopSize
                )

                // === 5. DÉTECTION D'ONSETS ===
                _analysisState.value = AnalysisState.Processing("Détection onsets", 0.8f)

                val onsets = onsetDetector.detectOnsets(
                    magnitudes = spectrogram,
                    threshold = config.onsetThreshold,
                    minIntervalMs = config.onsetMinInterval,
                    sampleRate = sampleRate,
                    hopSize = config.hopSize
                )

                // === 5. CONSTRUCTION DU RÉSULTAT ===
                _analysisState.value = AnalysisState.Processing("Finalisation", 0.95f)

                val result = AnalysisResult(
                    rmsValues = rmsValues,
                    onsets = onsets,
                    spectrogram = spectrogram,
                    sampleRate = sampleRate,
                    originalDuration = decodingResult.durationSeconds,
                    filteredSamples = filteredSamples, // Utile pour debug/visualisation
                    config = config,
                    // Métadonnées temporelles pour synchronisation
                    rmsTimeStamps = generateTimeStamps(rmsValues.size, config.rmsHopSize, sampleRate),
                    onsetTimeStamps = onsets.map { frameIndex ->
                        frameIndex * config.hopSize.toFloat() / sampleRate
                    },
                    detectedBpm = detectedBpm
                )

                _analysisState.value = AnalysisState.Completed(result)
                result

            } catch (e: Exception) {
                val errorMsg = "Erreur analyse DSP: ${e.message}"
                _analysisState.value = AnalysisState.Error(errorMsg)
                throw Exception(errorMsg, e)
            }
        }

    // === OUTILS INTERNES ===

    /**
     * Génère les timestamps temporels pour une séquence de fenêtres
     */
    private fun generateTimeStamps(
        frameCount: Int,
        hopSize: Int,
        sampleRate: Int
    ): List<Float> {
        return List(frameCount) { frameIndex ->
            frameIndex * hopSize.toFloat() / sampleRate
        }
    }

    /**
     * Réinitialise l'état interne du service
     */
    fun reset() {
        _analysisState.value = AnalysisState.Idle
        butterworthFilter.reset()
    }

    /**
     * Retourne les statistiques de l'analyse en cours
     */
    fun getAnalysisStats(): AnalysisStats? {
        return when (val state = _analysisState.value) {
            is AnalysisState.Completed -> AnalysisStats(
                rmsCount = state.result.rmsValues.size,
                onsetCount = state.result.onsets.size,
                spectrogramFrames = state.result.spectrogram.size,
                duration = state.result.originalDuration
            )
            else -> null
        }
    }
}