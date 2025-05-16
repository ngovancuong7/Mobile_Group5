package com.example.dictionary.data.api

import com.example.dictionary.data.model.RapidApiTranslationRequest
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface RapidApiTranslateService {
    @Headers(
        "Content-Type: application/json",
        "X-RapidAPI-Host: google-translate113.p.rapidapi.com"
    )
    @POST("api/v1/translator/json")
    suspend fun translate(
        @Header("X-RapidAPI-Key") apiKey: String,
        @Body request: RapidApiTranslationRequest
    ): Any // Sử dụng Any vì response có thể là bất kỳ JSON object nào
}
