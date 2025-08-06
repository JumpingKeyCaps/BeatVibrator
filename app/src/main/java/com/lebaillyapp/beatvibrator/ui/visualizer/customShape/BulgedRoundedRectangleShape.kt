package com.lebaillyapp.beatvibrator.ui.visualizer.customShape

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp // Importez Dp
import androidx.compose.ui.unit.dp // Importez dp pour les valeurs par défaut

/**
 * Une forme rectangulaire avec des coins arrondis et des côtés légèrement bombés (incurvés vers l'extérieur).
 *
 * @param cornerRadius Le rayon des coins arrondis en Dp.
 * @param bulgeAmount L'intensité du bombement des côtés. Une valeur de 0f signifie des côtés droits.
 * Une valeur positive crée un bombement vers l'extérieur.
 */
class BulgedRoundedRectangleShape(
    private val cornerRadius: Dp, // Changé en Dp
    private val bulgeAmount: Float = 0.05f // 5% de la dimension la plus petite comme valeur par défaut
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        return Outline.Generic(Path().apply {
            val width = size.width
            val height = size.height

            // Convertir le rayon des coins en pixels
            val cornerRadiusPx = with(density) { cornerRadius.toPx() } // Utilisation correcte de toPx()

            // Assurer que le rayon ne dépasse pas la moitié de la plus petite dimension
            val actualCornerRadius = cornerRadiusPx.coerceAtMost(minOf(width, height) / 2f)

            // Calculer l'amplitude du bombement en fonction de la taille
            val actualBulgeAmount = minOf(width, height) * bulgeAmount

            // Points de départ/fin pour les arcs et les courbes
            val left = 0f
            val top = 0f
            val right = width
            val bottom = height

            // --- Dessin du chemin ---

            // Commencer au début du segment supérieur droit (avant le coin)
            moveTo(right - actualCornerRadius, top)

            // 1. Coin supérieur droit
            arcTo(
                rect = Rect(right - 2 * actualCornerRadius, top, right, top + 2 * actualCornerRadius),
                startAngleDegrees = 270f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            // 2. Côté droit (bombé)
            // Le segment va de (right, top + actualCornerRadius) à (right, bottom - actualCornerRadius)
            cubicTo(
                x1 = right + actualBulgeAmount, y1 = top + actualCornerRadius + (bottom - 2 * actualCornerRadius - (top + actualCornerRadius)) * 0.33f,
                x2 = right + actualBulgeAmount, y2 = top + actualCornerRadius + (bottom - 2 * actualCornerRadius - (top + actualCornerRadius)) * 0.66f,
                x3 = right, y3 = bottom - actualCornerRadius
            )

            // 3. Coin inférieur droit
            arcTo(
                rect = Rect(right - 2 * actualCornerRadius, bottom - 2 * actualCornerRadius, right, bottom),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            // 4. Côté inférieur (bombé)
            // Le segment va de (right - actualCornerRadius, bottom) à (left + actualCornerRadius, bottom)
            cubicTo(
                x1 = right - actualCornerRadius - (right - 2 * actualCornerRadius - (left + actualCornerRadius)) * 0.33f, y1 = bottom + actualBulgeAmount,
                x2 = right - actualCornerRadius - (right - 2 * actualCornerRadius - (left + actualCornerRadius)) * 0.66f, y2 = bottom + actualBulgeAmount,
                x3 = left + actualCornerRadius, y3 = bottom
            )

            // 5. Coin inférieur gauche
            arcTo(
                rect = Rect(left, bottom - 2 * actualCornerRadius, left + 2 * actualCornerRadius, bottom),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            // 6. Côté gauche (bombé)
            // Le segment va de (left, bottom - actualCornerRadius) à (left, top + actualCornerRadius)
            cubicTo(
                x1 = left - actualBulgeAmount, y1 = bottom - actualCornerRadius - (bottom - 2 * actualCornerRadius - (top + actualCornerRadius)) * 0.33f,
                x2 = left - actualBulgeAmount, y2 = bottom - actualCornerRadius - (bottom - 2 * actualCornerRadius - (top + actualCornerRadius)) * 0.66f,
                x3 = left, y3 = top + actualCornerRadius
            )

            // 7. Coin supérieur gauche
            arcTo(
                rect = Rect(left, top, left + 2 * actualCornerRadius, top + 2 * actualCornerRadius),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            // 8. Côté supérieur (bombé)
            // Le segment va de (left + actualCornerRadius, top) à (right - actualCornerRadius, top)
            cubicTo(
                x1 = left + actualCornerRadius + (right - 2 * actualCornerRadius - (left + actualCornerRadius)) * 0.33f, y1 = top - actualBulgeAmount,
                x2 = left + actualCornerRadius + (right - 2 * actualCornerRadius - (left + actualCornerRadius)) * 0.66f, y2 = top - actualBulgeAmount,
                x3 = right - actualCornerRadius, y3 = top
            )

            close() // Ferme le chemin
        })
    }
}