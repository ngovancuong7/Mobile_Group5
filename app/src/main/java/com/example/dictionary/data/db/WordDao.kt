package com.example.dictionary.data.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.dictionary.data.model.Word
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(word: Word)

    @Update
    suspend fun update(word: Word)

    @Query("SELECT * FROM word WHERE word = :word LIMIT 1")
    suspend fun getWord(word: String): Word?

    @Query("SELECT * FROM word ORDER BY lastAccessed DESC")
    fun getAllWords(): Flow<List<Word>>

    @Query("SELECT * FROM word ORDER BY lastAccessed DESC LIMIT :limit")
    fun getRecentWords(limit: Int = 50): Flow<List<Word>>

    @Query("SELECT * FROM word WHERE word LIKE :searchQuery OR translation LIKE :searchQuery ORDER BY lastAccessed DESC")
    fun searchWords(searchQuery: String): Flow<List<Word>>

    @Query("DELETE FROM word WHERE word = :word")
    suspend fun deleteWord(word: String)

    @Query("DELETE FROM word WHERE word IN (:words)")
    suspend fun deleteWords(words: List<String>)

    @Query("DELETE FROM word")
    suspend fun deleteAllWords()

    @Query("UPDATE word SET lastAccessed = :timestamp, searchCount = searchCount + 1 WHERE word = :word")
    suspend fun updateAccessInfo(word: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE word SET is_favorite = :isFavorite WHERE word = :word")
    suspend fun updateFavorite(word: String, isFavorite: Boolean)

    @Transaction
    suspend fun insertOrUpdateWord(word: Word) {
        val existingWord = getWord(word.word)
        if (existingWord == null) {
            insert(word)
        } else {
            update(word.copy(
                searchCount = existingWord.searchCount + 1,
                lastAccessed = System.currentTimeMillis()
            ))
        }
    }
}
