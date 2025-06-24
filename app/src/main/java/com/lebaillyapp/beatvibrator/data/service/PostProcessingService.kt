package com.lebaillyapp.beatvibrator.data.service

import com.lebaillyapp.beatvibrator.domain.VibrationPulse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow
import kotlin.math.round

/**
 * ## PostProcessingService
 *
 * Service responsable de la transformation finale des événements d'onsets
 * en séquences optimisées de pulses vibratoires, prêtes à être envoyées au vibreur.
 *
 * Ce service applique :
 * - Suppression des bruits faibles
 * - Fusion des pulses proches
 * - Compression perceptuelle de l'intensité
 * - Ajustement dynamique de la durée
 * - Quantification temporelle optionnelle sur grille BPM
 * - Nettoyage final
 */
@Singleton
class PostProcessingService @Inject constructor() {

    sealed class PostProcessState {
        object Idle : PostProcessState()
        data class Processing(val step: String) : PostProcessState()
        data class Completed(val pulses: List<VibrationPulse>) : PostProcessState()
        data class Error(val message: String) : PostProcessState()
    }

    private val _state = MutableStateFlow<PostProcessState>(PostProcessState.Idle)
    val state: StateFlow<PostProcessState> = _state.asStateFlow()

    companion object {
        private const val MIN_INTERVAL_MS = 80L
        private const val MERGE_WINDOW_MS = 120L
        private const val MIN_INTENSITY = 0.08f
        private const val BASE_DURATION_MS = 40L
        private const val MAX_DURATION_MS = 100L
        private const val MIN_DURATION_MS = 20L
    }

    /**
     * Lance le post-traitement sur la liste brute d'onsets.
     *
     * @param onsets Liste de paires (timestamp en ms, amplitude normalisée)
     * @param bpm Tempo optionnel pour la quantification temporelle
     * @param division Subdivision rythmique (ex: 4 pour 1/16e notes)
     * @return Liste optimisée de [VibrationPulse]
     */
    suspend fun processOnsets(
        onsets: List<Pair<Long, Float>>,
        bpm: Int? = null,
        division: Int = 4
    ): List<VibrationPulse> = withContext(Dispatchers.Default) {
        try {
            _state.value = PostProcessState.Processing("Tri des onsets")
            if (onsets.isEmpty()) return@withContext emptyList<VibrationPulse>()

            val sorted = onsets.sortedBy { it.first }
            val pulses = mutableListOf<VibrationPulse>()

            var lastTime = -MIN_INTERVAL_MS
            var i = 0

            _state.value = PostProcessState.Processing("Fusion et compression")

            while (i < sorted.size) {
                val (baseTime, baseAmp) = sorted[i]

                if (baseAmp < MIN_INTENSITY || baseTime - lastTime < MIN_INTERVAL_MS) {
                    i++
                    continue
                }

                var mergedTime = baseTime
                var mergedAmp = baseAmp
                var count = 1

                var j = i + 1
                while (j < sorted.size && sorted[j].first - baseTime < MERGE_WINDOW_MS) {
                    mergedAmp += sorted[j].second
                    mergedTime += sorted[j].first
                    count++
                    j++
                }

                mergedAmp /= count
                mergedTime /= count

                val compressedAmp = mergedAmp.coerceIn(0f, 1f).pow(0.6f)

                val dynamicDuration = (BASE_DURATION_MS + (compressedAmp * (MAX_DURATION_MS - BASE_DURATION_MS)))
                    .toLong()
                    .coerceIn(MIN_DURATION_MS, MAX_DURATION_MS)

                val finalTime = bpm?.let {
                    val gridMs = 60_000f / it / division
                    round(mergedTime / gridMs) * gridMs
                }?.toLong() ?: mergedTime.toLong()

                pulses.add(
                    VibrationPulse(
                        timeMs = finalTime,
                        intensity = compressedAmp.coerceIn(0.15f, 1.0f),
                        durationMs = dynamicDuration
                    )
                )

                lastTime = baseTime
                i = j
            }

            val sortedPulses = pulses.sortedBy { it.timeMs }
            _state.value = PostProcessState.Completed(sortedPulses)
            sortedPulses
        } catch (e: Exception) {
            _state.value = PostProcessState.Error("Erreur post-processing: ${e.message}")
            emptyList()
        }
    }
}