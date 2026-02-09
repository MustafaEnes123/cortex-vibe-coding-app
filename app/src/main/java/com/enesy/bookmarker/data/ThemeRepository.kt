package com.enesy.bookmarker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class ThemeRepository(private val context: Context) {

    private val isDarkModeKey = booleanPreferencesKey("is_dark_mode")
    private val IS_ONBOARDING_COMPLETE = booleanPreferencesKey("is_onboarding_complete")
    private val IS_AUTO_SYNC = booleanPreferencesKey("is_auto_sync")
    private val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
    private val LANGUAGE = stringPreferencesKey("language")

    fun getThemeMode(): Flow<Boolean?> {
        return context.dataStore.data.map {
            it[isDarkModeKey]
        }
    }

    suspend fun saveThemeMode(isDark: Boolean) {
        context.dataStore.edit {
            it[isDarkModeKey] = isDark
        }
    }

    val onboardingState: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[IS_ONBOARDING_COMPLETE] ?: false
        }

    suspend fun completeOnboarding() {
        context.dataStore.edit {
            it[IS_ONBOARDING_COMPLETE] = true
        }
    }

    val autoSyncState: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[IS_AUTO_SYNC] ?: true
        }

    suspend fun saveAutoSyncState(isAutoSync: Boolean) {
        context.dataStore.edit {
            it[IS_AUTO_SYNC] = isAutoSync
        }
    }

    fun getApiKey(): Flow<String?> {
        return context.dataStore.data.map {
            it[GEMINI_API_KEY]
        }
    }

    suspend fun saveApiKey(key: String) {
        context.dataStore.edit {
            it[GEMINI_API_KEY] = key
        }
    }

    suspend fun clearApiKey() {
        context.dataStore.edit {
            it.remove(GEMINI_API_KEY)
        }
    }

    fun getLanguage(): Flow<String> {
        return context.dataStore.data.map {
            it[LANGUAGE] ?: "en"
        }
    }

    suspend fun saveLanguage(language: String) {
        context.dataStore.edit {
            it[LANGUAGE] = language
        }
    }
}
