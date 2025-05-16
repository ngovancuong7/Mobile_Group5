package com.example.dictionary.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dictionary.data.model.Word
import com.example.dictionary.data.repository.DictionaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val dictionaryRepository: DictionaryRepository
) : ViewModel() {

    // Thêm các biến và phương thức để xử lý tìm kiếm
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Thay thế biến allWords hiện tại bằng phiên bản mới có tìm kiếm
    val allWords = combine(
        dictionaryRepository.getSearchHistory(),
        _searchQuery
    ) { words, query ->
        if (query.isEmpty()) {
            words
        } else {
            words.filter { word ->
                word.word.contains(query, ignoreCase = true) ||
                        word.translation.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleFavorite(word: String) {
        viewModelScope.launch {
            dictionaryRepository.toggleFavorite(word)
        }
    }

    fun deleteWord(word: String) {
        viewModelScope.launch {
            dictionaryRepository.deleteSearchHistory(listOf(word))
        }
    }

    fun deleteAllHistory() {
        viewModelScope.launch {
            dictionaryRepository.deleteAllSearchHistory()
        }
    }
}
