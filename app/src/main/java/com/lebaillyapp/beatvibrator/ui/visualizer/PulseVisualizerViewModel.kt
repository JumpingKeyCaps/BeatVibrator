package com.lebaillyapp.beatvibrator.ui.visualizer

import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lebaillyapp.beatvibrator.domain.visualizer.Wave
import com.lebaillyapp.beatvibrator.domain.visualizer.WaveShaderParams
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.random.Random

/**
 * Pulse Visualizer ViewModel to manage active pulse waves state for AGSL shader deformation.
 * Handles pulse lifecycle, simulated emission, cleanup, and provides
 * uniform data formatted for the shader.
 */
class PulseVisualizerViewModel : ViewModel() {

    // --- Configuration des paramètres des ondes (ajustables pour l'effet visuel) ---
    /** Global damping factor controlling amplitude decay speed for all pulses. */
    private val globalDamping = 0.15f // Un facteur plus élevé (proche de 1.0f) signifie un amortissement plus lent.

    /** Minimum amplitude below which pulses are removed from the active list (CPU optimization). */
    private val minAmplitudeToRemovePulse = 0.2f // L légèrement augmenté pour nettoyer plus tôt si nécessaire

    /** Minimum amplitude threshold below which pulses do not deform pixels (GPU optimization via shader). */
    private val minAmplitudeThresholdForShader = 2f

    /** Initial amplitude for newly created pulses. Adjust based on desired visual impact. */
    private val initialAmplitude = 150f // L'amplitude du drag était souvent plus élevée, ajuster pour la musique.

    /** Initial frequency for newly created pulses. Controls how "dense" the ripples are. */
    private val initialFrequency = 5f // Plus la fréquence est élevée, plus il y a de cercles par onde.

    /** Initial speed for newly created pulses (pixels per second). */
    private val initialSpeed = 500f // Vitesse de propagation de l'onde.

    // --- État interne des pulsations ---
    /** MutableStateFlow holding the current list of active pulse waves. */
    private val _waves = MutableStateFlow<List<Wave>>(emptyList())
    val waves: StateFlow<List<Wave>> = _waves

    /** Maximum number of active waves the shader can process simultaneously.
     * Must match AGSL shader MAX_PULSES. */
    private val MAX_WAVES_IN_SHADER = 20

    // --- Paramètres pour la simulation de pulsation (sera retiré plus tard) ---
    private var simulationJob: kotlinx.coroutines.Job? = null
    private var simulationPulseIntervalSeconds = 1f // Émettre un pulse toutes les 0.5 secondes (2 pulses/sec)
    private var simulationPulseOrigin: Offset? = null // L'origine des pulses, typiquement le centre de l'image.

    /**
     * Starts a continuous simulation of pulses. This method is for debugging/demonstration
     * and will be replaced by actual music analysis later.
     *
     * @param origin The fixed origin point for all simulated pulses (e.g., center of the image).
     */
    fun startPulsationSimulation(origin: Offset) {
        if (simulationJob?.isActive == true) {
            // Simulation already running, or still active, cancel it before restarting
            simulationJob?.cancel()
            Log.d("PulseDebug", "Existing simulation cancelled.")
        }
        simulationPulseOrigin = origin
        simulationJob = viewModelScope.launch {
            val startTime = System.nanoTime()
            Log.d("PulseDebug", "Simulation job launched for origin: $origin")
            while (true) {
                val currentTimeSeconds = (System.nanoTime() - startTime) / 1_000_000_000f
                simulationPulseOrigin?.let {
                    // We can vary amplitude/frequency/speed here if needed for more dynamic simulation
                    // For now, fixed values for simplicity
                    addWave(
                        origin = it,
                        currentTimeSeconds = currentTimeSeconds,
                        // Optionally add some random variation for testing
                        amplitude = initialAmplitude + Random.nextFloat() * 10f, // Slight random amplitude variation
                        frequency = initialFrequency,
                        speed = initialSpeed
                    )
                }
                delay((simulationPulseIntervalSeconds * 1000).toLong())
            }
        }
    }

    /**
     * Stops the continuous pulsation simulation.
     */
    fun stopPulsationSimulation() {
        simulationJob?.cancel()
        simulationJob = null
    }

    /**
     * Removes waves whose amplitude after damping falls below the CPU threshold.
     * Should be called regularly with current system time from the Composable.
     *
     * @param currentTimeSeconds current system time in seconds
     */
    fun cleanupWaves(currentTimeSeconds: Float) {
        _waves.value = _waves.value.filter { wave ->
            val elapsedSeconds = currentTimeSeconds - wave.startTime
            val currentAmplitude = wave.amplitude * globalDamping.pow(elapsedSeconds)
            currentAmplitude >= minAmplitudeToRemovePulse
        }
    }

    /**
     * Adds a new pulse wave at the given origin.
     * This method is now simplified for musical pulsations, removing drag-specific logic.
     * If max waves limit is reached, removes the weakest wave first.
     *
     * @param origin origin of the pulse in screen coordinates.
     * @param currentTimeSeconds current system time in seconds when the pulse occurred.
     * @param amplitude (Optional) custom amplitude for this pulse. Defaults to initialAmplitude.
     * @param frequency (Optional) custom frequency for this pulse. Defaults to initialFrequency.
     * @param speed (Optional) custom speed for this pulse. Defaults to initialSpeed.
     */
    fun addWave(
        origin: Offset,
        currentTimeSeconds: Float,
        amplitude: Float = initialAmplitude,
        frequency: Float = initialFrequency,
        speed: Float = initialSpeed
    ) {
        val newWave = Wave(
            origin = origin,
            startTime = currentTimeSeconds,
            amplitude = amplitude,
            frequency = frequency,
            speed = speed
        )

        val currentWaves = _waves.value.toMutableList()

        // If the max waves limit is reached, remove the weakest wave before adding a new one
        if (currentWaves.size >= MAX_WAVES_IN_SHADER) {
            val weakestWave = currentWaves.minByOrNull { wave ->
                val elapsedSeconds = currentTimeSeconds - wave.startTime
                wave.amplitude * globalDamping.pow(elapsedSeconds)
            }
            if (weakestWave != null) {
                currentWaves.remove(weakestWave)
                // Log.d("PulseVM", "Removed weakest wave from time ${weakestWave.startTime}")
            }
        }

        currentWaves.add(newWave)
        _waves.value = currentWaves
        Log.d("PulseDebug", "Wave added. Total waves: ${currentWaves.size}")

         Log.d("PulseVM", "Pulse added at (${origin.x}, ${origin.y}) at time $currentTimeSeconds. Total pulses: ${currentWaves.size}")
    }

    /**
     * Prepares pulse wave parameters for shader uniforms based on active waves.
     *
     * @param currentTimeSeconds current system time in seconds
     * @return [WaveShaderParams] containing uniform data arrays and counts
     */
    fun getShaderUniforms(currentTimeSeconds: Float): WaveShaderParams {
        val activeWaves = _waves.value

        val origins = FloatArray(MAX_WAVES_IN_SHADER * 2)
        val amplitudes = FloatArray(MAX_WAVES_IN_SHADER)
        val frequencies = FloatArray(MAX_WAVES_IN_SHADER)
        val speeds = FloatArray(MAX_WAVES_IN_SHADER)
        val startTimes = FloatArray(MAX_WAVES_IN_SHADER)

        activeWaves.take(MAX_WAVES_IN_SHADER).forEachIndexed { index, wave ->
            // Note: The amplitude damping is applied in the shader, but we can also apply
            // it here for the `amplitudes` uniform if we want the shader to receive
            // an already damped value. However, the shader already applies it, so
            // it's better to pass the initial amplitude and let the shader do its job.
            // The `cleanupWaves` function uses damping to decide removal.

            origins[index * 2] = wave.origin.x
            origins[index * 2 + 1] = wave.origin.y
            amplitudes[index] = wave.amplitude // Pass initial amplitude, damping done in shader
            frequencies[index] = wave.frequency
            speeds[index] = wave.speed
            startTimes[index] = wave.startTime
        }

        return WaveShaderParams(
            numWaves = activeWaves.size.coerceAtMost(MAX_WAVES_IN_SHADER),
            origins = origins,
            amplitudes = amplitudes,
            frequencies = frequencies,
            speeds = speeds,
            startTimes = startTimes,
            globalDamping = globalDamping,
            minAmplitudeThreshold = minAmplitudeThresholdForShader
        )
    }

    override fun onCleared() {
        super.onCleared()
        stopPulsationSimulation() // Ensure simulation job is cancelled when ViewModel is cleared
    }
}


