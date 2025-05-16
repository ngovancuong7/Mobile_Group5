package com.example.dictionary.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "dic_text",
    indices = [Index(value = ["sourceText", "sourceLanguage", "targetLanguage"], unique = true)]
)
data class DicText(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sourceText: String,
    val translatedText: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val timestamp: Long = System.currentTimeMillis(),
    val lastAccessed: Long = System.currentTimeMillis(),
    val searchCount: Int = 1
)
