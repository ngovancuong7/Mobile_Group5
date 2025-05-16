package com.example.dictionary.data.model

import com.google.gson.annotations.SerializedName

data class TranslationResponse(
    @SerializedName("translatedText")
    val translatedText: String,

    @SerializedName("alternatives")
    val alternatives: List<String>? = null
)
