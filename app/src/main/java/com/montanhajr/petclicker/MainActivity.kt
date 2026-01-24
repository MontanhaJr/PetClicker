package com.montanhajr.petclicker

import android.media.AudioManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.montanhajr.petclicker.data.UserPreferences
import com.montanhajr.petclicker.ui.theme.PetClickerTheme
import com.montanhajr.petclicker.viewmodel.MainViewModel
import com.montanhajr.petclicker.viewmodel.MainViewModelFactory
import com.montanhajr.petclicker.viewmodel.SettingsViewModel
import com.montanhajr.petclicker.viewmodel.SettingsViewModelFactory

class MainActivity : ComponentActivity() {
    private var rewardedAd: RewardedAd? = null
    private var interstitialAd: InterstitialAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        MobileAds.initialize(this) {}
        loadRewardedAd()
        loadInterstitialAd()

        try {
            val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            val userPreferences = remember { UserPreferences(this) }
            PetClickerApp(
                userPreferences = userPreferences,
                showRewardedAd = { onRewardEarned ->
                    showRewardedAd(onRewardEarned)
                },
                showInterstitialAd = {
                    showInterstitialAd()
                }
            )
        }
    }

    private fun loadRewardedAd() {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(this, BuildConfig.REWARDED_AD_UNIT_ID, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                rewardedAd = null
            }
            override fun onAdLoaded(ad: RewardedAd) {
                rewardedAd = ad
            }
        })
    }

    private fun showRewardedAd(onRewardEarned: () -> Unit) {
        if (rewardedAd != null) {
            rewardedAd?.show(this) {
                onRewardEarned()
                loadRewardedAd()
            }
        } else {
            Toast.makeText(this, getString(R.string.rewardedAdLoading), Toast.LENGTH_SHORT).show()
            loadRewardedAd()
        }
    }

    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(this, BuildConfig.INTERSTITIAL_AD_UNIT_ID, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                interstitialAd = null
            }
            override fun onAdLoaded(ad: InterstitialAd) {
                interstitialAd = ad
            }
        })
    }

    private fun showInterstitialAd() {
        if (interstitialAd != null) {
            interstitialAd?.show(this)
            loadInterstitialAd() // Carrega o próximo após exibir
        } else {
            loadInterstitialAd()
        }
    }
}

@Composable
fun PetClickerApp(
    userPreferences: UserPreferences,
    showRewardedAd: (() -> Unit) -> Unit,
    showInterstitialAd: () -> Unit
) {
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
        Column(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = AppDestinations.MAIN_SCREEN,
                modifier = Modifier.weight(1f)
            ) {
                composable(AppDestinations.MAIN_SCREEN) {
                    MainScreen(
                        navController = navController,
                        selectedSound = selectedSound,
                        showInterstitialAd = showInterstitialAd
                    )
                }
                composable(AppDestinations.SETTINGS_SCREEN) {
                    SettingsScreen(
                        navController = navController,
                        isDarkTheme = isDarkTheme,
                        selectedSound = selectedSound,
                        onThemeChange = { viewModel.updateTheme(it) },
                        onSoundSelected = { viewModel.updateSound(it) },
                        showRewardedAd = showRewardedAd
                    )
                }
            }
            AdBanner(modifier = Modifier.navigationBarsPadding())
        }
    }
}
