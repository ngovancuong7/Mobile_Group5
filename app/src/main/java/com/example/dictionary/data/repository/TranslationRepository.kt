package com.example.dictionary.data.repository

import android.util.Log
import com.example.dictionary.BuildConfig
import com.example.dictionary.data.api.RapidApiTranslateService
import com.example.dictionary.data.db.WordDao
import com.example.dictionary.data.model.Language
import com.example.dictionary.data.model.RapidApiTranslationRequest
import com.example.dictionary.data.model.TranslationJson
import com.example.dictionary.data.model.Word
import com.example.dictionary.util.NetworkResult
import com.example.dictionary.util.PreferencesManager
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslationRepository @Inject constructor(
    private val rapidApiTranslateService: RapidApiTranslateService,
    private val wordDao: WordDao,
    private val preferencesManager: PreferencesManager,
    private val gson: Gson
) {
    private val TAG = "TranslationRepository"

    suspend fun translateText(text: String, sourceLanguage: String, targetLanguage: String): Flow<NetworkResult<String>> = flow {
        emit(NetworkResult.Loading())
        try {
            // Kiểm tra xem từ có trong cơ sở dữ liệu không (cho từ đơn)
            if (!text.contains(" ") && sourceLanguage == "en" && targetLanguage == "vi") {
                val cachedWord = wordDao.getWord(text)
                if (cachedWord != null) {
                    emit(NetworkResult.Success(cachedWord.translation))
                    return@flow
                }
            }

            // Lấy API key từ BuildConfig hoặc preferences
            val apiKey = BuildConfig.RAPID_API_KEY.ifEmpty { preferencesManager.getRapidApiKey() }
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

                // Lưu từ đơn vào cơ sở dữ liệu
                if (!text.contains(" ") && sourceLanguage == "en" && targetLanguage == "vi") {
                    val word = Word(
                        word = text,
                        translation = translatedText
                    )
                    wordDao.insert(word)
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

    suspend fun getAvailableLanguages(): Flow<NetworkResult<List<Language>>> = flow {
        emit(NetworkResult.Loading())
        try {
            // Danh sách ngôn ngữ được hỗ trợ bởi Google Translate
            val languages = listOf(
                Language("auto", "Tự động phát hiện"),
                Language("en", "English"),
                Language("vi", "Tiếng Việt"),
                Language("fr", "Français"),
                Language("de", "Deutsch"),
                Language("es", "Español"),
                Language("it", "Italiano"),
                Language("ja", "日本語"),
                Language("ko", "한국어"),
                Language("zh", "中文"),
                Language("ru", "Русский"),
                Language("ar", "العربية"),
                Language("hi", "हिन्दी"),
                Language("pt", "Português"),
                Language("tr", "Türkçe"),
                Language("nl", "Nederlands"),
                Language("pl", "Polski"),
                Language("th", "ไทย")
            )
            emit(NetworkResult.Success(languages))
        } catch (e: Exception) {
            emit(NetworkResult.Error("Không thể lấy danh sách ngôn ngữ: ${e.message}"))
        }
    }

    suspend fun toggleFavorite(word: String, isFavorite: Boolean) {
        wordDao.updateFavorite(word, isFavorite)
    }
}
