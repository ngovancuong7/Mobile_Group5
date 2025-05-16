package com.example.dictionary.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class WordWithFavorite(
    @Embedded
    val word: Word,

    val note: String? = null,

    val isFavorite: Boolean = false
)
