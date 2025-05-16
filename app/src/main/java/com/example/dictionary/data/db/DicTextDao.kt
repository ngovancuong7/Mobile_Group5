package com.example.dictionary.data.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.dictionary.data.model.DicText
import kotlinx.coroutines.flow.Flow

@Dao
interface DicTextDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dicText: DicText): Long

    @Update
    suspend fun update(dicText: DicText)

    @Query("SELECT * FROM dic_text WHERE sourceText = :sourceText AND sourceLanguage = :sourceLanguage AND targetLanguage = :targetLanguage LIMIT 1")
    suspend fun getDicText(sourceText: String, sourceLanguage: String, targetLanguage: String): DicText?

    @Query("SELECT * FROM dic_text ORDER BY lastAccessed DESC")
    fun getAllDicTexts(): Flow<List<DicText>>

    @Query("SELECT * FROM dic_text ORDER BY lastAccessed DESC LIMIT :limit")
    fun getRecentDicTexts(limit: Int = 50): Flow<List<DicText>>

    @Query("SELECT * FROM dic_text WHERE sourceText LIKE :searchQuery OR translatedText LIKE :searchQuery ORDER BY lastAccessed DESC")
    fun searchDicTexts(searchQuery: String): Flow<List<DicText>>

    @Query("DELETE FROM dic_text WHERE id = :id")
    suspend fun deleteDicText(id: Long)

    @Query("DELETE FROM dic_text WHERE id IN (:ids)")
    suspend fun deleteDicTexts(ids: List<Long>)

    @Query("DELETE FROM dic_text")
    suspend fun deleteAllDicTexts()

    @Query("UPDATE dic_text SET lastAccessed = :timestamp, searchCount = searchCount + 1 WHERE id = :id")
    suspend fun updateAccessInfo(id: Long, timestamp: Long = System.currentTimeMillis())

    @Transaction
    suspend fun insertOrUpdateDicText(dicText: DicText): Long {
        val existingDicText = getDicText(dicText.sourceText, dicText.sourceLanguage, dicText.targetLanguage)
        return if (existingDicText == null) {
            insert(dicText)
        } else {
            update(dicText.copy(
                id = existingDicText.id,
                searchCount = existingDicText.searchCount + 1,
                lastAccessed = System.currentTimeMillis()
            ))
            existingDicText.id
        }
    }
}
