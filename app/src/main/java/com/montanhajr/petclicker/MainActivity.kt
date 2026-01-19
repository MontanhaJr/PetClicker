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
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.ads.MobileAds
import com.montanhajr.petclicker.data.UserPreferences
import com.montanhajr.petclicker.ui.theme.PetClickerTheme
import com.montanhajr.petclicker.viewmodel.MainViewModel
import com.montanhajr.petclicker.viewmodel.MainViewModelFactory
import com.montanhajr.petclicker.viewmodel.SettingsViewModel
import com.montanhajr.petclicker.viewmodel.SettingsViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inicializa o SDK do Mobile Ads
        MobileAds.initialize(this) {}

        try {
            val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            val userPreferences = remember { UserPreferences(this) }
            PetClickerApp(userPreferences)
        }
    }
}

@Composable
fun PetClickerApp(userPreferences: UserPreferences) {
    val navController = rememberNavController()

    val mainViewModel: MainViewModel = viewModel(
        factory = MainViewModelFactory(userPreferences)
    )
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(userPreferences)
    )

    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val selectedSound by mainViewModel.selectedSound.collectAsState()

    PetClickerTheme(darkTheme = isDarkTheme) {
        NavHost(
            navController = navController,
            startDestination = AppDestinations.MAIN_SCREEN,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(AppDestinations.MAIN_SCREEN) {
                MainScreen(
                    navController = navController,
                    selectedSound = selectedSound
                )
            }
            composable(AppDestinations.SETTINGS_SCREEN) {
                SettingsScreen(
                    navController = navController,
                    isDarkTheme = isDarkTheme,
                    onThemeChange = { viewModel.updateTheme(it) },
                    onSoundSelected = { viewModel.updateSound(it) }
                )
            }
        }
    }
}
