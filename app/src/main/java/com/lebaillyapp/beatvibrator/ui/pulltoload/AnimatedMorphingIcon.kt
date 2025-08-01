package com.lebaillyapp.beatvibrator.ui.pulltoload

import android.graphics.drawable.AnimatedVectorDrawable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.graphics.drawable.toBitmap
import com.lebaillyapp.beatvibrator.R

// Ce composable prend en charge l'animation de votre icône
// Il prend en paramètre la progression de votre glissement.
@Composable
fun AnimatedMorphingIcon(
    progress: Float,
    modifier: Modifier = Modifier,
    // ID de votre AnimatedVectorDrawable, créé à partir des fichiers XML
    animatedIconResId: Int = R.drawable.animated_morphing_icon
) {
    val context = LocalContext.current
    val animatableProgress = remember { Animatable(0f) }

    // On utilise un LaunchedEffect pour animer le progrès du drawable
    // La valeur de 'progress' de votre glissement est la source de vérité
    LaunchedEffect(progress) {
        animatableProgress.animateTo(
            targetValue = progress,
            animationSpec = tween(durationMillis = 1)
        )
    }

    // Créer une instance de l'AnimatedVectorDrawable
    val animatedVectorDrawable = remember {
        context.resources.getDrawable(animatedIconResId, context.theme) as AnimatedVectorDrawable
    }

    // Utiliser drawIntoCanvas pour dessiner l'AnimatedVectorDrawable sur le canvas Compose
    Image(
        painter = remember {
            object : androidx.compose.ui.graphics.painter.Painter() {
                override val intrinsicSize: androidx.compose.ui.geometry.Size
                    get() = androidx.compose.ui.geometry.Size(
                        animatedVectorDrawable.intrinsicWidth.toFloat(),
                        animatedVectorDrawable.intrinsicHeight.toFloat()
                    )
                override fun DrawScope.onDraw() {
                    drawIntoCanvas { canvas ->
                        animatedVectorDrawable.setBounds(
                            0,
                            0,
                            intrinsicSize.width.toInt(),
                            intrinsicSize.height.toInt()
                        )
                        // Définir la progression de l'animation
                        animatedVectorDrawable.jumpToCurrentState()
                        animatedVectorDrawable.level = (animatableProgress.value * 10000).toInt()
                        animatedVectorDrawable.draw(canvas.nativeCanvas)
                    }
                }
            }
        },
        contentDescription = "Animated Morphing Icon",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun AnimatedMorphingIconPreview() {
    // Dans la preview, vous pouvez simuler la progression
    // Par exemple, en faisant varier la valeur de `progress`
    AnimatedMorphingIcon(progress = 0.5f)
}

