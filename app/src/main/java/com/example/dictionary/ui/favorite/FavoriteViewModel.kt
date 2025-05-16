package com.example.dictionary.ui.favorite

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dictionary.data.model.WordWithFavorite
import com.example.dictionary.data.repository.DictionaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoriteViewModel @Inject constructor(
    private val dictionaryRepository: DictionaryRepository
) : ViewModel() {

    // Thêm biến để lưu trữ từ khóa tìm kiếm
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // State for selected items in multi-selection mode
    private val _selectedItems = MutableStateFlow<Set<String>>(emptySet())

    // State for selection mode
    private val _isInSelectionMode = MutableStateFlow(false)
    val isInSelectionMode = _isInSelectionMode.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )

    // Get all favorite words
    private val _favoriteWords = dictionaryRepository.getFavoriteWords()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    // Kết hợp danh sách từ yêu thích với từ khóa tìm kiếm
    val favoriteWords = combine(_favoriteWords, _searchQuery) { words, query ->
        if (query.isEmpty()) {
            words
        } else {
            words.filter { word ->
                word.word.word.contains(query, ignoreCase = true) ||
                        word.word.translation.contains(query, ignoreCase = true) ||
                        (word.note?.contains(query, ignoreCase = true) ?: false)
            }
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // Combine words with selected state
    val wordsWithSelection = combine(favoriteWords, _selectedItems) { words, selected ->
        words.map { word ->
            WordWithSelection(
                word = word,
                isSelected = selected.contains(word.word.word)
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // Get count of selected items
    private val _selectedItemsCount = MutableLiveData(0)
    val selectedItemsCount: LiveData<Int> = _selectedItemsCount

    init {
        viewModelScope.launch {
            _selectedItems.collect {
                _selectedItemsCount.value = it.size
            }
        }
    }

    // Thêm phương thức để cập nhật từ khóa tìm kiếm
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleFavorite(word: String) {
        viewModelScope.launch {
            dictionaryRepository.toggleFavorite(word)
        }
    }

    fun updateNote(word: String, note: String?) {
        viewModelScope.launch {
            dictionaryRepository.updateFavoriteNote(word, note)
        }
    }

    fun toggleSelectionMode() {
        _isInSelectionMode.value = !_isInSelectionMode.value
        if (!_isInSelectionMode.value) {
            clearSelection()
        }
    }

    fun toggleSelection(wordText: String) {
        val selected = _selectedItems.value.toMutableSet()
        if (selected.contains(wordText)) {
            selected.remove(wordText)
        } else {
            selected.add(wordText)
        }
        _selectedItems.value = selected

        // If no items selected and in selection mode, exit selection mode
        if (selected.isEmpty() && _isInSelectionMode.value) {
            _isInSelectionMode.value = false
        }
    }

    fun selectAll() {
        _selectedItems.value = favoriteWords.value.map { it.word.word }.toSet()
    }

    fun clearSelection() {
        _selectedItems.value = emptySet()
    }

    fun removeSelectedFromFavorites() {
        viewModelScope.launch {
            val itemsToRemove = _selectedItems.value.toList()
            dictionaryRepository.removeFavorites(itemsToRemove)
            clearSelection()
            _isInSelectionMode.value = false
        }
    }

    data class WordWithSelection(
        val word: WordWithFavorite,
        val isSelected: Boolean = false
    )
}
