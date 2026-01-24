package com.montanhajr.petclicker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media.VolumeProviderCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
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
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private var rewardedAd: RewardedAd? = null
    private var interstitialAd: InterstitialAd? = null
    private lateinit var soundManager: SoundManager
    private var mediaSession: MediaSessionCompat? = null
    private var volumeProvider: VolumeProviderCompat? = null
    
    private var originalVolume: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        soundManager = SoundManager(this)
        setupMediaSession()

        MobileAds.initialize(this) {}
        loadRewardedAd()
        loadInterstitialAd()

        volumeControlStream = AudioManager.STREAM_MUSIC

        setContent {
            val userPreferences = remember { UserPreferences(this) }
            
            val mainViewModel: MainViewModel = viewModel(
                factory = MainViewModelFactory(userPreferences)
            )
            val selectedSound by mainViewModel.selectedSound.collectAsState()

            LaunchedEffect(selectedSound) {
                soundManager.loadSound(selectedSound)
            }

            PetClickerApp(
                userPreferences = userPreferences,
                mainViewModel = mainViewModel,
                onPlaySound = { soundManager.playSound() },
                showRewardedAd = { onRewardEarned ->
                    showRewardedAd(onRewardEarned)
                },
                showInterstitialAd = {
                    showInterstitialAd()
                }
            )
        }
    }

    private fun setupMediaSession() {
        mediaSession = MediaSessionCompat(this, "PetClicker").apply {
            volumeProvider = object : VolumeProviderCompat(
                VOLUME_CONTROL_RELATIVE,
                100,
                50
            ) {
                override fun onAdjustVolume(direction: Int) {
                    if (direction != 0) {
                        soundManager.playSound()
                    }
                }
            }
            
            val state = PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE)
                .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f)
                .build()
            setPlaybackState(state)
            isActive = true
        }
    }

    override fun onStart() {
        super.onStart()
        // Quando o app volta para o primeiro plano, usamos o volume LOCAL (nativo)
        mediaSession?.setPlaybackToLocal(AudioManager.STREAM_MUSIC)
    }

    override fun onStop() {
        super.onStop()
        // Quando o app vai para segundo plano ou a tela bloqueia, usamos o volume REMOTE
        // Isso permite capturar os botões de volume sem mostrar a barra de volume do sistema
        volumeProvider?.let {
            mediaSession?.setPlaybackToRemote(it)
        }
    }

    private fun adjustMediaVolume(reduce: Boolean) {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        
        if (reduce) {
            originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val targetVolume = (maxVolume * 0.3f).roundToInt()
            if (originalVolume > targetVolume) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
            }
        } else if (originalVolume != -1) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0)
            originalVolume = -1
        }
    }

    // Removido o override de onKeyDown que interceptava os botões de volume,
    // permitindo que o sistema os trate nativamente enquanto o app está aberto.

    override fun onDestroy() {
        super.onDestroy()
        soundManager.release()
        mediaSession?.release()
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
        rewardedAd?.let { ad ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    adjustMediaVolume(reduce = true)
                }
                override fun onAdDismissedFullScreenContent() {
                    adjustMediaVolume(reduce = false)
                    rewardedAd = null
                    loadRewardedAd()
                }
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    adjustMediaVolume(reduce = false)
                    rewardedAd = null
                    loadRewardedAd()
                }
            }
            ad.show(this) {
                onRewardEarned()
            }
        } ?: run {
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
        interstitialAd?.let { ad ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    adjustMediaVolume(reduce = true)
                }
                override fun onAdDismissedFullScreenContent() {
                    adjustMediaVolume(reduce = false)
                    interstitialAd = null
                    loadInterstitialAd()
                }
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    adjustMediaVolume(reduce = false)
                    interstitialAd = null
                    loadInterstitialAd()
                }
            }
            ad.show(this)
        } ?: run {
            loadInterstitialAd()
        }
    }
}

@Composable
fun PetClickerApp(
    userPreferences: UserPreferences,
    mainViewModel: MainViewModel,
    onPlaySound: () -> Unit,
    showRewardedAd: (() -> Unit) -> Unit,
    showInterstitialAd: () -> Unit
) {
    val navController = rememberNavController()

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
                        onPlaySound = onPlaySound,
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
