import android.graphics.drawable.AnimatedVectorDrawable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.lebaillyapp.beatvibrator.R

/**
 * Un composable qui affiche un [AnimatedVectorDrawable] et anime sa progression
 * en fonction d'une valeur de `progress` fournie.
 *
 * Cette fonction est utile pour lier une animation vectorielle à un état d'interface,
 * comme le glissement d'un doigt ou un défilement.
 *
 * @param progress La progression de l'animation, une valeur flottante entre 0.0f (début)
 * et 1.0f (fin).
 * @param modifier Le [Modifier] à appliquer à ce composable.
 * @param animatedIconResId L'ID de ressource de l'[AnimatedVectorDrawable] à afficher.
 * Par défaut, il utilise R.drawable.animated_morphing_icon.
 */
@Composable
fun AnimatedMorphingIcon(
    progress: Float,
    modifier: Modifier = Modifier,
    animatedIconResId: Int = R.drawable.animated_morphing_icon
) {
    val context = LocalContext.current
    val animatableProgress = remember { Animatable(0f) }

    // Utilise un LaunchedEffect pour animer la progression du drawable
    // La valeur de 'progress' est utilisée comme cible d'animation.
    LaunchedEffect(progress) {
        animatableProgress.animateTo(
            targetValue = progress,
            animationSpec = tween(durationMillis = 1)
        )
    }

    // Crée une instance de l'AnimatedVectorDrawable en utilisant l'ID de ressource.
    val animatedVectorDrawable = remember {
        context.resources.getDrawable(animatedIconResId, context.theme) as AnimatedVectorDrawable
    }

    // Affiche l'icône en utilisant un Painter personnalisé.
    Image(
        painter = remember {
            object : androidx.compose.ui.graphics.painter.Painter() {
                // Définit la taille intrinsèque du drawable.
                override val intrinsicSize: androidx.compose.ui.geometry.Size
                    get() = androidx.compose.ui.geometry.Size(
                        animatedVectorDrawable.intrinsicWidth.toFloat(),
                        animatedVectorDrawable.intrinsicHeight.toFloat()
                    )

                // Dessine le drawable sur le canvas Compose.
                override fun DrawScope.onDraw() {
                    drawIntoCanvas { canvas ->
                        // Définit les limites du drawable pour qu'il remplisse l'espace.
                        animatedVectorDrawable.setBounds(
                            0,
                            0,
                            intrinsicSize.width.toInt(),
                            intrinsicSize.height.toInt()
                        )
                        // Met à jour la progression de l'animation.
                        // La propriété `level` d'un drawable est un entier entre 0 et 10000.
                        animatedVectorDrawable.jumpToCurrentState()
                        animatedVectorDrawable.level = (animatableProgress.value * 10000).toInt()
                        // Dessine le drawable sur le canvas natif d'Android.
                        animatedVectorDrawable.draw(canvas.nativeCanvas)
                    }
                }
            }
        },
        contentDescription = "Animated Morphing Icon",
        modifier = modifier
    )
}

/**
 * Une fonction de prévisualisation pour le composable [AnimatedMorphingIcon].
 *
 * Elle simule l'état de l'icône à mi-chemin de la transition.
 */
@Preview(showBackground = true)
@Composable
fun AnimatedMorphingIconPreview() {
    AnimatedMorphingIcon(progress = 0.5f)
}