package com.example.dictionary.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dictionary.BuildConfig
import com.example.dictionary.util.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    fun getCurrentApiKey(): String {
        val savedKey = preferencesManager.getRapidApiKey()
        return if (savedKey.isEmpty()) BuildConfig.RAPID_API_KEY else savedKey
    }

    fun saveApiKey(apiKey: String) {
        viewModelScope.launch {
            preferencesManager.saveRapidApiKey(apiKey)
        }
    }
}
