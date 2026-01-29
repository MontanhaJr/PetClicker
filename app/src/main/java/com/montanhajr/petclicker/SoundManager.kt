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
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION) // Melhor para efeitos sonoros de UI/Cliques
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setFlags(AudioAttributes.FLAG_LOW_LATENCY) // Solicita baixa latência
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(5) // Aumentar um pouco ajuda a evitar cortes se clicar rápido
            .setAudioAttributes(attributes)
            .build()

        soundPool?.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0 && sampleId == soundId) {
                isLoaded = true
            }
        }
    }

    fun loadSound(resId: Int) {
        if (currentSoundResId == resId) return
        
        soundId?.let { soundPool?.unload(it) }
        isLoaded = false
        currentSoundResId = resId
        soundId = soundPool?.load(context, resId, 1)
    }

    fun playSound() {
        if (isLoaded) {
            soundId?.let {
                // Tocar com prioridade máxima (1)
                soundPool?.play(it, 1f, 1f, 1, 0, 1f)
            }
        }
    }

    fun release() {
        soundPool?.release()
        soundPool = null
    }
}
