package com.lebaillyapp.beatvibrator.data.service

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.lebaillyapp.beatvibrator.domain.audioProcess.DecodingResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ## Service de décodage audio offline (MP3 → PCM) - Version Optimisée
 *
 * Ce service utilise MediaExtractor + MediaCodec pour convertir efficacement
 * un fichier MP3 en échantillons PCM (float[]), utilisables ensuite pour des
 * traitements DSP afin de générer des motifs de vibration synchronisés.
 *
 * ### Améliorations v2 :
 * - Buffer management moderne (pas de deprecated APIs)
 * - Throttling des updates StateFlow
 * - Timeouts optimisés
 * - Gestion mémoire améliorée
 * - Validation robuste des formats
 */
@Singleton
@OptIn(UnstableApi::class)
class AudioProcessorService @Inject constructor(
    private val context: Context
) {

    companion object {
        private const val DEQUEUE_TIMEOUT_US = 10_000L // 10ms timeout
        private const val CHUNK_SIZE = 4096
        private const val PROGRESS_UPDATE_INTERVAL_MS = 100L // Throttle progress updates
    }

    // === ÉTAT DU DÉCODAGE ===

    sealed class DecodingState {
        object Idle : DecodingState()
        data class Processing(val progress: Float) : DecodingState()
        data class Completed(val result: DecodingResult) : DecodingState()
        data class Error(val message: String) : DecodingState()
    }

    private val _decodingState = MutableStateFlow<DecodingState>(DecodingState.Idle)
    val decodingState: StateFlow<DecodingState> = _decodingState.asStateFlow()

    // === DÉCODAGE PRINCIPAL ===

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
            if (audioTrackIndex < 0) {
                throw IllegalArgumentException("Aucune piste audio trouvée dans le fichier")
            }

            mediaExtractor.selectTrack(audioTrackIndex)
            val format = mediaExtractor.getTrackFormat(audioTrackIndex)

            // === 2. Validation et configuration MediaCodec ===
            val mimeType = format.getString(MediaFormat.KEY_MIME)
                ?: throw IllegalArgumentException("Type MIME absent")

            if (!mimeType.startsWith("audio/")) {
                throw IllegalArgumentException("Format audio non supporté: $mimeType")
            }

            mediaCodec = MediaCodec.createDecoderByType(mimeType).apply {
                configure(format, null, null, 0)
                start()
            }

            // === 3. Métadonnées audio avec validation ===
            val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) {
                format.getLong(MediaFormat.KEY_DURATION)
            } else {
                0L
            }

            // Validation des métadonnées
            if (sampleRate <= 0 || channelCount <= 0) {
                throw IllegalArgumentException("Métadonnées audio invalides: SR=$sampleRate, CH=$channelCount")
            }

            // === 4. Boucle de décodage optimisée ===
            val allSamples = mutableListOf<FloatArray>()
            var totalSamples = 0L
            var isInputComplete = false
            var isOutputComplete = false
            var lastProgressUpdate = 0L

            val bufferInfo = MediaCodec.BufferInfo()

            while (!isOutputComplete) {
                // --- Entrée : lire MP3 brut et alimenter MediaCodec ---
                if (!isInputComplete) {
                    val inIndex = mediaCodec.dequeueInputBuffer(DEQUEUE_TIMEOUT_US)
                    if (inIndex >= 0) {
                        val inputBuffer = mediaCodec.getInputBuffer(inIndex)
                        if (inputBuffer != null) {
                            inputBuffer.clear()

                            val sampleSize = mediaExtractor.readSampleData(inputBuffer, 0)
                            if (sampleSize >= 0) {
                                val presentationTimeUs = mediaExtractor.sampleTime
                                mediaCodec.queueInputBuffer(inIndex, 0, sampleSize, presentationTimeUs, 0)
                                mediaExtractor.advance()

                                // Throttled progress updates
                                val currentTime = System.currentTimeMillis()
                                if (durationUs > 0 && currentTime - lastProgressUpdate > PROGRESS_UPDATE_INTERVAL_MS) {
                                    val progress = (presentationTimeUs.toFloat() / durationUs).coerceIn(0f, 0.95f)
                                    _decodingState.value = DecodingState.Processing(progress)
                                    lastProgressUpdate = currentTime
                                }
                            } else {
                                // End of stream
                                mediaCodec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                isInputComplete = true
                            }
                        }
                    }
                }

                // --- Sortie : lire PCM décodé et convertir ---
                val outIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, DEQUEUE_TIMEOUT_US)
                when {
                    outIndex >= 0 -> {
                        val outputBuffer = mediaCodec.getOutputBuffer(outIndex)
                        if (outputBuffer != null && bufferInfo.size > 0) {
                            val pcmSamples = extractPcmSamples(outputBuffer, bufferInfo, channelCount)
                            if (pcmSamples.isNotEmpty()) {
                                allSamples.add(pcmSamples)
                                totalSamples += pcmSamples.size
                            }
                        }
                        mediaCodec.releaseOutputBuffer(outIndex, false)

                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            isOutputComplete = true
                        }
                    }
                    outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        // Format change - usually safe to ignore for audio
                        val newFormat = mediaCodec.outputFormat
                        // Log.d("AudioProcessor", "Output format changed: $newFormat")
                    }
                    outIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        // Normal - just continue
                    }
                    else -> {
                        // Unexpected index, continue
                    }
                }
            }

            // === 5. Validation et construction du résultat ===
            if (allSamples.isEmpty()) {
                throw IllegalStateException("Aucun échantillon audio décodé")
            }

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
            val errorMsg = "Erreur décodage audio: ${e.message}"
            _decodingState.value = DecodingState.Error(errorMsg)
            throw IllegalStateException(errorMsg, e)
        } finally {
            // Nettoyage robuste
            try {
                mediaCodec?.stop()
            } catch (e: Exception) {
                // Ignore stop errors
            }
            try {
                mediaCodec?.release()
                mediaExtractor?.release()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
    }

    // === OUTILS INTERNES ===

    private fun findAudioTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) {
                return i
            }
        }
        return -1
    }

    private fun extractPcmSamples(
        buffer: ByteBuffer,
        bufferInfo: MediaCodec.BufferInfo,
        channelCount: Int
    ): FloatArray {
        // Validation buffer
        if (bufferInfo.size <= 0) return floatArrayOf()

        buffer.position(bufferInfo.offset)
        buffer.limit(bufferInfo.offset + bufferInfo.size)

        val shortBuffer = buffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val remainingSamples = shortBuffer.remaining()

        if (remainingSamples <= 0) return floatArrayOf()

        val rawSamples = ShortArray(remainingSamples)
        shortBuffer.get(rawSamples)

        return convertToFloatSamples(rawSamples, channelCount)
    }

    private fun convertToFloatSamples(samples: ShortArray, channelCount: Int): FloatArray {
        if (samples.isEmpty()) return floatArrayOf()

        return when (channelCount) {
            1 -> {
                // Mono direct
                FloatArray(samples.size) { i ->
                    samples[i] / Short.MAX_VALUE.toFloat()
                }
            }
            2 -> {
                // Stéréo → Mono (moyennage)
                FloatArray(samples.size / 2) { i ->
                    val left = samples[i * 2] / Short.MAX_VALUE.toFloat()
                    val right = samples[i * 2 + 1] / Short.MAX_VALUE.toFloat()
                    (left + right) * 0.5f
                }
            }
            else -> {
                // Multi-canal → Mono (moyennage)
                FloatArray(samples.size / channelCount) { i ->
                    var sum = 0f
                    for (ch in 0 until channelCount) {
                        sum += samples[i * channelCount + ch] / Short.MAX_VALUE.toFloat()
                    }
                    sum / channelCount
                }
            }
        }
    }

    fun reset() {
        _decodingState.value = DecodingState.Idle
    }
}