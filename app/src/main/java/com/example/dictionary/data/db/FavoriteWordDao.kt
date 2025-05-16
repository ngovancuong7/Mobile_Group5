package com.example.dictionary.data.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.dictionary.data.model.FavoriteWord
import com.example.dictionary.data.model.WordWithFavorite
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteWordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favoriteWord: FavoriteWord)

    @Update
    suspend fun update(favoriteWord: FavoriteWord)

    @Delete
    suspend fun delete(favoriteWord: FavoriteWord)

    @Query("SELECT * FROM favorite_word WHERE word = :word LIMIT 1")
    suspend fun getFavoriteWord(word: String): FavoriteWord?

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_word WHERE word = :word LIMIT 1)")
    suspend fun isFavorite(word: String): Boolean

    @Query("SELECT * FROM favorite_word ORDER BY addedTimestamp DESC")
    fun getAllFavoriteWords(): Flow<List<FavoriteWord>>

    @Transaction
    @Query("SELECT word.*, favorite_word.note AS note, CASE WHEN favorite_word.word IS NOT NULL THEN 1 ELSE 0 END AS isFavorite FROM word LEFT JOIN favorite_word ON word.word = favorite_word.word WHERE favorite_word.word IS NOT NULL ORDER BY favorite_word.addedTimestamp DESC")
    fun getFavoriteWordsWithDetails(): Flow<List<WordWithFavorite>>

    @Query("DELETE FROM favorite_word WHERE word = :word")
    suspend fun deleteFavoriteWord(word: String)

    @Query("DELETE FROM favorite_word WHERE word IN (:words)")
    suspend fun deleteFavoriteWords(words: List<String>)

    @Query("UPDATE favorite_word SET note = :note WHERE word = :word")
    suspend fun updateNote(word: String, note: String?)

    @Transaction
    suspend fun toggleFavorite(word: String): Boolean {
        val isFavorite = isFavorite(word)
        if (isFavorite) {
            deleteFavoriteWord(word)
        } else {
            insert(FavoriteWord(word = word))
        }
        return !isFavorite
    }
}
