package com.lebaillyapp.beatvibrator.data.service


import android.content.Context
import android.net.Uri
import androidx.annotation.MainThread
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Représente l'état simplifié du player audio.
 *
 * Utilisé pour communiquer via [StateFlow] l'état courant du player à l'UI ou à d'autres consommateurs.
 */
sealed interface PlayerState {
    /** Player prêt mais inactif, aucune lecture lancée. */
    object Idle : PlayerState

    /** Player en cours de préparation (buffering, chargement des médias). */
    object Preparing : PlayerState

    /** Player en lecture active. */
    object Playing : PlayerState

    /** Player en pause. */
    object Paused : PlayerState

    /** Player arrêté (lecture terminée ou stoppée). */
    object Stopped : PlayerState

    /** Player en erreur. */
    data class Error(val throwable: Throwable) : PlayerState
}

/**
 * ## Service de lecture audio basé sur Media3 ExoPlayer (version 1.7.1).
 *
 * Ce service encapsule un lecteur audio complet, exposant son état via des [StateFlow]s,
 * et permettant le contrôle via une API simple (play, pause, stop, release).
 *
 * Ce service est conçu pour être injecté via Hilt et utilisé dans le ViewModel ou ailleurs.
 *
 * ### Responsabilités :
 * - Initialiser, préparer et jouer un fichier audio local (via URI).
 * - Exposer l'état du player ([PlayerState]), la position et la durée actuelle.
 * - Gérer la libération des ressources explicitement via [release].
 * - Suivre et rapporter les erreurs rencontrées pendant la lecture.
 * - Gérer automatiquement la libération à la fin du fichier pour éviter les fuites.
 *
 * ### Notes :
 * - Le cycle de vie du player est géré manuellement : il faut appeler [release]
 *   lorsque le player n'est plus utilisé (ex : destruction du ViewModel).
 * - Utilisation des coroutines et flow pour l'exposition reactive des états.
 * - Annotations `@MainThread` indiquent que les appels doivent se faire sur le thread UI.
 * - Certaines APIs Media3 sont marquées `@UnstableApi` et peuvent changer.
 *
 * @property context Contexte application injecté via Hilt, utilisé pour instancier ExoPlayer.
 */
@Singleton
class AudioPlaybackService @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** CoroutineScope lié au thread principal pour lancer la surveillance de la position. */
    private val scope = CoroutineScope(Dispatchers.Main)

    /** Instance interne du player Media3 ExoPlayer, null si non initialisé. */
    private var player: ExoPlayer? = null

    /** Etat interne mutable, exposé via [playerState] en lecture seule. */
    private val _playerState = MutableStateFlow<PlayerState>(PlayerState.Idle)
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    /** Position courante en millisecondes de la lecture, exposée via Flow. */
    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    /** Durée totale du média en millisecondes, exposée via Flow. */
    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    /** Job coroutine pour la mise à jour périodique de la position pendant la lecture. */
    private var positionJob: Job? = null

    /**
     * Démarre ou reprend la lecture du fichier audio référencé par [uri].
     *
     * Initialise le player si besoin, configure les attributs audio,
     * prépare la source média et lance la lecture en mode "playWhenReady".
     *
     * Lance également une coroutine pour mettre à jour la position et la durée.
     *
     * @param uri URI local du fichier audio à jouer.
     *
     * @throws IllegalArgumentException si l'URI est invalide ou non accessible.
     */
    @OptIn(UnstableApi::class)
    @MainThread
    fun play(uri: Uri) {
        if (player == null) {
            player = ExoPlayer.Builder(context).build().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(),
                    true
                )
                addListener(playerListener)
            }
        }

        _playerState.value = PlayerState.Preparing

        val mediaSource = buildMediaSource(uri)

        player?.apply {
            setMediaSource(mediaSource)
            prepare()
            playWhenReady = true
        }

        // Lance une coroutine qui met à jour régulièrement la position et durée
        positionJob?.cancel()
        positionJob = scope.launch {
            while (player != null && player!!.isPlaying) {
                _currentPositionMs.value = player!!.currentPosition
                _durationMs.value = player!!.duration.takeIf { it != C.TIME_UNSET } ?: 0L
                delay(200)
            }
        }
    }

    /**
     * Met en pause la lecture si le player est en lecture.
     *
     * Ne fait rien si le player n'est pas initialisé ou déjà en pause.
     */
    @MainThread
    fun pause() {
        player?.let {
            if (it.isPlaying) {
                it.playWhenReady = false
                _playerState.value = PlayerState.Paused
            }
        }
    }

    /**
     * Stoppe la lecture en cours, remet la position à zéro,
     * et met le player dans l'état [PlayerState.Stopped].
     *
     * Annule également la mise à jour périodique de la position.
     */
    @MainThread
    fun stop() {
        player?.let {
            it.stop()
            _playerState.value = PlayerState.Stopped
            _currentPositionMs.value = 0L
        }
        positionJob?.cancel()
    }

    /**
     * Libère les ressources du player ExoPlayer.
     *
     * Cette méthode doit être appelée impérativement lors de la destruction
     * du ViewModel ou de l'application pour éviter les fuites mémoire.
     *
     * Annule aussi la mise à jour périodique de la position et remet
     * tous les flows à leur état initial.
     */
    @MainThread
    fun release() {
        positionJob?.cancel()
        player?.run {
            removeListener(playerListener)
            release()
        }
        player = null
        _playerState.value = PlayerState.Idle
        _currentPositionMs.value = 0L
        _durationMs.value = 0L
    }

    /**
     * Construit un [MediaSource] pour Media3 ExoPlayer à partir d'un [Uri] local.
     *
     * Utilise un [DefaultDataSource.Factory] et un [ProgressiveMediaSource.Factory]
     * pour lire les fichiers classiques (MP3, WAV, etc).
     *
     * @param uri URI du fichier audio local.
     * @return MediaSource prêt à être utilisé par ExoPlayer.
     */
    @OptIn(UnstableApi::class)
    private fun buildMediaSource(uri: Uri): MediaSource {
        val dataSourceFactory = DefaultDataSource.Factory(context)
        return ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(uri))
    }

    /**
     * Listener ExoPlayer pour suivre les changements d'état et erreurs.
     *
     * Met à jour le [StateFlow] interne [_playerState] en fonction
     * des événements reçus du player.
     */
    private val playerListener = object : Player.Listener {

        override fun onPlaybackStateChanged(state: Int) {
            when (state) {
                Player.STATE_BUFFERING -> _playerState.value = PlayerState.Preparing
                Player.STATE_READY -> {
                    if (player?.playWhenReady == true) {
                        _playerState.value = PlayerState.Playing
                    } else {
                        _playerState.value = PlayerState.Paused
                    }
                }
                Player.STATE_ENDED -> {
                    _playerState.value = PlayerState.Stopped
                    // Libération automatique à la fin du fichier pour éviter fuites mémoire
                    release()
                }
                Player.STATE_IDLE -> {
                    _playerState.value = PlayerState.Idle
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _playerState.value = PlayerState.Error(error)
        }
    }
}