package com.beatvibrator.data.service

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.lebaillyapp.beatvibrator.domain.DecodingResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ## Service de décodage audio offline (MP3 → PCM)
 *
 * Ce service utilise MediaExtractor + MediaCodec pour convertir efficacement
 * un fichier MP3 en échantillons PCM (float[]), utilisables ensuite pour des
 * traitements DSP ou des motifs de vibration synchronisés.
 *
 * ### Pipeline :
 * 1. Extraction des frames audio via `MediaExtractor`
 * 2. Décodage rapide en mode offline via `MediaCodec`
 * 3. Conversion PCM brute → FloatArray mono
 * 4. Stockage par chunks pour éviter OOM
 *
 * ### Avantages :
 * -  Rapide et sans playback audio
 * -  Pas besoin d'ExoPlayer en lecture silencieuse
 * -  Format float[] normalisé mono (de -1.0 à +1.0)
 * -  Progression trackable via `StateFlow`
 *
 * Utilisation typique dans un `ViewModel` :
 * ```
 * val result = audioProcessorService.decodeAudioFile(myUri)
 * val samples = result.getAllSamplesFlat()
 * ```
 */
@Singleton
@OptIn(UnstableApi::class)
class AudioProcessorService @Inject constructor(
    private val context: Context
) {

    companion object {
        private const val TIMEOUT_US = 10_000L // Timeout MediaCodec (µs)
        private const val CHUNK_SIZE = 4096    // Taille d’un bloc de PCM
    }

    // === ÉTAT DU DÉCODAGE ===

    /**
     * Représente l’état global du décodage
     */
    sealed class DecodingState {
        object Idle : DecodingState()
        data class Processing(val progress: Float) : DecodingState()
        data class Completed(val result: DecodingResult) : DecodingState()
        data class Error(val message: String) : DecodingState()
    }

    private val _decodingState = MutableStateFlow<DecodingState>(DecodingState.Idle)

    /**
     * Flux d’état observable depuis le ViewModel ou l’UI
     */
    val decodingState: StateFlow<DecodingState> = _decodingState.asStateFlow()

    // === DÉCODAGE PRINCIPAL ===

    /**
     * Décode un fichier MP3 donné en échantillons PCM mono normalisés
     *
     * @param fileUri URI du fichier MP3 à décoder
     * @return Un [DecodingResult] contenant les données PCM
     * @throws Exception si le décodage échoue
     */
    suspend fun decodeAudioFile(fileUri: Uri): DecodingResult = withContext(Dispatchers.IO) {
        _decodingState.value = DecodingState.Processing(0f)

        var mediaExtractor: MediaExtractor? = null
        var mediaCodec: MediaCodec? = null

        try {
            // === 1. Initialisation MediaExtractor ===
            mediaExtractor = MediaExtractor().apply {
                setDataSource(context, fileUri, null)
            }

            val audioTrackIndex = findAudioTrack(mediaExtractor)
            if (audioTrackIndex < 0) throw IllegalArgumentException("Aucune piste audio trouvée")

            mediaExtractor.selectTrack(audioTrackIndex)
            val format = mediaExtractor.getTrackFormat(audioTrackIndex)

            // === 2. Configuration MediaCodec ===
            val mimeType = format.getString(MediaFormat.KEY_MIME)
                ?: throw IllegalArgumentException("Type MIME absent")
            mediaCodec = MediaCodec.createDecoderByType(mimeType).apply {
                configure(format, null, null, 0)
                start()
            }

            // === 3. Métadonnées audio ===
            val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) {
                format.getLong(MediaFormat.KEY_DURATION)
            } else {
                0L
            }

            // === 4. Boucle de décodage ===
            val allSamples = mutableListOf<FloatArray>()
            var totalSamples = 0L
            var isInputComplete = false
            var isOutputComplete = false

            val inputBuffers = mediaCodec.inputBuffers
            val outputBuffers = mediaCodec.outputBuffers
            val bufferInfo = MediaCodec.BufferInfo()

            while (!isOutputComplete) {
                // --- Entrée : lire MP3 brut et alimenter MediaCodec ---
                if (!isInputComplete) {
                    val inIndex = mediaCodec.dequeueInputBuffer(50_000)
                    if (inIndex >= 0) {
                        val inputBuffer = mediaCodec.getInputBuffer(inIndex) ?: continue
                        inputBuffer.clear()

                        val sampleSize = mediaExtractor.readSampleData(inputBuffer, 0)
                        if (sampleSize >= 0) {
                            val pts = mediaExtractor.sampleTime
                            mediaCodec.queueInputBuffer(inIndex, 0, sampleSize, pts, 0)
                            mediaExtractor.advance()

                            // Progrès linéaire
                            if (durationUs > 0) {
                                val progress = (pts.toFloat() / durationUs).coerceIn(0f, 1f)
                                _decodingState.value = DecodingState.Processing(progress)
                            }
                        } else {
                            mediaCodec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            isInputComplete = true
                        }
                    }
                }

                // --- Sortie : lire PCM décodé et convertir ---
                val outIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 50_000)
                when {
                    outIndex >= 0 -> {
                        val outputBuffer = mediaCodec.getOutputBuffer(outIndex) ?: continue
                        if (bufferInfo.size > 0) {
                            val pcmSamples = extractPcmSamples(outputBuffer, bufferInfo, channelCount)
                            allSamples.add(pcmSamples)
                            totalSamples += pcmSamples.size
                        }
                        mediaCodec.releaseOutputBuffer(outIndex, false)

                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            isOutputComplete = true
                        }
                    }
                    outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        // Format dynamique ignoré ici
                    }
                }
            }

            // === 5. Construction du résultat final ===
            val result = DecodingResult(
                samples = allSamples,
                sampleRate = sampleRate,
                channelCount = channelCount,
                totalSamples = totalSamples,
                durationSeconds = totalSamples.toFloat() / sampleRate,
                originalDurationUs = durationUs
            )
            _decodingState.value = DecodingState.Completed(result)
            result
        } catch (e: Exception) {
            _decodingState.value = DecodingState.Error("Erreur: ${e.message}")
            throw e
        } finally {
            try {
                mediaCodec?.stop(); mediaCodec?.release()
                mediaExtractor?.release()
            } catch (_: Exception) {
            }
        }
    }

    // === OUTILS INTERNES ===

    /**
     * Recherche la première piste audio dans un `MediaExtractor`
     *
     * @return Index de la piste audio, ou -1 si aucune trouvée
     */
    private fun findAudioTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) return i
        }
        return -1
    }

    /**
     * Extrait un tableau PCM (float[] mono) à partir d’un buffer MediaCodec
     *
     * @param buffer Buffer brut de sortie
     * @param bufferInfo Métadonnées du chunk
     * @param channelCount Nombre de canaux (1 = mono, 2 = stéréo)
     */
    private fun extractPcmSamples(
        buffer: ByteBuffer,
        bufferInfo: MediaCodec.BufferInfo,
        channelCount: Int
    ): FloatArray {
        buffer.position(bufferInfo.offset)
        buffer.limit(bufferInfo.offset + bufferInfo.size)

        val shortBuffer = buffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val rawSamples = ShortArray(shortBuffer.remaining())
        shortBuffer.get(rawSamples)

        return convertToFloatSamples(rawSamples, channelCount)
    }

    /**
     * Convertit des données PCM 16-bit (short[]) en float[] mono normalisé
     *
     * @param samples Tableau brut de short PCM
     * @param channelCount Nombre de canaux
     * @return Tableau mono en float, normalisé [-1f, +1f]
     */
    private fun convertToFloatSamples(samples: ShortArray, channelCount: Int): FloatArray {
        return when (channelCount) {
            1 -> FloatArray(samples.size) { i ->
                samples[i] / Short.MAX_VALUE.toFloat()
            }
            2 -> FloatArray(samples.size / 2) { i ->
                val left = samples[i * 2] / Short.MAX_VALUE.toFloat()
                val right = samples[i * 2 + 1] / Short.MAX_VALUE.toFloat()
                (left + right) * 0.5f
            }
            else -> FloatArray(samples.size / channelCount) { i ->
                var sum = 0f
                for (ch in 0 until channelCount) {
                    sum += samples[i * channelCount + ch] / Short.MAX_VALUE.toFloat()
                }
                sum / channelCount
            }
        }
    }

    /**
     * Réinitialise l’état interne du service (utile après une erreur)
     */
    fun reset() {
        _decodingState.value = DecodingState.Idle
    }
}
