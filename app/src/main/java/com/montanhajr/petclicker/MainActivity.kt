package com.montanhajr.petclicker

import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.montanhajr.petclicker.billing.BillingManager
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
    private lateinit var billingManager: BillingManager
    
    private var originalVolume: Int = -1
    private var isLockScreenFeatureUserEnabled: Boolean = false
    private var currentSoundResId: Int = -1
    
    var justFinishedAd: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        val userPreferences = UserPreferences(this)
        val settingsViewModel: SettingsViewModel by viewModels {
            SettingsViewModelFactory(userPreferences)
        }

        billingManager = BillingManager(this, lifecycleScope) { isPremium ->
            settingsViewModel.updatePremiumStatus(isPremium)
        }
        billingManager.startConnection()

        splashScreen.setKeepOnScreenCondition {
            !settingsViewModel.isReady.value
        }

        enableEdgeToEdge()

        soundManager = SoundManager(this)

        MobileAds.initialize(this) {}
        loadRewardedAd()
        loadInterstitialAd()

        volumeControlStream = AudioManager.STREAM_MUSIC

        setContent {
            val mainViewModel: MainViewModel = viewModel(
                factory = MainViewModelFactory(userPreferences)
            )

            val selectedSound by mainViewModel.selectedSound.collectAsState()
            val isPremium by settingsViewModel.isPremium.collectAsState()
            val lockScreenEnabled by settingsViewModel.isLockScreenFeatureEnabled.collectAsState()

            LaunchedEffect(selectedSound) {
                currentSoundResId = selectedSound
                soundManager.loadSound(selectedSound)
                // Se a função estiver ativa, atualizamos o som no serviço também
                if (isLockScreenFeatureUserEnabled) {
                    startSoundService(selectedSound)
                }
            }
            
            LaunchedEffect(lockScreenEnabled) {
                enableLockScreenSession(lockScreenEnabled, currentSoundResId)
            }

            PetClickerApp(
                userPreferences = userPreferences,
                mainViewModel = mainViewModel,
                settingsViewModel = settingsViewModel,
                onPlaySound = { soundManager.playSound() },
                showRewardedAd = { onRewardEarned ->
                    if (isPremium) onRewardEarned() else showRewardedAd(onRewardEarned)
                },
                showInterstitialAd = {
                    if (!isPremium) showInterstitialAd()
                },
                onPurchasePremium = {
                    billingManager.launchBillingFlow(this@MainActivity)
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (::billingManager.isInitialized) {
            billingManager.queryPurchases()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundManager.release()
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
    
    fun enableLockScreenSession(enabled: Boolean, soundResId: Int) {
        isLockScreenFeatureUserEnabled = enabled
        if (enabled) {
            startSoundService(soundResId)
        } else {
            stopSoundService()
        }
    }

    private fun startSoundService(soundResId: Int) {
        val intent = Intent(this, SoundService::class.java).apply {
            putExtra(SoundService.EXTRA_SOUND_RES_ID, soundResId)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopSoundService() {
        val intent = Intent(this, SoundService::class.java)
        stopService(intent)
    }
}
