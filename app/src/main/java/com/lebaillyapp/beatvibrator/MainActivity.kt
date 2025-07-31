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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
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
                // Définis la couleur désirée pour tes barres ici
                val desiredBarColor = Color(0xFFBABABA)

                // Appelle ta fonction pour définir les couleurs des barres système
                SetSystemBarsColor(
                    statusBarColor = desiredBarColor,
                    navigationBarColor = desiredBarColor,
                    useLightStatusBarIcons = true, // Icônes claires sur fond sombre
                    useLightNavigationBarIcons = true // Icônes claires sur fond sombre
                )

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Pour centrer ou positionner ton player, utilise un Column ou Box
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding) // Important pour les insets de Scaffold
                            .background(Color(0xFFBBBBBB)),
                        horizontalAlignment = Alignment.CenterHorizontally, // Centre horizontalement
                        verticalArrangement = Arrangement.Bottom // Centre verticalement
                    ) {
                        MicroPlayerComponent(
                            modifier = Modifier.padding(26.dp),
                            songTitle = "Beautiful Things",
                            albumName = "FireWork and Rollerblades",
                            artistName = "Benson Boone",
                            initialProgress = 0.0f,
                            elevation = 8.dp
                        )

                    }
                }
            }
        }


    }
}

/**
 * Définit la couleur de la barre de statut et de navigation du système.
 *
 * @param statusBarColor La couleur désirée pour la barre de statut.
 * @param navigationBarColor La couleur désirée pour la barre de navigation.
 * @param useLightStatusBarIcons Si vrai, les icônes de la barre de statut seront sombres (pour fonds clairs).
 * Si faux, les icônes seront claires (pour fonds sombres).
 * @param useLightNavigationBarIcons Si vrai, les icônes de la barre de navigation seront sombres (pour fonds clairs).
 * Si faux, les icônes seront claires (pour fonds sombres).
 */
@Composable
fun SetSystemBarsColor(
    statusBarColor: Color,
    navigationBarColor: Color,
    useLightStatusBarIcons: Boolean = false, // Par défaut: icônes claires pour fond sombre
    useLightNavigationBarIcons: Boolean = false // Par défaut: icônes claires pour fond sombre
) {
    val view = LocalView.current
    if (!view.isInEditMode) { // S'assure que le code ne s'exécute pas en mode aperçu/édition
        SideEffect {
            val window = (view.context as Activity).window
            // S'assurer que le contenu s'étend derrière les barres (souvent géré par enableEdgeToEdge())
            WindowCompat.setDecorFitsSystemWindows(window, false)

            // Définir les couleurs des barres
            window.statusBarColor = statusBarColor.toArgb()
            window.navigationBarColor = navigationBarColor.toArgb()

            // Contrôler l'apparence des icônes
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = useLightStatusBarIcons
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = useLightNavigationBarIcons
        }
    }
}

