package com.montanhajr.petclicker.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.montanhajr.petclicker.AdBanner
import com.montanhajr.petclicker.AppDestinations
import com.montanhajr.petclicker.MainActivity
import com.montanhajr.petclicker.MainScreen
import com.montanhajr.petclicker.SettingsScreen
import com.montanhajr.petclicker.data.UserPreferences
import com.montanhajr.petclicker.ui.theme.PetClickerTheme
import com.montanhajr.petclicker.viewmodel.MainViewModel
import com.montanhajr.petclicker.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetClickerApp(
    userPreferences: UserPreferences,
    mainViewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    onPlaySound: () -> Unit,
    showRewardedAd: (() -> Unit) -> Unit,
    showInterstitialAd: () -> Unit,
    onPurchasePremium: () -> Unit
) {
    val navController = rememberNavController()

    val isDarkTheme by settingsViewModel.isDarkTheme.collectAsState()
    val isPremium by settingsViewModel.isPremium.collectAsState()
    val selectedSound by mainViewModel.selectedSound.collectAsState()
    val isLockScreenFeatureEnabled by settingsViewModel.isLockScreenFeatureEnabled.collectAsState()
    
    var showFeatureExplanation by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val activity = context as? MainActivity
                if (activity?.justFinishedAd == true) {
                    activity.justFinishedAd = false
                } else {
                    settingsViewModel.enableLockScreenFeature(false)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    LaunchedEffect(isLockScreenFeatureEnabled) {
        (context as? MainActivity)?.enableLockScreenSession(isLockScreenFeatureEnabled, selectedSound)
    }

    PetClickerTheme(darkTheme = isDarkTheme) {
        Column(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = AppDestinations.MAIN_SCREEN,
                modifier = Modifier.weight(1f)
            ) {
                composable(AppDestinations.MAIN_SCREEN) {
                    MainScreen(
                        navController = navController,
                        mainViewModel = mainViewModel,
                        isPremium = isPremium,
                        onPlaySound = onPlaySound,
                        showInterstitialAd = showInterstitialAd
                    )
                }
                composable(AppDestinations.SETTINGS_SCREEN) {
                    SettingsScreen(
                        navController = navController,
                        isDarkTheme = isDarkTheme,
                        selectedSound = selectedSound,
                        isLockScreenFeatureEnabled = isLockScreenFeatureEnabled,
                        isPremium = isPremium,
                        onThemeChange = { settingsViewModel.updateTheme(it) },
                        onSoundSelected = { settingsViewModel.updateSound(it) },
                        onLockScreenFeatureToggle = {
                            settingsViewModel.enableLockScreenFeature(true)
                            showFeatureExplanation = true
                        },
                        showRewardedAd = showRewardedAd,
                        onPurchasePremium = onPurchasePremium
                    )
                }
            }
            if (!isPremium) {
                AdBanner(modifier = Modifier.navigationBarsPadding())
            }

            if (showFeatureExplanation) {
                VolumeFeatureInfoDialog(
                    onDismiss = { showFeatureExplanation = false }
                )
            }
        }
    }
}
