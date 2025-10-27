package com.montanhajr.petclicker

import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.montanhajr.petclicker.ui.theme.PetClickerTheme

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
            PetClickerTheme {
                PetClickerApp()
            }
        }
    }
}

@Composable
fun PetClickerApp() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = (AppDestinations.MAIN_SCREEN),
        modifier = Modifier.fillMaxSize()
    ) {
        composable(AppDestinations.MAIN_SCREEN) {
            MainScreen(navController = navController)
        }
        composable(AppDestinations.SETTINGS_SCREEN) {
            SettingsScreen(navController = navController)
        }
    }
}