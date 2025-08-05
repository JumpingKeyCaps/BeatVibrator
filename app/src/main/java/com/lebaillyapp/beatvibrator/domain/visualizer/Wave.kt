package com.lebaillyapp.beatvibrator.domain.visualizer

import androidx.compose.ui.geometry.Offset

/**
 * Represents a single pulse wave in the visualization.
 * It contains all parameters needed by the AGSL shader to render the wave.
 */
data class Wave(
    val origin: Offset,        // The (x,y) screen coordinate where the pulse originated.
    val startTime: Float,      // The time in seconds when this pulse was created.
    val amplitude: Float,      // The initial strength/intensity of the pulse.
    val frequency: Float,      // How many cycles (peaks/troughs) per unit distance within the wave.
    val speed: Float           // How fast the pulse propagates outwards (pixels per second).
)