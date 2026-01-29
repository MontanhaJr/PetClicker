package com.montanhajr.petclicker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.montanhajr.petclicker.data.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val userPreferences: UserPreferences
) : ViewModel() {

    // Indica se as preferências já foram carregadas do DataStore
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    val isDarkTheme: StateFlow<Boolean> = userPreferences.darkThemeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false // Valor inicial temporário
        )

    private val _selectedSound = MutableStateFlow(com.montanhajr.petclicker.R.raw.clicker1)
    val selectedSound: StateFlow<Int> = _selectedSound.asStateFlow()

    private val _isLockScreenFeatureEnabled = MutableStateFlow(false)
    val isLockScreenFeatureEnabled: StateFlow<Boolean> = _isLockScreenFeatureEnabled.asStateFlow()

    init {
        viewModelScope.launch {
            // Quando recebermos o primeiro valor real do DataStore, marcamos como pronto
            userPreferences.darkThemeFlow.collectLatest {
                _isReady.value = true
            }
        }
        viewModelScope.launch {
            userPreferences.selectedSoundFlow.collectLatest { _selectedSound.value = it }
        }
    }

    fun updateTheme(isDark: Boolean) {
        viewModelScope.launch {
            userPreferences.saveDarkTheme(isDark)
        }
    }

    fun updateSound(soundResId: Int) {
        _selectedSound.value = soundResId
        viewModelScope.launch {
            userPreferences.saveSelectedSound(soundResId)
        }
    }

    fun enableLockScreenFeature(enabled: Boolean) {
        _isLockScreenFeatureEnabled.value = enabled
    }
}
