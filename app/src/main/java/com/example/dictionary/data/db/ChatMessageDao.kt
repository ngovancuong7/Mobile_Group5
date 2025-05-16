package com.example.dictionary.data.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.dictionary.data.model.ChatMessage

@Dao
interface ChatMessageDao {
    @Insert
    suspend fun insert(chatMessage: ChatMessage)

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): LiveData<List<ChatMessage>>

    @Query("DELETE FROM chat_messages")
    suspend fun clearAllMessages()
}
