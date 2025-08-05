package com.lebaillyapp.beatvibrator.domain.visualizer

import androidx.compose.ui.geometry.Offset

/**
 * Data class to hold all uniform parameters required by the AGSL shader
 * for rendering multiple pulse waves.
 * This structure helps in passing coherent data to the shader efficiently.
 */
data class WaveShaderParams(
    val numWaves: Int,                 // The actual number of active waves to render.
    val origins: FloatArray,           // Array of (x,y) coordinates for each wave origin.
    val amplitudes: FloatArray,        // Array of initial amplitudes for each wave.
    val frequencies: FloatArray,       // Array of frequencies for each wave.
    val speeds: FloatArray,            // Array of speeds for each wave.
    val startTimes: FloatArray,        // Array of start times for each wave.
    val globalDamping: Float,          // Global damping factor applied to all waves.
    val minAmplitudeThreshold: Float   // Minimum amplitude for a wave to have visual effect.
) {
    // Custom equals and hashCode for FloatArray content comparison
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WaveShaderParams) return false

        if (numWaves != other.numWaves) return false
        if (!origins.contentEquals(other.origins)) return false
        if (!amplitudes.contentEquals(other.amplitudes)) return false
        if (!frequencies.contentEquals(other.frequencies)) return false
        if (!speeds.contentEquals(other.speeds)) return false
        if (!startTimes.contentEquals(other.startTimes)) return false
        if (globalDamping != other.globalDamping) return false
        if (minAmplitudeThreshold != other.minAmplitudeThreshold) return false

        return true
    }

    override fun hashCode(): Int {
        var result = numWaves
        result = 31 * result + origins.contentHashCode()
        result = 31 * result + amplitudes.contentHashCode()
        result = 31 * result + frequencies.contentHashCode()
        result = 31 * result + speeds.contentHashCode()
        result = 31 * result + startTimes.contentHashCode()
        result = 31 * result + globalDamping.hashCode()
        result = 31 * result + minAmplitudeThreshold.hashCode()
        return result
    }
}