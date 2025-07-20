package com.example.shoply.sharedPref

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch

object SettingsDataStore {
    private var dataStore: DataStore<Preferences>? = null

    private val Context.dataStore by preferencesDataStore(name = "user_settings")

    // Initialize the DataStore with the provided context
    fun initialize(context: Context) {
        try {
            if (dataStore == null) {
                dataStore = context.dataStore
                Log.d("SettingsDataStore", "DataStore initialized successfully")
            } else {
                Log.d("SettingsDataStore", "DataStore already initialized, skipping")
            }
        } catch (e: Exception) {
            Log.e("SettingsDataStore", "Error initializing DataStore: ${e.message}", e)
        }
    }

    // Get the dark theme preference as a Flow
    val isDarkTheme: Flow<Boolean>
        get() = try {
            dataStore?.data?.map { preferences ->
                preferences[DARK_THEME_KEY] ?: DEFAULT_DARK_THEME
            }?.catch { e ->
                Log.e("SettingsDataStore", "Error reading dark theme preference: ${e.message}", e)
                emit(DEFAULT_DARK_THEME)
            } ?: run {
                Log.e("SettingsDataStore", "DataStore not initialized for dark theme")
                throw IllegalStateException("SettingsDataStore not initialized")
            }
        } catch (e: Exception) {
            Log.e("SettingsDataStore", "Unexpected error accessing dark theme: ${e.message}", e)
            throw e
        }

    // Set the dark theme preference
    suspend fun setDarkTheme(enabled: Boolean) {
        try {
            dataStore?.edit { preferences ->
                preferences[DARK_THEME_KEY] = enabled
                Log.d("SettingsDataStore", "Dark theme set to: $enabled")
            } ?: run {
                Log.e("SettingsDataStore", "DataStore not initialized for setting dark theme")
                throw IllegalStateException("SettingsDataStore not initialized")
            }
        } catch (e: Exception) {
            Log.e("SettingsDataStore", "Error setting dark theme: ${e.message}", e)
        }
    }

    // Get the onboarding seen preference as a Flow
    val isOnboardingSeen: Flow<Boolean>
        get() = try {
            dataStore?.data?.map { preferences ->
                preferences[ONBOARDING_SEEN_KEY] ?: DEFAULT_ONBOARDING_SEEN
            }?.catch { e ->
                Log.e("SettingsDataStore", "Error reading onboarding seen preference: ${e.message}", e)
                emit(DEFAULT_ONBOARDING_SEEN)
            } ?: run {
                Log.e("SettingsDataStore", "DataStore not initialized for onboarding seen")
                throw IllegalStateException("SettingsDataStore not initialized")
            }
        } catch (e: Exception) {
            Log.e("SettingsDataStore", "Unexpected error accessing onboarding seen: ${e.message}", e)
            throw e
        }

    // Set the onboarding seen preference
    suspend fun setOnboardingSeen(seen: Boolean) {
        try {
            dataStore?.edit { preferences ->
                preferences[ONBOARDING_SEEN_KEY] = seen
                Log.d("SettingsDataStore", "Onboarding seen set to: $seen")
            } ?: run {
                Log.e("SettingsDataStore", "DataStore not initialized for setting onboarding seen")
                throw IllegalStateException("SettingsDataStore not initialized")
            }
        } catch (e: Exception) {
            Log.e("SettingsDataStore", "Error setting onboarding seen: ${e.message}", e)
        }
    }

    private val DARK_THEME_KEY = booleanPreferencesKey("dark_theme")
    const val DEFAULT_DARK_THEME = false
    private val ONBOARDING_SEEN_KEY = booleanPreferencesKey("onboarding_seen")
    private const val DEFAULT_ONBOARDING_SEEN = false
}