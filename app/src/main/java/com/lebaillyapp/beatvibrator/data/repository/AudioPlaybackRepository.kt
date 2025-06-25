package com.lebaillyapp.beatvibrator.data.repository

import android.net.Uri
import com.lebaillyapp.beatvibrator.data.service.AudioPlaybackService
import com.lebaillyapp.beatvibrator.data.service.PlayerState
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ## Repository pour la lecture audio.
 *
 * Il sert de pont entre le ViewModel et le [AudioPlaybackService],
 * exposant un StateFlow pour l'état du player ainsi que des méthodes
 * pour contrôler la lecture audio.
 *
 * @property audioPlaybackService instance du service de lecture audio injecté.
 */
@Singleton
class AudioPlaybackRepository @Inject constructor(
    private val audioPlaybackService: AudioPlaybackService,
) {

    /**
     * État courant du player audio (Idle, Playing, Paused, Error, etc.).
     */
    val playerState: StateFlow<PlayerState> = audioPlaybackService.playerState

    /**
     * Position courante de lecture en millisecondes.
     */
    val currentPositionMs: StateFlow<Long> = audioPlaybackService.currentPositionMs

    /**
     * Durée totale du média en millisecondes.
     */
    val durationMs: StateFlow<Long> = audioPlaybackService.durationMs

    /**
     * Démarre la lecture du fichier audio situé à l'URI donné.
     *
     * @param uri URI du fichier audio local à jouer.
     */
    suspend fun play(uri: Uri) {
        // Le play() dans le service est synchrone, on peut wrapper en suspend si besoin
        audioPlaybackService.play(uri)
    }

    /**
     * Met en pause la lecture audio.
     */
    suspend fun pause() {
        audioPlaybackService.pause()
    }

    /**
     * Stoppe la lecture audio.
     */
    suspend fun stop() {
        audioPlaybackService.stop()
    }

    /**
     * Libère les ressources du player.
     */
    suspend fun release() {
        audioPlaybackService.release()
    }
}