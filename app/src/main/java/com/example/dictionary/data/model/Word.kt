package com.example.dictionary.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Ignore

@Entity(
    tableName = "word",
    indices = [Index(value = ["word"], unique = true)]
)
data class Word(
    @PrimaryKey
    val word: String,
    val translation: String,
    val phonetic: String = "",
    val partOfSpeech: String? = null,
    val definition: String? = null,

    // Lưu trữ dưới dạng chuỗi JSON thay vì List
    @ColumnInfo(name = "meanings_json")
    val meaningsJson: String? = null,

    @ColumnInfo(name = "synonyms_json")
    val synonymsJson: String? = null,

    @ColumnInfo(name = "antonyms_json")
    val antonymsJson: String? = null,

    @ColumnInfo(name = "examples_json")
    val examplesJson: String? = null,

    val timestamp: Long = System.currentTimeMillis(),
    val lastAccessed: Long = System.currentTimeMillis(),
    val searchCount: Int = 1,
    val audioUrl: String? = null,

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false
) {
    // Các trường không lưu trong database
    @Ignore
    val meanings: List<Meaning>? = null

    @Ignore
    val synonyms: List<String>? = null

    @Ignore
    val antonyms: List<String>? = null

    @Ignore
    val examples: List<Example>? = null
}

data class Meaning(
    val partOfSpeech: String,
    val definition: String,
    val examples: List<String>? = null,
    val synonyms: List<String>? = null,
    val antonyms: List<String>? = null
)

data class Example(
    val text: String,
    val translation: String? = null
)
