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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Un conteneur composable qui permet de glisser l'écran vers le bas pour déclencher une action,
 * comme un "pull to refresh".
 *
 * @param modifier Le modificateur pour le conteneur.
 * @param pullThreshold La distance de glissement nécessaire pour valider l'action.
 * @param dampingFactor Un facteur d'atténuation pour l'effet élastique. Plus la valeur est élevée (vers 1.0f), plus c'est rigide.
 * @param onActionTriggered Le callback à exécuter lorsque le glissement est validé.
 * @param onOffsetChanged Le callback à exécuter à chaque fois que la position de glissement change.
 * @param content Le contenu de l'écran à rendre glissable.
 * @param stiffness La raideur du ressort. Une valeur plus élevée rend le retour plus rapide.
 * @param dampingRatio Le ratio d'amortissement. Une valeur plus élevée réduit l'effet de rebond.
 */
@Composable
fun PullToLoadScreen(
    modifier: Modifier = Modifier,
    pullThreshold: Float = 350f, // Seuil de validation en pixels (environ 150dp)
    dampingFactor: Float = 0.05f,
    onActionTriggered: () -> Unit,
    onOffsetChanged: (Float) -> Unit,
    stiffness: Float = 300f, // Par défaut, une vitesse modérée
    dampingRatio: Float = 2.0f, // Par défaut, un léger rebond
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    // Animatable pour gérer l'offset vertical
    val offsetY = remember { Animatable(0f) }
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
                                // Seul le glissement vers le bas est autorisé (offset positif).
                                val newOffset = (offsetY.value + dragAmount.y).coerceAtLeast(0f)
                                // Effet élastique : le glissement ralentit à mesure que le seuil est approché
                                val dampedOffset = if (newOffset > 0) {
                                    newOffset * (1.0f - dampingFactor * (1.0f - (1.0f / (1.0f + newOffset / pullThreshold))))
                                } else {
                                    0f
                                }
                                offsetY.snapTo(dampedOffset)

                                // NOUVEAU: On informe le parent de la nouvelle valeur d'offset
                                onOffsetChanged(offsetY.value)
                            }
                        },
                        onDragEnd = {
                            coroutineScope.launch {
                                // Vérifie si l'offset a atteint le seuil
                                if (offsetY.value >= pullThreshold) {
                                    onActionTriggered()
                                    Toast.makeText(context, "Action déclenchée !", Toast.LENGTH_SHORT).show()
                                }
                                // Anime le retour de l'écran en position initiale (0)
                                offsetY.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(stiffness = stiffness, dampingRatio = dampingRatio)
                                ) {
                                    // NOUVEAU: On informe le parent que l'animation est terminée
                                    onOffsetChanged(offsetY.value)
                                }
                            }
                        }
                    )
                }
                    .graphicsLayer {
                        // Application de l'offset vertical
                        translationY = offsetY.value
                    }
            }
    ) {
        content()
    }
}
