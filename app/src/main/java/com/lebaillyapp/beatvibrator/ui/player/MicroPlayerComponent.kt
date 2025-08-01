package com.lebaillyapp.beatvibrator.ui.player

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.* // Garde cette import pour 'remember' si tu l'utilises pour des états UI locaux et non critiques
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lebaillyapp.beatvibrator.ui.theme.AlbumFont
import com.lebaillyapp.beatvibrator.ui.theme.ArtistFont
import com.lebaillyapp.beatvibrator.ui.theme.MainFont

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MicroPlayerComponent(
    modifier: Modifier = Modifier,
    songTitle: String = "Unknown Song",
    albumName: String = "Unknown Album",
    artistName: String = "Unknown Artist",
    // --- Nouveaux paramètres pour externaliser la logique ---
    isPlaying: Boolean, // L'état de lecture/pause est passé en paramètre
    currentProgress: Float, // La progression (0.0f à 1.0f) est passée en paramètre
    elapsedTimeMillis: Long, // Le temps écoulé est passé en paramètre
    totalDurationMillis: Long, // La durée totale est passée en paramètre
    onTogglePlayPause: () -> Unit, // Le callback pour l'action play/pause
    // --------------------------------------------------------
    elevation: Dp = 8.dp
) {
    // Suppression des 'remember { mutableStateOf(...) }' et 'LaunchedEffect'
    // Ils seront gérés par le composant parent ou un ViewModel plus tard.

    // Transition pour animer les changements entre play / pause
    // Reste ici car c'est une animation visuelle basée sur l'état 'isPlaying'
    val transition = updateTransition(targetState = isPlaying, label = "PlayPauseTransition")

    val timerFontSizeSp by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 400, easing = LinearEasing) },
        label = "TimerFontSize"
    ) { playing ->
        if (playing) 32f else 22f
    }

    val timerWidthDp by transition.animateDp(
        transitionSpec = { tween(durationMillis = 400, easing = LinearEasing) },
        label = "TimerWidth"
    ) { playing ->
        if (playing) 94.dp else 66.dp
    }

    val infoAlpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 400, easing = LinearEasing) },
        label = "InfoAlpha"
    ) { playing ->
        if (playing) 0.8f else 1f
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp),
        shape = RoundedCornerShape(52.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF292525),
                            Color(0x2F2E1FFF)
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Utilise le paramètre isPlaying et le callback onTogglePlayPause
                PlayPauseButton(
                    isPlaying = isPlaying,
                    onTogglePlay = onTogglePlayPause,
                    modifier = Modifier.size(60.dp)
                )

                Spacer(Modifier.width(if (isPlaying) 15.dp else 10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .animateContentSize(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Utilise elapsedTimeMillis directement
                        val minutes = (elapsedTimeMillis / 1000 / 60).toInt()
                        val seconds = (elapsedTimeMillis / 1000 % 60).toInt()
                        Text(
                            text = String.format("%02d:%02d", minutes, seconds),
                            color = Color.White,
                            fontSize = timerFontSizeSp.sp,
                            fontFamily = MainFont,
                            modifier = Modifier
                                .width(timerWidthDp)
                                .offset(y = (-10).dp)
                        )

                        Spacer(Modifier.width(0.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .graphicsLayer { alpha = infoAlpha }
                                .padding(top = 0.dp),
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
                                fontSize = 18.sp,
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

                    // Utilise currentProgress directement
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

// PlayPauseButton et CustomProgressBar restent inchangés
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
            .background(Color.White)
            .drawBehind {
                val radius = size.minDimension / 2f
                val centerX = size.width / 2f
                val centerY = size.height / 2f

                val innerShadowBrush = Brush.radialGradient(
                    0.0f to Color.Black.copy(alpha = 0.0f),
                    0.9f to Color.Black.copy(alpha = 0.10f),
                    0.98f to Color.Black.copy(alpha = 0.30f),
                    1.0f to Color.Black.copy(alpha = 0.60f),
                    center = Offset(centerX, centerY),
                    radius = radius
                )

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
    progress: Float,
    height: Dp = 4.dp,
    trackColor: Color = Color(0xFF4C4C4C),
    progressColor: Color = Color(0xFFC0C0C0),
    cornerRadius: Dp = 2.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(trackColor)
            .drawBehind {
                val progressWidth = size.width * progress
                drawRoundRect(
                    color = progressColor,
                    size = Size(width = progressWidth, height = size.height),
                    cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
                )
            }
    )
}