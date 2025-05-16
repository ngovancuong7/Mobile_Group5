package com.example.dictionary.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sharedPreferences = context.getSharedPreferences("dictionary_prefs", Context.MODE_PRIVATE)

    fun saveRapidApiKey(apiKey: String) {
        sharedPreferences.edit().putString("rapid_api_key", apiKey).apply()
    }

    fun getRapidApiKey(): String {
        return sharedPreferences.getString("rapid_api_key", "") ?: ""
    }

    fun clearAllPreferences() {
        sharedPreferences.edit().clear().apply()
    }
}
