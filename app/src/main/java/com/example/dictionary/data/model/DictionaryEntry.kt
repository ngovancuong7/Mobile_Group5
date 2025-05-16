package com.example.dictionary.data.model

import com.google.gson.annotations.SerializedName

data class DictionaryEntry(
    @SerializedName("word")
    val word: String,

    @SerializedName("phonetic")
    val phonetic: String? = null,

    @SerializedName("phonetics")
    val phonetics: List<Phonetic>? = null,

    @SerializedName("meanings")
    val meanings: List<DictionaryMeaning>? = null,

    @SerializedName("origin")
    val origin: String? = null
) {
    data class Phonetic(
        @SerializedName("text")
        val text: String? = null,

        @SerializedName("audio")
        val audio: String? = null
    )

    data class DictionaryMeaning(
        @SerializedName("partOfSpeech")
        val partOfSpeech: String? = null,

        @SerializedName("definitions")
        val definitions: List<Definition>? = null,

        @SerializedName("synonyms")
        val synonyms: List<String>? = null,

        @SerializedName("antonyms")
        val antonyms: List<String>? = null
    )

    data class Definition(
        @SerializedName("definition")
        val definition: String? = null,

        @SerializedName("example")
        val example: String? = null,

        @SerializedName("synonyms")
        val synonyms: List<String>? = null,

        @SerializedName("antonyms")
        val antonyms: List<String>? = null
    )
}
