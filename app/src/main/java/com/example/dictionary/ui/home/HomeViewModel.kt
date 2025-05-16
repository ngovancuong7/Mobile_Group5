package com.example.dictionary.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dictionary.data.model.Word
import com.example.dictionary.data.repository.TranslationRepository
import com.example.dictionary.data.repository.WordRepository
import com.example.dictionary.util.NetworkResult
import com.example.dictionary.util.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val translationRepository: TranslationRepository,
    private val wordRepository: WordRepository,
    private val networkUtils: NetworkUtils
) : ViewModel() {

    private val _translationResult = MutableStateFlow<NetworkResult<String>?>(null)
    val translationResult: StateFlow<NetworkResult<String>?> = _translationResult

    private val _currentWord = MutableLiveData<Word?>()
    val currentWord: LiveData<Word?> = _currentWord

    fun translateText(text: String, sourceLanguage: String, targetLanguage: String) {
        if (!networkUtils.isNetworkAvailable()) {
            _translationResult.value = NetworkResult.Error("Không có kết nối mạng. Vui lòng kiểm tra lại.")
            return
        }

        viewModelScope.launch {
            translationRepository.translateText(text, sourceLanguage, targetLanguage)
                .collectLatest { result ->
                    _translationResult.value = result

                    // If it's a single word, check if it's in favorites
                    if (!text.contains(" ") && result is NetworkResult.Success) {
                        checkWordStatus(text)
                    }
                }
        }
    }

    private fun checkWordStatus(word: String) {
        viewModelScope.launch {
            _currentWord.value = wordRepository.getWord(word)
        }
    }

    fun toggleFavorite(word: String) {
        viewModelScope.launch {
            wordRepository.toggleFavorite(word)
            checkWordStatus(word)
        }
    }
}
