package com.lebaillyapp.beatvibrator.domain.dsp

/**
 * Statistiques de l'analyse pour monitoring
 */
data class AnalysisStats(
    val rmsCount: Int,
    val onsetCount: Int,
    val spectrogramFrames: Int,
    val duration: Float
)