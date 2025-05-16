package com.example.dictionary.data.model

data class LanguagesResponse(
    val languages: List<Language>
)

data class Language(
    val code: String,
    val name: String
)
