package com.lebaillyapp.beatvibrator.domain.dsp

import kotlin.math.*

/**
 * FFT (Fast Fourier Transform) maison, radix-2, pour analyse spectrale.
 *
 * Utile pour extraire la magnitude fréquentielle sur des fenêtres glissantes.
 */
class FFT {

    /**
     * Calcule un spectrogramme (liste de spectres de magnitude) sur un signal PCM.
     *
     * @param samples Signal audio brut (mono, float normalisé [-1,1])
     * @param fftSize Taille de la fenêtre FFT (doit être une puissance de 2)
     * @param hopSize Pas entre chaque fenêtre (en échantillons)
     * @return Liste des spectres magnitude (FloatArray de fftSize/2 + 1 fréquences)
     */
    fun computeSpectrogram(
        samples: FloatArray,
        fftSize: Int,
        hopSize: Int
    ): List<FloatArray> {
        require(fftSize > 0 && (fftSize and (fftSize - 1)) == 0) { "fftSize doit être une puissance de 2" }

        val spectrogram = mutableListOf<FloatArray>()
        val window = hammingWindow(fftSize)

        var pos = 0
        while (pos + fftSize <= samples.size) {
            // Appliquer la fenêtre + copier dans un tableau complexe (réel + imaginaire)
            val real = DoubleArray(fftSize)
            val imag = DoubleArray(fftSize) { 0.0 }

            for (i in 0 until fftSize) {
                real[i] = (samples[pos + i] * window[i]).toDouble()
            }

            // Calcul FFT in-place
            fft(real, imag)

            // Calcul magnitude spectrale (valeurs positives seulement, jusqu'à Nyquist)
            val magnitudes = FloatArray(fftSize / 2 + 1)
            for (i in 0..fftSize / 2) {
                magnitudes[i] = sqrt(real[i] * real[i] + imag[i] * imag[i]).toFloat()
            }

            spectrogram.add(magnitudes)
            pos += hopSize
        }

        return spectrogram
    }

    /**
     * Fenêtre de Hamming classique pour atténuer la fuite spectrale.
     */
    private fun hammingWindow(size: Int): FloatArray {
        return FloatArray(size) { i ->
            (0.54 - 0.46 * cos(2.0 * PI * i / (size - 1))).toFloat()
        }
    }

    /**
     * Algorithme FFT radix-2 in-place.
     * Modifie les tableaux real et imag pour contenir la transformée.
     */
    private fun fft(real: DoubleArray, imag: DoubleArray) {
        val n = real.size
        val levels = (ln(n.toDouble()) / ln(2.0)).toInt()
        require(1 shl levels == n) { "La taille doit être une puissance de 2" }

        // Bit-reverse permutation
        for (i in 0 until n) {
            val j = reverseBits(i, levels)
            if (j > i) {
                val tempReal = real[i]
                val tempImag = imag[i]
                real[i] = real[j]
                imag[i] = imag[j]
                real[j] = tempReal
                imag[j] = tempImag
            }
        }

        // Danielson-Lanczos section
        var size = 2
        while (size <= n) {
            val halfSize = size / 2
            val tableStep = n / size
            for (i in 0 until n step size) {
                for (j in 0 until halfSize) {
                    val k = j * tableStep
                    val angle = -2.0 * PI * k / n
                    val cos = cos(angle)
                    val sin = sin(angle)

                    val tReal = cos * real[i + j + halfSize] - sin * imag[i + j + halfSize]
                    val tImag = sin * real[i + j + halfSize] + cos * imag[i + j + halfSize]

                    real[i + j + halfSize] = real[i + j] - tReal
                    imag[i + j + halfSize] = imag[i + j] - tImag

                    real[i + j] += tReal
                    imag[i + j] += tImag
                }
            }
            size *= 2
        }
    }

    /**
     * Inverse des bits sur `bits` bits.
     * Exemple: reverseBits(6 (110), 3) -> 3 (011)
     */
    private fun reverseBits(x: Int, bits: Int): Int {
        var y = 0
        for (i in 0 until bits) {
            y = (y shl 1) or (x shr i and 1)
        }
        return y
    }
}