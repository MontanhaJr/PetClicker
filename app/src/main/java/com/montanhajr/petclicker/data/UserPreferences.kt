package com.montanhajr.petclicker.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.montanhajr.petclicker.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val PREFERENCES_NAME = "pet_clicker_prefs"

val Context.dataStore by preferencesDataStore(name = PREFERENCES_NAME)

object PreferencesKeys {
    val SELECTED_SOUND = intPreferencesKey("selected_sound")
    val DARK_THEME = booleanPreferencesKey("dark_theme")
}

class UserPreferences(private val context: Context) {

    val selectedSoundFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.SELECTED_SOUND] ?: R.raw.clicker1
    }

    val darkThemeFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.DARK_THEME] ?: false
    }

    suspend fun saveSelectedSound(soundResId: Int) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.SELECTED_SOUND] = soundResId
        }
    }

    suspend fun saveDarkTheme(isDark: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.DARK_THEME] = isDark
        }
    }
}
