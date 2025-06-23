package com.lebaillyapp.beatvibrator.domain.postProcess

import com.lebaillyapp.beatvibrator.domain.VibrationPulse
import kotlin.math.pow
import kotlin.math.round

/**
 * ## PostProcessor
 *
 * PostProcessor est un module de traitement final permettant de convertir des événements d'onsets
 * (détectés en amont via FFT, RMS, etc.) en une séquence propre et perceptible de pulses vibratoires.
 *
 * Ce composant applique plusieurs techniques de filtrage, compression et quantification
 * pour améliorer le rendu final sur un vibreur à résonance linéaire (LRA), notamment :
 *
 * - Suppression des bruits faibles (anti-bruit)
 * - Regroupement adaptatif des pulses trop proches
 * - Compression logarithmique douce de l’intensité (perceptuelle)
 * - Calcul dynamique de la durée du pulse en fonction de l’intensité
 * - Alignement optionnel sur une grille rythmique (quantification BPM)
 * - Nettoyage final via tri temporel et clamping de l’intensité
 *
 * L’objectif est de produire un pattern vibrant rythmé, expressif et perceptible,
 * qui retranscrit au mieux les éléments percussifs ou basse fréquence d’un fichier audio.
 */
object PostProcessor {

    private const val MIN_INTERVAL_MS = 80L      // Intervalle minimum entre deux pulses (évite les doublons trop rapprochés)
    private const val MERGE_WINDOW_MS = 120L     // Fenêtre de regroupement pour fusionner plusieurs onsets proches
    private const val MIN_INTENSITY = 0.08f      // Seuil minimal pour considérer un onset comme significatif
    private const val BASE_DURATION_MS = 40L     // Durée minimale de base des pulses (ajustée dynamiquement ensuite)
    private const val MAX_DURATION_MS = 100L     // Durée maximale autorisée
    private const val MIN_DURATION_MS = 20L      // Durée minimale autorisée (protection anti-buzz)

    /**
     * Convertit une liste brute d'onsets (timestamp + amplitude) en une séquence de pulses vibratoires optimisée.
     *
     * @param onsets Liste d’événements sous forme de paires (timestamp en ms, amplitude normalisée [0f..1f])
     * @param bpm Optionnel : valeur de tempo pour quantifier le timing des pulses sur une grille rythmique.
     *            Utile pour synchroniser les vibrations avec la pulsation musicale.
     * @param division Nombre de subdivisions de temps par battement. Exemple : 4 = 1/16 notes si bpm = 120.
     *
     * @return Liste ordonnée de [VibrationPulse], nettoyée, compressée et adaptée à la lecture par VibratorManager.
     */
    fun processOnsets(
        onsets: List<Pair<Long, Float>>,
        bpm: Int? = null,
        division: Int = 4
    ): List<VibrationPulse> {
        if (onsets.isEmpty()) return emptyList()

        val sorted = onsets.sortedBy { it.first }
        val pulses = mutableListOf<VibrationPulse>()

        var lastTime = -MIN_INTERVAL_MS

        var i = 0
        while (i < sorted.size) {
            val (baseTime, baseAmp) = sorted[i]

            // Ignore les pulses trop faibles ou trop rapprochés
            if (baseAmp < MIN_INTENSITY || baseTime - lastTime < MIN_INTERVAL_MS) {
                i++
                continue
            }

            // --- Fusion des pulses proches dans une même fenêtre ---
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

            // --- Compression douce de l’intensité (soft-knee perceptuelle) ---
            val compressedAmp = mergedAmp.coerceIn(0f, 1f).pow(0.6f)

            // --- Durée ajustée dynamiquement selon l’intensité ---
            val dynamicDuration = (BASE_DURATION_MS + (compressedAmp * (MAX_DURATION_MS - BASE_DURATION_MS)))
                .toLong()
                .coerceIn(MIN_DURATION_MS, MAX_DURATION_MS)

            // --- Option de snapping sur grille rythmique si BPM connu ---
            val finalTime = bpm?.let {
                val gridMs = 60_000f / it / division
                round(mergedTime / gridMs) * gridMs
            }?.toLong() ?: mergedTime.toLong()

            // --- Création du pulse vibratoire final ---
            pulses.add(
                VibrationPulse(
                    timeMs = finalTime,
                    intensity = compressedAmp.coerceIn(0.15f, 1.0f), // clamp bas pour éviter les buzz inutiles
                    durationMs = dynamicDuration
                )
            )

            lastTime = baseTime
            i = j
        }

        return pulses.sortedBy { it.timeMs }
    }
}