package com.example.dictionary.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.example.dictionary.data.db.WordDao
import com.example.dictionary.data.model.Word
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WordRepository @Inject constructor(
    private val wordDao: WordDao
) {
    val allWords: Flow<List<Word>> = wordDao.getAllWords()
    val recentWords: Flow<List<Word>> = wordDao.getRecentWords()

    suspend fun getWord(word: String): Word? {
        return wordDao.getWord(word)
    }

    suspend fun insertWord(word: Word) {
        wordDao.insert(word)
    }

    suspend fun updateWord(word: Word) {
        wordDao.update(word)
    }

    suspend fun toggleFavorite(word: String) {
        val currentWord = wordDao.getWord(word)
        val isFavorite = !(currentWord?.isFavorite ?: false)
        wordDao.updateFavorite(word, isFavorite)
    }
}
