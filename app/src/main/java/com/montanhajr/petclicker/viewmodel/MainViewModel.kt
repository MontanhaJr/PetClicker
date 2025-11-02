package com.montanhajr.petclicker.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.montanhajr.petclicker.data.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class MainViewModel(
    userPreferences: UserPreferences
) : ViewModel() {

    val selectedSound: StateFlow<Int> = userPreferences.selectedSoundFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = com.montanhajr.petclicker.R.raw.clicker1
        )
}