package com.lebaillyapp.beatvibrator.ui.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.* // Utilisons Material 3
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lebaillyapp.beatvibrator.ui.theme.AlbumFont
import com.lebaillyapp.beatvibrator.ui.theme.ArtistFont
import com.lebaillyapp.beatvibrator.ui.theme.MainFont

// IMPORTANT: Ajoute ExperimentalFoundationApi ici pour basicMarquee
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MicroPlayerComponent(
    modifier: Modifier = Modifier,
    songTitle: String = "Unknown Song",
    albumName: String = "Unknown Album",
    artistName: String = "Unknown Artist",
    initialProgress: Float = 0.0f, // 0.0 to 1.0
    totalDurationMillis: Long = 150000L,
    elevation: Dp = 8.dp
) {
    var isPlaying by remember { mutableStateOf(false) }
    var currentProgress by remember { mutableFloatStateOf(initialProgress) }
    var elapsedTimeMillis by remember { mutableLongStateOf((initialProgress * totalDurationMillis).toLong()) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (elapsedTimeMillis < totalDurationMillis) {
                kotlinx.coroutines.delay(1000L) // Update every second
                elapsedTimeMillis += 1000L
                currentProgress = elapsedTimeMillis.toFloat() / totalDurationMillis
                if (currentProgress > 1f) currentProgress = 1f // Cap at 100%
            }
            isPlaying = false // Song finished
            elapsedTimeMillis = 0L // Reset for next play
            currentProgress = 0f
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp), // Hauteur ajustée
        shape = RoundedCornerShape(52.dp), // Pour la forme "tictac"
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize() // Le Box doit prendre toute la taille de la Card
                .background( // Applique le dégradé sur ce Box
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF292525), // Couleur de début du dégradé
                            Color(0x46342E38)  // Couleur de fin du dégradé
                        ),
                        start = Offset(0f, 0f), // Démarre en haut
                        end = Offset(Float.POSITIVE_INFINITY,0f) // Finit en bas
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play/Pause Button
                PlayPauseButton(
                    isPlaying = isPlaying,
                    onTogglePlay = { isPlaying = !isPlaying },
                    modifier = Modifier.size(60.dp)
                )

                val frontSpacer = if(isPlaying) 15.dp else 10.dp
                Spacer(Modifier.width(frontSpacer))

                // Nouvelle COLONNE pour regrouper le timer, les infos du morceau ET la barre de progression
                Column(
                    modifier = Modifier
                        .weight(1f) // Cette colonne prend l'espace restant
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center // Centre verticalement le contenu de cette colonne
                ) {
                    Spacer(modifier = Modifier.height(10.dp))

                    // Conteneur pour le timer et les infos (qui étaient dans une Row ensemble)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth() // La Row prend toute la largeur disponible dans la Column parente
                            .padding(end = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Timer
                        val minutes = (elapsedTimeMillis / 1000 / 60).toInt()
                        val seconds = (elapsedTimeMillis / 1000 % 60).toInt()
                        val timerFontSize = if (isPlaying) 32.sp else 22.sp
                        val timerFontSpace = if (isPlaying) 94.dp else 66.dp
                        Text(
                            text = String.format("%02d:%02d", minutes, seconds),
                            color = Color.White,
                            fontSize = timerFontSize,
                            fontFamily = MainFont,
                            modifier = Modifier.align(Alignment.CenterVertically).offset(y = (-10).dp)
                                .width(timerFontSpace)
                        )

                        Spacer(Modifier.width(0.dp))

                        // Song Info (Title, Album, Artist) - MAINTENANT C'EST UNE COLONNE SÉPARÉE À CÔTÉ DU TIMER
                        Column(
                            modifier = Modifier
                                .weight(1f) // Prend l'espace restant dans cette Row
                                .padding(top = 0.dp) // Padding à gauche
                                .align(Alignment.CenterVertically), // Centre verticalement par rapport au timer
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = songTitle,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontFamily = MainFont,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.basicMarquee()
                            )
                            Text(
                                text = albumName,
                                color = Color(0xFFC0C0C0),
                                fontSize = 22.sp,
                                fontFamily = AlbumFont,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.offset(y = (-6).dp)
                            )
                            Text(
                                text = artistName,
                                color = Color(0xFFC0C0C0),
                                fontSize = 12.sp,
                                fontFamily = ArtistFont,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.offset(y = (-14).dp)
                            )
                        }
                    }

                    // La barre de progression vient DIRECTEMENT SOUS LA ROW du timer/infos
                    CustomProgressBar(
                        progress = currentProgress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 20.dp)
                            .offset(y = (-15).dp),
                        height = 2.dp,
                        trackColor = Color(0x7E474656),
                        progressColor = Color(0xFF706B6B),
                        cornerRadius = 1.5.dp
                    )
                }
            }
        }
    }
}


/**
 * ## Composant bouton play/pause.
 * @param isPlaying Si le player est en train de jouer.
 * @param onTogglePlay Fonction à appeler lorsque le bouton est cliqué.
 * @param modifier Modifier pour personnaliser l'apparence.
 */
@Composable
fun PlayPauseButton(
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(65.dp)
            .clip(CircleShape)
            .background(Color.White) // Le fond blanc du bouton
            .drawBehind {
                val radius = size.minDimension / 2f
                val centerX = size.width / 2f
                val centerY = size.height / 2f

                // Crée le Brush radialGradient AVANT de l'utiliser dans drawCircle
                val innerShadowBrush = Brush.radialGradient(
                    // Utilise la signature avec vararg Pair<Float, Color> pour les stops précis
                    0.0f to Color.Black.copy(alpha = 0.0f),    // Transparent au centre
                    0.9f to Color.Black.copy(alpha = 0.10f),   // Très légère opacité un peu après le centre
                    0.98f to Color.Black.copy(alpha = 0.30f),   // Opacité plus marquée près du bord
                    1.0f to Color.Black.copy(alpha = 0.60f),   // L'opacité la plus forte juste au bord
                    center = Offset(centerX, centerY), // Spécifie le centre du dégradé
                    radius = radius // Spécifie le rayon du dégradé
                )

                // Dessine un cercle en utilisant ce Brush.
                // Le cercle sera dessiné exactement aux dimensions et position spécifiées.
                drawCircle(
                    brush = innerShadowBrush,
                    radius = radius,
                    center = Offset(centerX, centerY)
                )
            },
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onTogglePlay,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color(0xFF3F3E49),
                modifier = Modifier.size(30.dp)
            )
        }
    }
}


@Composable
fun CustomProgressBar(
    modifier: Modifier = Modifier,
    progress: Float, // La valeur de progression (0.0f à 1.0f)
    height: Dp = 4.dp, // Hauteur de la barre de progression
    trackColor: Color = Color(0xFF4C4C4C), // Couleur du "vide" de la barre (monochrome, gris foncé)
    progressColor: Color = Color(0xFFC0C0C0), // Couleur de la progression actuelle (gris clair)
    cornerRadius: Dp = 2.dp // Rayon des coins arrondis
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(cornerRadius)) // Coins arrondis pour toute la barre
            .background(trackColor) // Couleur de fond de la barre
            .drawBehind { // Dessine la progression par-dessus
                val progressWidth = size.width * progress
                drawRoundRect(
                    color = progressColor,
                    size = Size(width = progressWidth, height = size.height),
                    cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
                )
            }
    )
}


// PREVIEW ########################################
@Preview(showBackground = true)
@Composable
fun PreviewMusicPlayerTicTac() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.DarkGray) // Un fond pour bien voir la carte
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        MicroPlayerComponent()
    }
}