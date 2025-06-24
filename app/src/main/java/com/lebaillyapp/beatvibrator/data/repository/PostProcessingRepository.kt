package com.lebaillyapp.beatvibrator.data.repository

import com.lebaillyapp.beatvibrator.data.service.PostProcessingService
import com.lebaillyapp.beatvibrator.domain.VibrationPulse
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ## PostProcessingRepository
 *
 * Repository responsable d'orchestrer le post-traitement des onsets
 * en séquences vibratoires optimisées via [PostProcessingService].
 *
 * Fournit :
 * - Une API suspendue pour transformer les onsets en [VibrationPulse]
 * - Un [StateFlow] exposant l'état du traitement en temps réel
 */
@Singleton
class PostProcessingRepository @Inject constructor(
    private val service: PostProcessingService
) {

    /** État en temps réel du post-traitement */
    val state: StateFlow<PostProcessingService.PostProcessState> = service.state

    /**
     * Lance le traitement et retourne les pulses vibratoires prêtes.
     *
     * @param onsets Liste des onsets (timestamp, amplitude)
     * @param bpm Tempo optionnel pour quantifier les pulses
     * @param division Subdivision rythmique (ex: 4 = double croche)
     */
    suspend fun generatePulses(
        onsets: List<Pair<Long, Float>>,
        bpm: Int? = null,
        division: Int = 4
    ): List<VibrationPulse> {
        return service.processOnsets(onsets, bpm, division)
    }
}