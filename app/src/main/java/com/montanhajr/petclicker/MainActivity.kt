package com.montanhajr.petclicker

import android.media.AudioManager
import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media.VolumeProviderCompat
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
import com.montanhajr.petclicker.ui.PetClickerApp
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
    private var isLockScreenFeatureUserEnabled: Boolean = false
    
    // Flag para evitar que a feature seja desativada logo após assistir o AD
    var justFinishedAd: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        val userPreferences = UserPreferences(this)
        val settingsViewModel: SettingsViewModel by viewModels {
            SettingsViewModelFactory(userPreferences)
        }

        splashScreen.setKeepOnScreenCondition {
            !settingsViewModel.isReady.value
        }

        enableEdgeToEdge()

        soundManager = SoundManager(this)
        setupMediaSession()

        MobileAds.initialize(this) {}
        loadRewardedAd()
        loadInterstitialAd()

        volumeControlStream = AudioManager.STREAM_MUSIC

        setContent {
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
                settingsViewModel = settingsViewModel,
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
                        val state = PlaybackStateCompat.Builder()
                            .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE)
                            .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f)
                            .build()
                        setPlaybackState(state)
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
        // Inicialmente local para não interferir nos botões enquanto o app está aberto
        mediaSession?.setPlaybackToLocal(AudioManager.STREAM_MUSIC)
    }

    override fun onResume() {
        super.onResume()
        // Quando o app volta para o primeiro plano, restauramos o controle de volume normal do sistema
        mediaSession?.setPlaybackToLocal(AudioManager.STREAM_MUSIC)
    }

    override fun onPause() {
        super.onPause()
        // Quando o app sai do primeiro plano (ou a tela bloqueia), 
        // se a feature estiver habilitada, assumimos o controle dos botões
        if (isLockScreenFeatureUserEnabled) {
            volumeProvider?.let {
                mediaSession?.setPlaybackToRemote(it)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundManager.release()
        mediaSession?.release()
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

    private fun loadRewardedAd() {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(this, BuildConfig.REWARDED_AD_UNIT_ID, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) { rewardedAd = null }
            override fun onAdLoaded(ad: RewardedAd) { rewardedAd = ad }
        })
    }

    private fun showRewardedAd(onRewardEarned: () -> Unit) {
        rewardedAd?.let { ad ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() { adjustMediaVolume(reduce = true) }
                override fun onAdDismissedFullScreenContent() {
                    justFinishedAd = true
                    adjustMediaVolume(reduce = false)
                    rewardedAd = null
                    loadRewardedAd()
                }
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    justFinishedAd = true
                    adjustMediaVolume(reduce = false)
                    rewardedAd = null
                    loadRewardedAd()
                }
            }
            ad.show(this) { onRewardEarned() }
        } ?: run {
            Toast.makeText(this, getString(R.string.rewardedAdLoading), Toast.LENGTH_SHORT).show()
            loadRewardedAd()
        }
    }

    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(this, BuildConfig.INTERSTITIAL_AD_UNIT_ID, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) { interstitialAd = null }
            override fun onAdLoaded(ad: InterstitialAd) { interstitialAd = ad }
        })
    }

    private fun showInterstitialAd() {
        interstitialAd?.let { ad ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() { adjustMediaVolume(reduce = true) }
                override fun onAdDismissedFullScreenContent() {
                    justFinishedAd = true
                    adjustMediaVolume(reduce = false)
                    interstitialAd = null
                    loadInterstitialAd()
                }
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    justFinishedAd = true
                    adjustMediaVolume(reduce = false)
                    interstitialAd = null
                    loadInterstitialAd()
                }
            }
            ad.show(this)
        } ?: run { loadInterstitialAd() }
    }
    
    fun enableLockScreenSession(enabled: Boolean) {
        isLockScreenFeatureUserEnabled = enabled
        // Se desabilitar manualmente, voltamos ao local imediatamente
        if (!enabled) {
            mediaSession?.setPlaybackToLocal(AudioManager.STREAM_MUSIC)
        }
        // Se habilitar, não fazemos nada agora. O onPause cuidará de ativar o modo Remote.
    }
}
