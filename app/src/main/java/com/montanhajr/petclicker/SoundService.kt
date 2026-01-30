package com.montanhajr.petclicker

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.VolumeProviderCompat

class SoundService : Service() {
    private var mediaSession: MediaSessionCompat? = null
    private lateinit var soundManager: SoundManager

    companion object {
        const val CHANNEL_ID = "pet_clicker_service_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_STOP = "STOP_SERVICE"
        const val EXTRA_SOUND_RES_ID = "SOUND_RES_ID"
    }

    override fun onCreate() {
        super.onCreate()
        soundManager = SoundManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val soundResId = intent?.getIntExtra(EXTRA_SOUND_RES_ID, -1) ?: -1
        if (soundResId != -1) {
            soundManager.loadSound(soundResId)
        }

        createNotificationChannel()
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Controle de volume ativo")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        // Android 14+ requer especificar o tipo no startForeground
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID, 
                notification, 
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        
        if (mediaSession == null) {
            setupMediaSession()
        }
        
        return START_STICKY
    }

    private fun setupMediaSession() {
        mediaSession = MediaSessionCompat(this, "PetClickerService").apply {
            val volumeProvider = object : VolumeProviderCompat(
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
            
            // Estado necessário para justificar o FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            val state = PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE)
                .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f)
                .build()
            
            setPlaybackState(state)
            setPlaybackToRemote(volumeProvider)
            isActive = true
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Pet Clicker Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        mediaSession?.apply {
            isActive = false
            release()
        }
        soundManager.release()
        super.onDestroy()
    }
}
