package com.example.dictionary.data.model

import com.google.gson.annotations.SerializedName

data class DictionaryError(
    @SerializedName("title")
    val title: String? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("resolution")
    val resolution: String? = null
)
