package com.lebaillyapp.beatvibrator

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.content.PermissionChecker
import com.lebaillyapp.beatvibrator.ui.player.MicroPlayerComponent
import com.lebaillyapp.beatvibrator.ui.pullToLoad.PullToLoadScreen
import com.lebaillyapp.beatvibrator.ui.theme.BeatVibratorTheme
import com.lebaillyapp.beatvibrator.ui.visualizer.PulseVisualizer
import com.lebaillyapp.beatvibrator.ui.visualizer.customShape.BulgedRoundedRectangleShape
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

    private lateinit var pickAudioLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private var pendingOpenPicker = false

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialise le launcher SAF pour choisir un fichier audio
        pickAudioLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                try {
                    contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: SecurityException) {
                    Log.w("MainActivity", "Impossible de prendre permission persistante", e)
                }
                Log.d("MainActivity", "Musique sélectionnée : $it")
                Toast.makeText(this, "Musique choisie : $it", Toast.LENGTH_SHORT).show()
            } ?: run {
                Toast.makeText(this, "Sélection annulée", Toast.LENGTH_SHORT).show()
            }
        }

        // Launcher pour demander la permission runtime adaptée SAF
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                if (pendingOpenPicker) {
                    pendingOpenPicker = false
                    launchAudioPicker()
                }
            } else {
                Toast.makeText(
                    this,
                    "Permission refusée. Impossible de sélectionner une musique.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

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

                val startColor = Color(0xFFC6FF00)
                val endColor = Color(0xFF1DE9B6)
                var pullOffset by remember { mutableFloatStateOf(0f) }
                val pullThreshold = 650f
                val progress = (pullOffset / pullThreshold).coerceIn(0f, 1f)
                val backgroundColor = lerp(startColor, endColor, progress)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(backgroundColor)
                ) {
                    PullToLoadScreen(
                        onOffsetChanged = { offset -> pullOffset = offset },
                        onActionTriggered = {
                            if (hasAudioPermission()) {
                                launchAudioPicker()
                            } else {
                                pendingOpenPicker = true
                                requestAudioPermission()
                            }
                        }
                    ) {
                        Card(
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(
                                topStart = 30.dp,
                                topEnd = 30.dp,
                                bottomStart = 20.dp,
                                bottomEnd = 20.dp
                            ),
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
                                    PulseVisualizerScreen()
                                }
                            }
                        }
                    }
                    PlayerControls(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 20.dp)
                    )
                }
            }
        }
    }




    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @Composable
    fun PulseVisualizerScreen() {
        val context = LocalContext.current

        // 1. Chargez une ImageBitmap à utiliser comme fond
        // Placez une image dans res/drawable (par exemple, 'background_image.jpg')
        val backgroundImageBitmap: ImageBitmap = remember {
            BitmapFactory.decodeResource(context.resources, R.drawable.cover_example_2).asImageBitmap()
        }

        // 2. Passez l'ImageBitmap et l'ID du shader à votre composable PulseVisualizer
        // Nous enveloppons maintenant le PulseVisualizer dans une Card

        // Définissez la forme custom
        val customBulgedShape = remember {
            BulgedRoundedRectangleShape(
                cornerRadius = 25.dp,
                bulgeAmount = 0.03f
            )
        }


        Card(
            modifier = Modifier
                .width(300.dp)
                .height(300.dp),
            shape = customBulgedShape,
            colors = CardDefaults.cardColors(containerColor = Color(0xFFBABABA)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp) // Élévation pour la Card du visualiseur
        ) {
            Box(modifier = Modifier.fillMaxSize()) { // Le Box existant pour le fillMaxSize du PulseVisualizer
                PulseVisualizer(
                    modifier = Modifier.fillMaxSize(),
                    bitmap = backgroundImageBitmap,
                    shaderResId = R.raw.pulse_visualizer, //   fichier AGSL
                    shape = customBulgedShape
                )
            }
        }
    }








    private fun hasAudioPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_MEDIA_AUDIO
            ) == PermissionChecker.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PermissionChecker.PERMISSION_GRANTED
        }
    }

    private fun requestAudioPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(android.Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun launchAudioPicker() {
        pickAudioLauncher.launch(arrayOf("audio/*"))
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

/**
 * Un composable qui définit la couleur et le style des barres système.
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
