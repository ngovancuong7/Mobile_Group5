package com.example.dictionary.ui.dictionary

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dictionary.data.model.DictionaryEntry
import com.example.dictionary.data.repository.DictionaryRepository
import com.example.dictionary.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DictionaryViewModel @Inject constructor(
    private val dictionaryRepository: DictionaryRepository
) : ViewModel() {

    private val _lookupResult = MutableLiveData<NetworkResult<Pair<String, List<DictionaryEntry>>>>()
    val lookupResult: LiveData<NetworkResult<Pair<String, List<DictionaryEntry>>>> = _lookupResult

    // Thêm biến để lưu trữ từ đã tra cứu gần đây nhất
    private val _lastLookedUpWord = MutableLiveData<LastLookedUpWord?>()
    val lastLookedUpWord: LiveData<LastLookedUpWord?> = _lastLookedUpWord

    private val _isFavorite = MutableLiveData<Boolean>()
    val isFavorite: LiveData<Boolean> = _isFavorite

    // Thêm data class để lưu trữ thông tin từ đã tra cứu
    data class LastLookedUpWord(
        val word: String,
        val translation: String
    )

    fun lookupWord(word: String) {
        viewModelScope.launch {
            dictionaryRepository.lookupWord(word)
                .collectLatest { result ->
                    _lookupResult.value = result

                    // Lưu từ đã tra cứu
                    if (result is NetworkResult.Success) {
                        val (translation, _) = result.data!!
                        _lastLookedUpWord.value = LastLookedUpWord(word, translation)

                        // Kiểm tra trạng thái yêu thích
                        checkFavoriteStatus(word)
                    }
                }
        }
    }

    fun toggleFavorite(word: String) {
        viewModelScope.launch {
            val newStatus = dictionaryRepository.toggleFavorite(word)
            _isFavorite.value = newStatus
        }
    }

    private fun checkFavoriteStatus(word: String) {
        viewModelScope.launch {
            val isFavorite = dictionaryRepository.getWordFavoriteStatus(word)
            _isFavorite.value = isFavorite
        }
    }
}
