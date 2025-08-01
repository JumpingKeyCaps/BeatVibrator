package com.lebaillyapp.beatvibrator

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.lebaillyapp.beatvibrator.ui.player.MicroPlayerComponent
import com.lebaillyapp.beatvibrator.ui.theme.BeatVibratorTheme
import dagger.hilt.android.AndroidEntryPoint

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

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Appelle le composant parent qui gère l'état
                    PlayerScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

/**
 * Composant parent temporaire qui contient la logique d'état et de temps.
 * Il alimente le MicroPlayerComponent avec ses données.
 */
@Composable
fun PlayerScreen(modifier: Modifier = Modifier) {
    // --- États et logique temporaire pour le test à l'arrache ---
    var isPlaying by remember { mutableStateOf(false) }
    var currentProgress by remember { mutableFloatStateOf(0.0f) }
    var elapsedTimeMillis by remember { mutableLongStateOf(0L) }
    val totalDurationMillis = 150000L // 2 minutes 30 secondes

    // Logique du timer, relancée à chaque changement de 'isPlaying'
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (elapsedTimeMillis < totalDurationMillis) {
                // Attendre 1 seconde avant de mettre à jour
                kotlinx.coroutines.delay(1000L)
                elapsedTimeMillis += 1000L
                currentProgress = (elapsedTimeMillis.toFloat() / totalDurationMillis).coerceAtMost(1f)
            }
            // Arrêter et réinitialiser la lecture une fois le morceau terminé
            isPlaying = false
            elapsedTimeMillis = 0L
            currentProgress = 0f
        }
    }
    // -------------------------------------------------------------

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFBBBBBB)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        // Le MicroPlayerComponent reçoit maintenant tous ses états et son callback
        MicroPlayerComponent(
            modifier = Modifier.padding(36.dp),
            songTitle = "Beautiful Things",
            albumName = "FireWork and Rollerblades",
            artistName = "Benson Boone",
            isPlaying = isPlaying, // Passe l'état actuel
            currentProgress = currentProgress, // Passe la progression actuelle
            elapsedTimeMillis = elapsedTimeMillis, // Passe le temps écoulé
            totalDurationMillis = totalDurationMillis, // Passe la durée totale
            onTogglePlayPause = { isPlaying = !isPlaying }, // Passe l'action de mise à jour
            elevation = 8.dp
        )
    }
}

/**
 * Définit la couleur de la barre de statut et de navigation du système.
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

@Preview(showBackground = true)
@Composable
fun MainPreview() {
    BeatVibratorTheme {
        // Affiche le PlayerScreen pour la prévisualisation
        PlayerScreen()
    }
}
