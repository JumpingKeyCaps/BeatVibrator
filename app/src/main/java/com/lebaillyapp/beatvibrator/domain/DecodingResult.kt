package com.lebaillyapp.beatvibrator.domain

/**
 * Données de sortie du décodage audio complet
 *
 * Contient le résultat final du décodage audio
 *
 * - `samples` : blocs PCM normalisés
 * - `sampleRate` : en Hz (ex. 44100)
 * - `channelCount` : avant conversion (souvent 2)
 * - `totalSamples` : échantillons mono concaténés
 * - `durationSeconds` : durée estimée après conversion
 */
data class DecodingResult(
    val samples: List<FloatArray>,
    val sampleRate: Int,
    val channelCount: Int,
    val totalSamples: Long,
    val durationSeconds: Float,
    val originalDurationUs: Long
) {
    /**
     * Fusionne tous les chunks en un seul tableau PCM (mono float[])
     */
    fun getAllSamplesFlat(): FloatArray {
        val totalSize = samples.sumOf { it.size }
        val result = FloatArray(totalSize)
        var offset = 0
        for (chunk in samples) {
            chunk.copyInto(result, offset)
            offset += chunk.size
        }
        return result
    }

    /**
     * Retourne une description textuelle des stats du décodage
     */
    fun getStats(): String {
        return "Décodage: ${samples.size} chunks, $totalSamples samples, " +
                "${durationSeconds}s, ${sampleRate}Hz, ${channelCount}ch"
    }
}