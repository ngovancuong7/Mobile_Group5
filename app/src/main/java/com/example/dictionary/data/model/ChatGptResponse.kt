package com.example.dictionary.data.model

import com.google.gson.annotations.SerializedName

data class ChatGptResponse(
    @SerializedName("response")
    val message: String,

    @SerializedName("status")
    val status: String,

    @SerializedName("timestamp")
    val timestamp: String? = null
)
