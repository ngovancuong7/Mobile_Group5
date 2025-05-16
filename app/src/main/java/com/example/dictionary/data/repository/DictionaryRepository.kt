package com.example.dictionary.data.repository

import android.util.Log
import com.example.dictionary.BuildConfig
import com.example.dictionary.data.api.FreeDictionaryApiService
import com.example.dictionary.data.api.RapidApiTranslateService
import com.example.dictionary.data.db.DicTextDao
import com.example.dictionary.data.db.FavoriteWordDao
import com.example.dictionary.data.db.WordDao
import com.example.dictionary.data.model.DicText
import com.example.dictionary.data.model.DictionaryEntry
import com.example.dictionary.data.model.DictionaryEntry.DictionaryMeaning
import com.example.dictionary.data.model.DictionaryEntry.Definition
import com.example.dictionary.data.model.DictionaryEntry.Phonetic
import com.example.dictionary.data.model.Example
import com.example.dictionary.data.model.FavoriteWord
import com.example.dictionary.data.model.Meaning
import com.example.dictionary.data.model.RapidApiTranslationRequest
import com.example.dictionary.data.model.TranslationJson
import com.example.dictionary.data.model.Word
import com.example.dictionary.data.model.WordWithFavorite
import com.example.dictionary.util.NetworkResult
import com.example.dictionary.util.NetworkUtils
import com.example.dictionary.util.PreferencesManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DictionaryRepository @Inject constructor(
    private val freeDictionaryApiService: FreeDictionaryApiService,
    private val rapidApiTranslateService: RapidApiTranslateService,
    private val wordDao: WordDao,
    private val favoriteWordDao: FavoriteWordDao,
    private val dicTextDao: DicTextDao,
    private val networkUtils: NetworkUtils,
    private val preferencesManager: PreferencesManager,
    private val gson: Gson
) {
    private val TAG = "DictionaryRepository"

    suspend fun lookupWord(word: String): Flow<NetworkResult<Pair<String, List<DictionaryEntry>>>> = flow {
        emit(NetworkResult.Loading())

        try {
            // Kiểm tra xem từ có trong cơ sở dữ liệu không
            val cachedWord = wordDao.getWord(word)
            val isNetworkAvailable = networkUtils.isNetworkAvailable()

            if (cachedWord != null) {
                // Cập nhật thông tin truy cập
                wordDao.updateAccessInfo(word)

                // Nếu có dữ liệu trong DB và không có internet, sử dụng dữ liệu từ DB
                if (!isNetworkAvailable) {
                    // Chuyển đổi JSON thành đối tượng
                    val meanings = parseJsonToMeanings(cachedWord.meaningsJson)

                    val cachedMeanings = meanings?.map { meaning ->
                        DictionaryMeaning(
                            partOfSpeech = meaning.partOfSpeech,
                            definitions = meaning.examples?.map { example ->
                                Definition(
                                    definition = meaning.definition,
                                    example = example
                                )
                            } ?: listOf(Definition(definition = meaning.definition)),
                            synonyms = meaning.synonyms,
                            antonyms = meaning.antonyms
                        )
                    }

                    if (cachedMeanings != null && cachedMeanings.isNotEmpty()) {
                        val cachedEntry = DictionaryEntry(
                            word = word,
                            phonetic = cachedWord.phonetic,
                            phonetics = listOf(Phonetic(text = cachedWord.phonetic, audio = cachedWord.audioUrl)),
                            meanings = cachedMeanings
                        )

                        emit(NetworkResult.Success(Pair(cachedWord.translation, listOf(cachedEntry))))
                        return@flow
                    } else {
                        // Nếu không có meanings trong DB, trả về dữ liệu cơ bản
                        emit(NetworkResult.Success(Pair(cachedWord.translation, emptyList())))
                        return@flow
                    }
                }

                // Nếu có internet, tiến hành tra cứu API nhưng đã có dữ liệu trong DB
                var translation = cachedWord.translation

                // Thử lấy thông tin chi tiết từ API
                try {
                    val dictionaryEntries = freeDictionaryApiService.getWordDefinition(word)

                    // Cập nhật word vào DB với thông tin mới từ API
                    updateWordWithApiData(word, translation, dictionaryEntries)

                    emit(NetworkResult.Success(Pair(translation, dictionaryEntries)))
                } catch (e: Exception) {
                    // Nếu không thể lấy được từ API, sử dụng dữ liệu từ DB
                    Log.e(TAG, "Error fetching word details from API: ${e.message}")

                    // Tùy thuộc vào lỗi, có thể trả về dữ liệu từ DB hoặc báo lỗi
                    if (e is HttpException && e.code() == 404) {
                        emit(NetworkResult.Error("Không tìm thấy thông tin chi tiết cho từ này",
                            Pair(translation, emptyList())))
                    } else {
                        emit(NetworkResult.Success(Pair(translation, emptyList())))
                    }
                }
            } else {
                // Từ không có trong DB
                if (!isNetworkAvailable) {
                    emit(NetworkResult.Error("Không có kết nối internet và từ chưa được lưu trong cơ sở dữ liệu"))
                    return@flow
                }

                // Dịch từ tiếng Anh sang tiếng Việt
                val apiKey = preferencesManager.getRapidApiKey().ifEmpty { BuildConfig.RAPID_API_KEY }
                var translation = ""

                try {
                    val request = RapidApiTranslationRequest(
                        from = "en",
                        to = "vi",
                        json = TranslationJson(text = word)
                    )

                    val response = rapidApiTranslateService.translate(apiKey, request)
                    val jsonResponse = gson.toJsonTree(response).asJsonObject

                    translation = when {
                        jsonResponse.has("trans") && jsonResponse.get("trans").isJsonObject -> {
                            val transObject = jsonResponse.getAsJsonObject("trans")
                            if (transObject.has("text")) {
                                transObject.get("text").asString
                            } else {
                                "Không thể dịch từ này"
                            }
                        }
                        jsonResponse.has("text") -> {
                            jsonResponse.get("text").asString
                        }
                        else -> {
                            "Không thể dịch từ này"
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Translation API error", e)
                    translation = "Không thể dịch từ này"
                }

                // Lấy thông tin chi tiết từ Free Dictionary API
                try {
                    val dictionaryEntries = freeDictionaryApiService.getWordDefinition(word)

                    // Lưu từ mới vào cơ sở dữ liệu
                    updateWordWithApiData(word, translation, dictionaryEntries)

                    emit(NetworkResult.Success(Pair(translation, dictionaryEntries)))
                } catch (e: HttpException) {
                    if (e.code() == 404) {
                        // Xử lý lỗi 404 - Không tìm thấy từ
                        val errorBody = e.response()?.errorBody()?.string()
                        val dictionaryError = gson.fromJson(errorBody, com.example.dictionary.data.model.DictionaryError::class.java)
                        val errorMessage = dictionaryError?.message ?: "Không tìm thấy thông tin cho từ này"

                        // Vẫn lưu từ với thông tin cơ bản
                        val basicWord = Word(
                            word = word,
                            translation = translation
                        )
                        wordDao.insert(basicWord)

                        emit(NetworkResult.Error(errorMessage, Pair(translation, emptyList())))
                    } else {
                        // Xử lý các lỗi HTTP khác
                        emit(NetworkResult.Error("Lỗi khi tra cứu từ: ${e.message}", Pair(translation, emptyList())))
                    }
                } catch (e: Exception) {
                    // Xử lý các lỗi khác
                    emit(NetworkResult.Error("Lỗi khi tra cứu từ: ${e.message}", Pair(translation, emptyList())))
                }
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error("Lỗi tra cứu từ: ${e.message}"))
        }
    }

    private suspend fun updateWordWithApiData(word: String, translation: String, entries: List<DictionaryEntry>) {
        if (entries.isEmpty()) return

        val entry = entries[0]

        // Tìm audio URL từ phonetics
        var audioUrl: String? = null
        entry.phonetics?.forEach { phonetic ->
            if (!phonetic.audio.isNullOrEmpty()) {
                audioUrl = phonetic.audio
                return@forEach
            }
        }

        // Chuyển đổi meanings từ API sang định dạng của DB
        val meanings = entry.meanings?.map { meaning ->
            // Lấy definition đầu tiên và các ví dụ từ tất cả definitions
            val firstDefinition = meaning.definitions?.firstOrNull()
            val exampleTexts = meaning.definitions
                ?.filter { it.example != null }
                ?.mapNotNull { it.example }

            Meaning(
                partOfSpeech = meaning.partOfSpeech ?: "",
                definition = firstDefinition?.definition ?: "",
                examples = exampleTexts,
                synonyms = meaning.synonyms,
                antonyms = meaning.antonyms
            )
        }

        // Lấy danh sách ví dụ
        val examples = entry.meanings
            ?.flatMap { meaning ->
                meaning.definitions
                    ?.filter { it.example != null }
                    ?.map {
                        Example(
                            text = it.example ?: "",
                            translation = null // Có thể dịch ví dụ trong tương lai
                        )
                    } ?: emptyList()
            }

        // Chuyển đổi các đối tượng thành chuỗi JSON
        val meaningsJson = if (meanings != null) gson.toJson(meanings) else null
        val synonymsJson = entry.meanings?.flatMap { it.synonyms ?: emptyList() }?.distinct()?.let { gson.toJson(it) }
        val antonymsJson = entry.meanings?.flatMap { it.antonyms ?: emptyList() }?.distinct()?.let { gson.toJson(it) }
        val examplesJson = if (examples != null) gson.toJson(examples) else null

        // Tạo đối tượng Word với đầy đủ thông tin
        val updatedWord = Word(
            word = word,
            translation = translation,
            phonetic = entry.phonetic ?: entry.phonetics?.firstOrNull()?.text ?: "",
            meaningsJson = meaningsJson,
            examplesJson = examplesJson,
            audioUrl = audioUrl,
            partOfSpeech = entry.meanings?.firstOrNull()?.partOfSpeech,
            definition = entry.meanings?.firstOrNull()?.definitions?.firstOrNull()?.definition,
            synonymsJson = synonymsJson,
            antonymsJson = antonymsJson
        )

        // Lưu hoặc cập nhật từ trong DB
        wordDao.insertOrUpdateWord(updatedWord)
    }

    // Hàm chuyển đổi JSON thành đối tượng
    private fun parseJsonToMeanings(json: String?): List<Meaning>? {
        if (json.isNullOrEmpty()) return null
        val type = object : TypeToken<List<Meaning>>() {}.type
        return gson.fromJson(json, type)
    }

    private fun parseJsonToStringList(json: String?): List<String>? {
        if (json.isNullOrEmpty()) return null
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type)
    }

    private fun parseJsonToExamples(json: String?): List<Example>? {
        if (json.isNullOrEmpty()) return null
        val type = object : TypeToken<List<Example>>() {}.type
        return gson.fromJson(json, type)
    }

    suspend fun translateText(text: String, sourceLanguage: String, targetLanguage: String): Flow<NetworkResult<String>> = flow {
        emit(NetworkResult.Loading())

        try {
            // Kiểm tra kết nối internet
            val isNetworkAvailable = networkUtils.isNetworkAvailable()

            // Kiểm tra xem văn bản đã được dịch và lưu trữ chưa
            val cachedText = dicTextDao.getDicText(text, sourceLanguage, targetLanguage)

            if (cachedText != null) {
                // Cập nhật thông tin truy cập
                dicTextDao.updateAccessInfo(cachedText.id)

                // Nếu không có internet, sử dụng dữ liệu đã lưu
                if (!isNetworkAvailable) {
                    emit(NetworkResult.Success(cachedText.translatedText))
                    return@flow
                }

                // Nếu là từ đơn, có thể xem xét tra cứu lại để cập nhật
                if (!text.contains(" ") && sourceLanguage == "en" && targetLanguage == "vi") {
                    // Kiểm tra nếu đã lưu trong bảng word
                    val cachedWord = wordDao.getWord(text)
                    if (cachedWord != null) {
                        emit(NetworkResult.Success(cachedWord.translation))
                        return@flow
                    }
                }

                // Đối với văn bản dài, chúng ta sử dụng bản dịch đã lưu
                emit(NetworkResult.Success(cachedText.translatedText))
                return@flow
            }

            // Nếu không có dữ liệu đã lưu và không có internet
            if (!isNetworkAvailable) {
                emit(NetworkResult.Error("Không có kết nối internet và văn bản chưa được lưu trong cơ sở dữ liệu"))
                return@flow
            }

            // Dịch văn bản bằng API
            val apiKey = preferencesManager.getRapidApiKey().ifEmpty { BuildConfig.RAPID_API_KEY }
            if (apiKey.isEmpty()) {
                emit(NetworkResult.Error("API key chưa được cấu hình. Vui lòng thêm API key trong phần Cài đặt."))
                return@flow
            }

            // Chuẩn bị request
            val request = RapidApiTranslationRequest(
                from = if (sourceLanguage == "auto") "auto" else sourceLanguage,
                to = targetLanguage,
                json = TranslationJson(text = text)
            )

            try {
                // Gọi API
                val response = rapidApiTranslateService.translate(apiKey, request)

                // Parse response
                val jsonResponse = gson.toJsonTree(response).asJsonObject
                val translatedText = when {
                    // Kiểm tra cấu trúc {"trans":{"text":"Hello"}}
                    jsonResponse.has("trans") && jsonResponse.get("trans").isJsonObject -> {
                        val transObject = jsonResponse.getAsJsonObject("trans")
                        if (transObject.has("text")) {
                            transObject.get("text").asString
                        } else {
                            throw Exception("Không tìm thấy trường 'text' trong object 'trans'")
                        }
                    }
                    // Kiểm tra cấu trúc {"text":"Hello"}
                    jsonResponse.has("text") -> {
                        jsonResponse.get("text").asString
                    }
                    else -> {
                        throw Exception("Không thể tìm thấy văn bản đã dịch trong phản hồi")
                    }
                }

                // Lưu văn bản đã dịch
                if (!text.contains(" ") && sourceLanguage == "en" && targetLanguage == "vi") {
                    // Nếu là từ đơn, lưu vào bảng Word
                    val word = Word(
                        word = text,
                        translation = translatedText
                    )
                    wordDao.insert(word)
                } else {
                    // Nếu là văn bản dài, lưu vào bảng DicText
                    val dicText = DicText(
                        sourceText = text,
                        translatedText = translatedText,
                        sourceLanguage = sourceLanguage,
                        targetLanguage = targetLanguage
                    )
                    dicTextDao.insertOrUpdateDicText(dicText)
                }

                emit(NetworkResult.Success(translatedText))
            } catch (e: Exception) {
                Log.e(TAG, "Translation API error", e)
                emit(NetworkResult.Error("Lỗi dịch: ${e.message}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Translation failed", e)
            emit(NetworkResult.Error("Dịch thất bại: ${e.message}"))
        }
    }

    suspend fun toggleFavorite(word: String): Boolean {
        val currentWord = wordDao.getWord(word)
        val newFavoriteStatus = !(currentWord?.isFavorite ?: false)
        wordDao.updateFavorite(word, newFavoriteStatus)

        if (newFavoriteStatus) {
            // Nếu đánh dấu yêu thích, thêm vào bảng favorite_word
            favoriteWordDao.insert(FavoriteWord(word = word))
        } else {
            // Nếu bỏ đánh dấu yêu thích, xóa khỏi bảng favorite_word
            favoriteWordDao.deleteFavoriteWord(word)
        }

        return newFavoriteStatus
    }

    suspend fun getWordFavoriteStatus(word: String): Boolean {
        return wordDao.getWord(word)?.isFavorite ?: false
    }

    suspend fun updateFavoriteNote(word: String, note: String?) {
        favoriteWordDao.updateNote(word, note)
    }

    fun getSearchHistory(): Flow<List<Word>> {
        return wordDao.getAllWords()
    }

    fun getFavoriteWords(): Flow<List<WordWithFavorite>> {
        return favoriteWordDao.getFavoriteWordsWithDetails()
    }

    suspend fun deleteSearchHistory(words: List<String>) {
        wordDao.deleteWords(words)
    }

    suspend fun deleteAllSearchHistory() {
        wordDao.deleteAllWords()
    }

    suspend fun removeFavorites(words: List<String>) {
        favoriteWordDao.deleteFavoriteWords(words)
        // Cập nhật trạng thái yêu thích trong bảng word
        for (word in words) {
            wordDao.updateFavorite(word, false)
        }
    }

    fun getTextHistory(): Flow<List<DicText>> {
        return dicTextDao.getAllDicTexts()
    }

    suspend fun deleteTextHistory(ids: List<Long>) {
        dicTextDao.deleteDicTexts(ids)
    }

    suspend fun deleteAllTextHistory() {
        dicTextDao.deleteAllDicTexts()
    }
}
