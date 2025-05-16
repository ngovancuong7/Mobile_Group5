package com.example.dictionary.data.model

import com.google.gson.annotations.SerializedName

data class RapidApiTranslationRequest(
    @SerializedName("from")
    val from: String,

    @SerializedName("to")
    val to: String,

    @SerializedName("json")
    val json: TranslationJson
)

data class TranslationJson(
    @SerializedName("text")
    val text: String
)
