package com.montanhajr.petclicker

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

class SoundManager(private val context: Context) {
    private var soundPool: SoundPool? = null
    private var soundId: Int? = null
    private var currentSoundResId: Int? = null
    private var isLoaded = false

    init {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME) // USAGE_GAME é excelente para baixa latência e segue o volume de mídia
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
            
        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(attributes)
            .build()

        soundPool?.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                isLoaded = true
            }
        }
    }

    fun loadSound(resId: Int) {
        if (currentSoundResId == resId) return
        
        isLoaded = false
        currentSoundResId = resId
        soundId = soundPool?.load(context, resId, 1)
    }

    fun playSound() {
        // Se ainda não carregou ou falhou, tentamos carregar novamente se houver um ID
        soundId?.let { id ->
            soundPool?.play(id, 1f, 1f, 1, 0, 1f)
        }
    }

    fun release() {
        soundPool?.release()
        soundPool = null
    }
}
