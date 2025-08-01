package com.lebaillyapp.beatvibrator

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.lebaillyapp.beatvibrator.ui.player.MicroPlayerComponent
import com.lebaillyapp.beatvibrator.ui.pullToLoad.PullToLoadScreen
import com.lebaillyapp.beatvibrator.ui.theme.BeatVibratorTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Point d'entrée principal de l'application. Cette activité gère la configuration de l'interface
 * utilisateur, notamment la couleur de la barre système, l'arrière-plan dynamique en fonction
 * du glissement, et l'intégration des différents composants de l'application.
 *
 * Elle utilise [dagger.hilt.android.AndroidEntryPoint] pour l'injection de dépendances.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BeatVibratorTheme {
                val desiredBarColor = Color(0xFFBABABA)
                SetSystemBarsColor(
                    statusBarColor = desiredBarColor,
                    navigationBarColor = desiredBarColor,
                    useLightStatusBarIcons = true,
                    useLightNavigationBarIcons = true
                )

                // Les couleurs de début et de fin pour l'interpolation du dégradé de fond.
                val startColor = Color(0xFFC6FF00)
                val endColor = Color(0xFF1DE9B6)

                // L'état qui stocke le décalage (offset) de l'action de glissement.
                var pullOffset by remember { mutableFloatStateOf(0f) }

                // Définit le seuil de glissement pour calculer la progression.
                val pullThreshold = 650f
                // Calcule la progression (0.0f à 1.0f) en fonction de l'offset de glissement.
                val progress = (pullOffset / pullThreshold).coerceIn(0f, 1f)

                // Interpole la couleur de fond en fonction de la progression.
                val backgroundColor = lerp(startColor, endColor, progress)

                // Utilise un Box pour empiler les éléments de l'écran.
                Box(
                    modifier = Modifier.fillMaxSize()
                        .background(backgroundColor)
                ) {
                    // Le contenu principal de l'écran, enveloppé dans un composant de glissement.
                    PullToLoadScreen(
                        // Callback pour mettre à jour l'état de l'offset en fonction du glissement.
                        onOffsetChanged = { offset -> pullOffset = offset },
                        // Callback pour déclencher une action lorsque le glissement est terminé.
                        onActionTriggered = { /* L'action de chargement sera implémentée ici */ }
                    ) {
                        Card(
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFBABABA)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Scaffold(
                                modifier = Modifier.fillMaxSize(),
                                containerColor = Color.Transparent
                            ) { innerPadding ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    // Le contenu de la colonne est vide dans cet exemple
                                }
                            }
                        }
                    }

                    // Le lecteur de musique est positionné en bas et reste fixe pendant le glissement.
                    PlayerControls(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp))
                }
            }
        }
    }
}

/**
 * Gère l'état et l'affichage du composant de lecteur de musique.
 * Simule la lecture et l'avancement d'une chanson.
 *
 * @param modifier Le [Modifier] à appliquer au composant.
 */
@Composable
fun PlayerControls(modifier: Modifier = Modifier) {
    var isPlaying by remember { mutableStateOf(false) }
    var currentProgress by remember { mutableFloatStateOf(0.0f) }
    var elapsedTimeMillis by remember { mutableLongStateOf(0L) }
    val totalDurationMillis = 150000L

    // Simule la progression de la lecture de la musique
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (elapsedTimeMillis < totalDurationMillis) {
                kotlinx.coroutines.delay(1000L)
                elapsedTimeMillis += 1000L
                currentProgress = (elapsedTimeMillis.toFloat() / totalDurationMillis).coerceAtMost(1f)
            }
            // Réinitialise l'état une fois la chanson terminée
            isPlaying = false
            elapsedTimeMillis = 0L
            currentProgress = 0f
        }
    }

    MicroPlayerComponent(
        modifier = modifier.padding(26.dp),
        songTitle = "Beautiful Things",
        albumName = "FireWork and Rollerblades",
        artistName = "Benson Boone",
        isPlaying = isPlaying,
        currentProgress = currentProgress,
        elapsedTimeMillis = elapsedTimeMillis,
        totalDurationMillis = totalDurationMillis,
        onTogglePlayPause = { isPlaying = !isPlaying },
        elevation = 8.dp
    )
}

/**
 * Un composable qui définit la couleur et le style des barres système.
 *
 * @param statusBarColor La couleur à utiliser pour la barre d'état.
 * @param navigationBarColor La couleur à utiliser pour la barre de navigation.
 * @param useLightStatusBarIcons Si `true`, les icônes de la barre d'état seront claires (pour un fond sombre).
 * @param useLightNavigationBarIcons Si `true`, les icônes de la barre de navigation seront claires (pour un fond sombre).
 */
@Composable
fun SetSystemBarsColor(
    statusBarColor: Color,
    navigationBarColor: Color,
    useLightStatusBarIcons: Boolean = false,
    useLightNavigationBarIcons: Boolean = false
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)

            window.statusBarColor = statusBarColor.toArgb()
            window.navigationBarColor = navigationBarColor.toArgb()

            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = useLightStatusBarIcons
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = useLightNavigationBarIcons
        }
    }
}

