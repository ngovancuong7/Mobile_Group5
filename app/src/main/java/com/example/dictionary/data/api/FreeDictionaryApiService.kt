package com.example.dictionary.data.api

import com.example.dictionary.data.model.DictionaryEntry
import retrofit2.http.GET
import retrofit2.http.Path

interface FreeDictionaryApiService {
    @GET("entries/en/{word}")
    suspend fun getWordDefinition(@Path("word") word: String): List<DictionaryEntry>
}
