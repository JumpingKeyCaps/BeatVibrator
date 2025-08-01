package com.lebaillyapp.beatvibrator.ui.audioImport

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Un conteneur composable qui permet de glisser l'écran pour déclencher une action,
 * en créant un effet de rotation autour du coin inférieur droit.
 *
 * @param modifier Le modificateur pour le conteneur.
 * @param rotationThreshold L'angle de rotation (en degrés) nécessaire pour valider l'action.
 * @param dampingFactor Un facteur d'atténuation pour l'effet élastique. Plus la valeur est élevée (vers 1.0f), plus c'est rigide.
 * @param onActionTriggered Le callback à exécuter lorsque le glissement est validé.
 * @param content Le contenu de l'écran à rendre glissable.
 */
@Composable
fun DragToSwipeScreen(
    modifier: Modifier = Modifier,
    rotationThreshold: Float = -20f, // Seuil de validation en degrés
    dampingFactor: Float = 0.2f,
    onActionTriggered: () -> Unit,
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    // Animatable pour gérer l'angle de rotation
    // La variable a été renommée pour éviter la confusion avec le paramètre de graphicsLayer
    val rotationAnimatable = remember { Animatable(0f) }
    val context = LocalContext.current

    Box(
        modifier = modifier
            .composed {
                // Le modificateur pour l'écoute des gestes de glissement
                pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            coroutineScope.launch {
                                // Conversion du glissement en rotation.
                                // Un drag horizontal de 1px est converti en une petite rotation.
                                // Le glissement vers la gauche doit donner une rotation négative.
                                val newRotation = (rotationAnimatable.value + dragAmount.x * 0.1f).coerceAtMost(0f)
                                // Effet élastique : la rotation ralentit à mesure que le seuil est approché
                                val dampedRotation = if (newRotation < 0) {
                                    newRotation * (1.0f - dampingFactor * (1.0f - (1.0f / (1.0f + newRotation / rotationThreshold))))
                                } else {
                                    0f
                                }
                                rotationAnimatable.snapTo(dampedRotation)
                            }
                        },
                        onDragEnd = {
                            coroutineScope.launch {
                                // Vérifie si la rotation a atteint le seuil
                                if (rotationAnimatable.value <= rotationThreshold) {
                                    onActionTriggered()
                                    Toast.makeText(context, "Action déclenchée !", Toast.LENGTH_SHORT).show()
                                }
                                // Anime le retour de l'écran en position initiale (0°)
                                rotationAnimatable.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring()
                                )
                            }
                        }
                    )
                }
                    .graphicsLayer {
                        // Application de la rotation.
                        // On définit le pivot au coin inférieur droit (x=1.0, y=1.0)
                        transformOrigin = TransformOrigin(1.0f, 1.0f)
                        // On utilise rotationAnimatable.value pour obtenir la valeur Float de l'Animatable
                        rotationZ = rotationAnimatable.value
                    }
            }
    ) {
        content()
    }
}