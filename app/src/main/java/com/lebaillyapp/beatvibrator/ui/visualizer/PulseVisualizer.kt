package com.lebaillyapp.beatvibrator.ui.visualizer

import androidx.annotation.RawRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.lifecycle.viewmodel.compose.viewModel
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.toSize

/**
 * A Jetpack Compose Composable that applies a dynamic AGSL shader effect
 * to an [ImageBitmap] to visualize musical pulsations.
 *
 * The shader creates ripple-like waves emanating from the center, driven by
 * simulated pulsations from the [PulseVisualizerViewModel].
 *
 * @param modifier The modifier to be applied to this Composable.
 * @param bitmap The [ImageBitmap] to which the pulse visualization shader will be applied.
 * @param pulseVisualizerViewModel The ViewModel managing the pulse data. Defaults to a new instance.
 * @param shaderResId The raw resource ID of the AGSL shader file (e.g., R.raw.pulse_visualizer).
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun PulseVisualizer(
    modifier: Modifier = Modifier,
    bitmap: ImageBitmap,
    pulseVisualizerViewModel: PulseVisualizerViewModel = viewModel(),
    @RawRes shaderResId: Int,
    shape: Shape = RectangleShape
) {
    val context = LocalContext.current

    // Load shader code once.
    val shaderCode = remember {
        context.resources.openRawResource(shaderResId).bufferedReader().use { it.readText() }
    }
    val shader = remember { RuntimeShader(shaderCode) }

    // Track animation time in seconds, consistent with shader's 'uTime'.
    var currentTimeSeconds by remember { mutableFloatStateOf(0f) }

    // State to hold the size of the composable.
    var composableSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

    // LaunchedEffect to drive the animation time and cleanup waves.
    LaunchedEffect(Unit) {
        val startTime = System.nanoTime()
        while (true) {
            withFrameNanos { frameTime ->
                currentTimeSeconds = (frameTime - startTime) / 1_000_000_000f
            }
            pulseVisualizerViewModel.cleanupWaves(currentTimeSeconds)
            // Ajouter un delay de 16ms (environ 60 FPS)
            kotlinx.coroutines.delay(16)
        }
    }

    // Start pulsation simulation once the composable has a size.
    LaunchedEffect(composableSize) {
        if (composableSize.width > 0f && composableSize.height > 0f) { // Utiliser 0f pour comparer des Floats
            val centerX = composableSize.width / 2f
            val centerY = composableSize.height / 2f
            pulseVisualizerViewModel.startPulsationSimulation(Offset(centerX, centerY))
        } else {
            pulseVisualizerViewModel.stopPulsationSimulation()
        }
    }

    // Get current shader uniforms from ViewModel.
    val waves by pulseVisualizerViewModel.waves.collectAsState()

    // currentShaderParams sera recalculé à chaque recomposition
    val currentShaderParams = pulseVisualizerViewModel.getShaderUniforms(currentTimeSeconds)

    // Set uniforms using SideEffect.
    SideEffect {
        if (composableSize.width > 0f && composableSize.height > 0f) {
            shader.setFloatUniform("uResolution", composableSize.width, composableSize.height)
            shader.setFloatUniform("uTime", currentTimeSeconds)
            shader.setFloatUniform("uGlobalDamping", currentShaderParams.globalDamping)
            shader.setFloatUniform("uMinAmplitudeThreshold", currentShaderParams.minAmplitudeThreshold)
            shader.setIntUniform("uNumWaves", currentShaderParams.numWaves)

            shader.setFloatUniform("uWaveOrigins", currentShaderParams.origins)
            shader.setFloatUniform("uWaveAmplitudes", currentShaderParams.amplitudes)
            shader.setFloatUniform("uWaveFrequencies", currentShaderParams.frequencies)
            shader.setFloatUniform("uWaveSpeeds", currentShaderParams.speeds)
            shader.setFloatUniform("uWaveStartTimes", currentShaderParams.startTimes)
        }
    }

    // force la recréation du RenderEffect à chaque recomposition,
    // ce qui force le redessin et la prise en compte des uniforms mis à jour.
    val renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "inputShader").asComposeRenderEffect()

    Image(
        painter = androidx.compose.ui.graphics.painter.BitmapPainter(bitmap),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { newSize ->
                composableSize = newSize.toSize() // Utiliser .toSize() pour correspondre à Size.Zero
            }
            // TRES IMPORTANT !!!!!  clip = true  pour eviter bug sur couche de rendu si composable dans un scaffold ou card parent !
            // Et passer la shape custom ici, et non via un .clip()
            .graphicsLayer(renderEffect = renderEffect, clip = true, shape = shape)

    )
}





