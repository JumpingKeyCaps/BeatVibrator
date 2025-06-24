package com.lebaillyapp.beatvibrator.data.repository

import android.net.Uri
import com.lebaillyapp.beatvibrator.data.service.AudioProcessorService
import com.lebaillyapp.beatvibrator.domain.audioProcess.DecodingResult
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * ## Repository d'encapsulation du décodage audio (MP3 → PCM)
 *
 * Ce repository fait le lien entre le ViewModel et le service [AudioProcessorService].
 * Il expose un accès en lecture à l'état du décodage, et une méthode de déclenchement
 * du décodage sans lancer de coroutine (le ViewModel reste maître du scope).
 */
class AudioDecodingRepository @Inject constructor(
    private val audioProcessorService: AudioProcessorService
) {

    /** État du décodage audio, exposé en lecture seule */
    val decodingState: StateFlow<AudioProcessorService.DecodingState> =
        audioProcessorService.decodingState

    /**
     * Déclenche le décodage du fichier audio spécifié.
     * À appeler depuis une coroutine du ViewModel.
     *
     * @param uri URI du fichier MP3 à décoder.
     * @return Résultat du décodage contenant les échantillons PCM.
     * @throws IllegalStateException si le décodage échoue.
     */
    suspend fun decode(uri: Uri): DecodingResult {
        return audioProcessorService.decodeAudioFile(uri)
    }

    /**
     * Réinitialise l'état du décodage.
     * Peut être appelé depuis le ViewModel pour repartir de zéro.
     */
    fun reset() {
        audioProcessorService.reset()
    }
}