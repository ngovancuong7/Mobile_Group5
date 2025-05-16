package com.example.dictionary.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "favorite_word",
    foreignKeys = [
        ForeignKey(
            entity = Word::class,
            parentColumns = ["word"],
            childColumns = ["word"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["word"], unique = true)]
)
data class FavoriteWord(
    @PrimaryKey
    val word: String,
    val addedTimestamp: Long = System.currentTimeMillis(),
    val note: String? = null
)
