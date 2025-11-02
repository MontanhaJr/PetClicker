package com.montanhajr.petclicker

import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.montanhajr.petclicker.data.UserPreferences
import com.montanhajr.petclicker.ui.theme.PetClickerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            val userPreferences = remember { UserPreferences(this) }

            val isDarkTheme by userPreferences.darkThemeFlow.collectAsState(initial = false)
            val selectedSound by userPreferences.selectedSoundFlow.collectAsState(
                initial = R.raw.clicker1
            )

            PetClickerTheme(darkTheme = isDarkTheme) {
                PetClickerApp(
                    userPreferences = userPreferences,
                    isDarkTheme = isDarkTheme,
                    selectedSound = selectedSound
                )
            }
        }
    }
}

@Composable
fun PetClickerApp(
    userPreferences: UserPreferences,
    isDarkTheme: Boolean,
    selectedSound: Int
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    PetClickerTheme(darkTheme = isDarkTheme) {
        NavHost(
            navController = navController,
            startDestination = AppDestinations.MAIN_SCREEN,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(AppDestinations.MAIN_SCREEN) {
                MainScreen(
                    navController = navController,
                    defaultSound = selectedSound
                )
            }
            composable(AppDestinations.SETTINGS_SCREEN) {
                SettingsScreen(
                    navController = navController,
                    isDarkTheme = isDarkTheme,
                    onThemeChange = { isDark ->
                        scope.launch {
                            userPreferences.saveDarkTheme(isDark)
                        }
                    },
                    onSoundSelected = { soundResId ->
                        scope.launch {
                            userPreferences.saveSelectedSound(soundResId)
                        }
                    }
                )
            }
        }
    }
}
