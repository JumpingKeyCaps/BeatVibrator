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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.lebaillyapp.beatvibrator.ui.audioImport.DragToSwipeScreen
import com.lebaillyapp.beatvibrator.ui.audioImport.PullToLoadScreen
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

                // Utilise un Box pour empiler les éléments
                Box(modifier = Modifier.fillMaxSize()
                    .background(Color(0xFFC6FF00))) {
                    // On enveloppe le contenu principal de l'écran dans notre nouveau conteneur de glissement.
                    // C'est cette partie qui va bouger.
                    PullToLoadScreen(
                        onActionTriggered = {
                            // Ici,  appeler la fonction SAF plus tard
                            // Pour le moment, l'action est gérée par le Toast dans le composant
                        }
                    ) {
                        Card(
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
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

                                }
                            }
                        }
                    }

                    // Le MicroPlayerComponent est positionné en bas,
                    // en dehors de DragToSwipeScreen, donc il reste fixe.
                    PlayerControls(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp))
                }
            }
        }
    }
}

/**
 * Ce composable gère l'état du lecteur et affiche le MicroPlayerComponent.
 * Il est maintenant séparé de la logique de glissement.
 */
@Composable
fun PlayerControls(modifier: Modifier = Modifier) {
    var isPlaying by remember { mutableStateOf(false) }
    var currentProgress by remember { mutableFloatStateOf(0.0f) }
    var elapsedTimeMillis by remember { mutableLongStateOf(0L) }
    val totalDurationMillis = 150000L

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (elapsedTimeMillis < totalDurationMillis) {
                kotlinx.coroutines.delay(1000L)
                elapsedTimeMillis += 1000L
                currentProgress = (elapsedTimeMillis.toFloat() / totalDurationMillis).coerceAtMost(1f)
            }
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
        Box(modifier = Modifier.fillMaxSize()) {
            DragToSwipeScreen(onActionTriggered = {}) {
                Scaffold { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFBBBBBB))
                            .padding(innerPadding),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {}
                }
            }
            PlayerControls(modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}
