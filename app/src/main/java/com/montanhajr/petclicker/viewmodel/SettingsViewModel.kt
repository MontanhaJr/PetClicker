package com.montanhajr.petclicker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.montanhajr.petclicker.data.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _selectedSound = MutableStateFlow(com.montanhajr.petclicker.R.raw.clicker1)
    val selectedSound: StateFlow<Int> = _selectedSound.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferences.darkThemeFlow.collectLatest { _isDarkTheme.value = it }
        }
        viewModelScope.launch {
            userPreferences.selectedSoundFlow.collectLatest { _selectedSound.value = it }
        }
    }

    fun updateTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
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
}
