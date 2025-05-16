package com.example.dictionary.data.api

import com.example.dictionary.data.model.ChatGptResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface ChatGptApiService {
    @GET("chat-completion-one")
    suspend fun sendMessage(
        @Header("X-RapidAPI-Key") apiKey: String,
        @Header("X-RapidAPI-Host") host: String = "free-chatgpt-api.p.rapidapi.com",
        @Query("prompt") prompt: String
    ): ChatGptResponse
}
