package com.lebaillyapp.beatvibrator.data.repository

import com.lebaillyapp.beatvibrator.data.service.DspAnalysisService
import com.lebaillyapp.beatvibrator.domain.audioProcess.DecodingResult
import com.lebaillyapp.beatvibrator.domain.dsp.AnalysisResult
import com.lebaillyapp.beatvibrator.domain.dsp.AnalysisStats
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ## Repository pour le service d'analyse DSP audio.
 *
 * Ce repository expose une API simple pour lancer une analyse audio
 * à partir d'un résultat de décodage, observer l'état de l'analyse
 * et récupérer les stats ou resetter le service.
 *
 * Il repose directement sur le DspAnalysisService, sans logique métier supplémentaire.
 */
@Singleton
class DspRepository @Inject constructor(
    private val dspAnalysisService: DspAnalysisService
) {

    /**
     * Flux d'état observable pour suivre la progression ou le résultat de l'analyse DSP.
     */
    val analysisState: StateFlow<DspAnalysisService.AnalysisState> = dspAnalysisService.analysisState

    /**
     * Lance une analyse complète du signal audio PCM décodé.
     *
     * @param decodingResult Le résultat de décodage audio (PCM) à analyser.
     * @return Le résultat d'analyse DSP complet.
     * @throws Exception en cas d'erreur lors de l'analyse.
     */
    suspend fun analyseAudio(decodingResult: DecodingResult): AnalysisResult {
        return dspAnalysisService.analyseAudio(decodingResult)
    }

    /**
     * Réinitialise le service DSP pour un nouvel usage.
     */
    fun reset() {
        dspAnalysisService.reset()
    }

    /**
     * Récupère les statistiques de la dernière analyse effectuée,
     * ou null si aucune analyse complète n'a encore eu lieu.
     */
    fun getAnalysisStats(): AnalysisStats? {
        return dspAnalysisService.getAnalysisStats()
    }
}