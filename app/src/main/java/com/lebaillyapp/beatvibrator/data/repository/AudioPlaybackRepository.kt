package com.lebaillyapp.beatvibrator.data.repository

import android.net.Uri
import com.lebaillyapp.beatvibrator.data.service.AudioPlaybackService
import com.lebaillyapp.beatvibrator.data.service.PlayerState
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers


/**
 * ## Repository pour la lecture audio.
 *
 * Il sert de pont entre le ViewModel et le [AudioPlaybackService],
 * exposant un StateFlow pour l'état du player ainsi que des méthodes
 * pour contrôler la lecture audio.
 *
 * Ce repository encapsule la logique métier liée à la lecture audio
 * et garantit que tous les appels au service se font sur le thread principal.
 *
 * ### Responsabilités :
 * - Exposer les états du player via des StateFlow
 * - Contrôler la lecture audio (play, pause, resume, stop, seek)
 * - Gérer la libération des ressources
 * - Assurer la cohérence des appels sur le thread UI
 * - Fournir des méthodes pratiques pour l'état du player
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
     * Cette méthode initialise le player si nécessaire et commence la lecture.
     * Elle garantit que l'appel se fait sur le thread principal.
     *
     * @param uri URI du fichier audio local à jouer.
     * @throws IllegalArgumentException si l'URI est invalide ou non accessible.
     */
    suspend fun play(uri: Uri) {
        withContext(Dispatchers.Main) {
            audioPlaybackService.play(uri)
        }
    }

    /**
     * Met en pause la lecture audio.
     *
     * Ne fait rien si le player n'est pas en cours de lecture.
     */
    suspend fun pause() {
        withContext(Dispatchers.Main) {
            audioPlaybackService.pause()
        }
    }

    /**
     * Reprend la lecture audio si elle était en pause.
     *
     * Ne fait rien si le player n'est pas en pause ou pas prêt.
     */
    suspend fun resume() {
        withContext(Dispatchers.Main) {
            audioPlaybackService.resume()
        }
    }

    /**
     * Stoppe la lecture audio et remet la position à zéro.
     */
    suspend fun stop() {
        withContext(Dispatchers.Main) {
            audioPlaybackService.stop()
        }
    }

    /**
     * Navigue à une position spécifique dans le fichier audio.
     *
     * @param positionMs Position en millisecondes où positionner la lecture.
     */
    suspend fun seekTo(positionMs: Long) {
        withContext(Dispatchers.Main) {
            audioPlaybackService.seekTo(positionMs)
        }
    }

    /**
     * Libère les ressources du player.
     *
     * Cette méthode doit être appelée lors de la destruction du ViewModel
     * ou de l'application pour éviter les fuites mémoire.
     */
    suspend fun release() {
        withContext(Dispatchers.Main) {
            audioPlaybackService.release()
        }
    }

    // --- Méthodes utilitaires pour l'état ---

    /**
     * Vérifie si le player est actuellement en train de jouer.
     *
     * @return true si le player est dans l'état Playing, false sinon.
     */
    fun isPlaying(): Boolean = playerState.value is PlayerState.Playing

    /**
     * Vérifie si le player est en pause.
     *
     * @return true si le player est dans l'état Paused, false sinon.
     */
    fun isPaused(): Boolean = playerState.value is PlayerState.Paused

    /**
     * Vérifie si le player est arrêté.
     *
     * @return true si le player est dans l'état Stopped, false sinon.
     */
    fun isStopped(): Boolean = playerState.value is PlayerState.Stopped

    /**
     * Vérifie si le player est en erreur.
     *
     * @return true si le player est dans l'état Error, false sinon.
     */
    fun isError(): Boolean = playerState.value is PlayerState.Error

    /**
     * Vérifie si le player est inactif.
     *
     * @return true si le player est dans l'état Idle, false sinon.
     */
    fun isIdle(): Boolean = playerState.value is PlayerState.Idle

    /**
     * Vérifie si le player est en cours de préparation.
     *
     * @return true si le player est dans l'état Preparing, false sinon.
     */
    fun isPreparing(): Boolean = playerState.value is PlayerState.Preparing

    /**
     * Récupère l'erreur si le player est dans l'état Error.
     *
     * @return Throwable si le player est en erreur, null sinon.
     */
    fun getError(): Throwable? {
        return when (val state = playerState.value) {
            is PlayerState.Error -> state.throwable
            else -> null
        }
    }

    /**
     * Calcule le pourcentage de progression de la lecture.
     *
     * @return Pourcentage de progression (0.0 à 1.0), 0.0 si la durée est inconnue.
     */
    fun getProgressPercentage(): Float {
        val duration = durationMs.value
        val position = currentPositionMs.value

        return if (duration > 0) {
            (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    }

    /**
     * Formate la position courante en chaîne de caractères (mm:ss).
     *
     * @return Position formatée (ex: "01:30").
     */
    fun getFormattedCurrentPosition(): String {
        return formatTime(currentPositionMs.value)
    }

    /**
     * Formate la durée totale en chaîne de caractères (mm:ss).
     *
     * @return Durée formatée (ex: "03:45").
     */
    fun getFormattedDuration(): String {
        return formatTime(durationMs.value)
    }

    /**
     * Formate un temps en millisecondes en chaîne mm:ss.
     *
     * @param timeMs Temps en millisecondes.
     * @return Temps formaté (ex: "02:30").
     */
    private fun formatTime(timeMs: Long): String {
        val totalSeconds = timeMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    // --- Méthodes de contrôle avancées ---

    /**
     * Bascule entre lecture et pause.
     *
     * Si le player est en lecture, le met en pause.
     * Si le player est en pause, reprend la lecture.
     */
    suspend fun togglePlayPause() {
        when {
            isPlaying() -> pause()
            isPaused() -> resume()
        }
    }

    /**
     * Avance de quelques secondes dans la lecture.
     *
     * @param seconds Nombre de secondes à avancer (par défaut 10).
     */
    suspend fun skipForward(seconds: Int = 10) {
        val newPosition = currentPositionMs.value + (seconds * 1000)
        val maxPosition = durationMs.value

        seekTo(if (maxPosition > 0) newPosition.coerceAtMost(maxPosition) else newPosition)
    }

    /**
     * Recule de quelques secondes dans la lecture.
     *
     * @param seconds Nombre de secondes à reculer (par défaut 10).
     */
    suspend fun skipBackward(seconds: Int = 10) {
        val newPosition = currentPositionMs.value - (seconds * 1000)
        seekTo(newPosition.coerceAtLeast(0))
    }

    /**
     * Navigue à un pourcentage spécifique de la durée totale.
     *
     * @param percentage Pourcentage de la durée (0.0 à 1.0).
     */
    suspend fun seekToPercentage(percentage: Float) {
        val duration = durationMs.value
        if (duration > 0) {
            val position = (duration * percentage.coerceIn(0f, 1f)).toLong()
            seekTo(position)
        }
    }
}