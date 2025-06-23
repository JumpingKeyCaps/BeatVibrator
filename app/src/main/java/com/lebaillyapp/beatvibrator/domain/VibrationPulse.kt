package com.lebaillyapp.beatvibrator.domain

/**
 * Represents a single haptic vibration event.
 *
 * @param timeMs Start time of the vibration, in milliseconds relative to the start of the track.
 * @param intensity Intensity of the vibration [0..1].
 * @param durationMs Duration of the vibration in milliseconds.
 */
data class VibrationPulse(
    val timeMs: Long,
    val intensity: Float,
    val durationMs: Long
)