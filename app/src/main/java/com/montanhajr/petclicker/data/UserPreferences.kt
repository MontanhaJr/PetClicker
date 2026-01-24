package com.montanhajr.petclicker.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.montanhajr.petclicker.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val PREFERENCES_NAME = "pet_clicker_prefs"

val Context.dataStore by preferencesDataStore(name = PREFERENCES_NAME)

object PreferencesKeys {
    val SELECTED_SOUND_NAME = stringPreferencesKey("selected_sound_name")
    val DARK_THEME = booleanPreferencesKey("dark_theme")
}

class UserPreferences(private val context: Context) {

    val selectedSoundFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        val soundName = prefs[PreferencesKeys.SELECTED_SOUND_NAME] ?: "clicker1"
        // Converte o nome de volta para o ID do recurso em tempo de execução
        val resId = context.resources.getIdentifier(soundName, "raw", context.packageName)
        if (resId != 0) resId else R.raw.clicker1
    }

    val darkThemeFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.DARK_THEME] ?: false
    }

    suspend fun saveSelectedSound(soundResId: Int) {
        val soundName = context.resources.getResourceEntryName(soundResId)
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.SELECTED_SOUND_NAME] = soundName
        }
    }

    suspend fun saveDarkTheme(isDark: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.DARK_THEME] = isDark
        }
    }
}
