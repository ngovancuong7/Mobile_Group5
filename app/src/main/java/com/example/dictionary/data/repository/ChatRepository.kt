package com.example.dictionary.data.repository

import androidx.lifecycle.LiveData
import com.example.dictionary.BuildConfig
import com.example.dictionary.data.api.ChatGptApiService
import com.example.dictionary.data.db.ChatMessageDao
import com.example.dictionary.data.model.ChatMessage
import com.example.dictionary.util.NetworkResult
import com.example.dictionary.util.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val chatGptApiService: ChatGptApiService,
    private val chatMessageDao: ChatMessageDao,
    private val preferencesManager: PreferencesManager
) {
    val allMessages: LiveData<List<ChatMessage>> = chatMessageDao.getAllMessages()

    suspend fun sendMessage(message: String): Flow<NetworkResult<String>> = flow {
        emit(NetworkResult.Loading())

        try {
            // Lưu tin nhắn người dùng vào cơ sở dữ liệu
            val userMessage = ChatMessage(
                message = message,
                isUser = true
            )
            chatMessageDao.insert(userMessage)

            // Lấy API key từ preferences hoặc BuildConfig
            val apiKey = preferencesManager.getRapidApiKey().ifEmpty { BuildConfig.RAPID_API_KEY }

            // Gửi tin nhắn đến ChatGPT API
            val response = chatGptApiService.sendMessage(
                apiKey = apiKey,
                prompt = message
            )

            // Kiểm tra trạng thái phản hồi
            if (response.status == "success") {
                val aiResponse = response.message

                // Lưu phản hồi AI vào cơ sở dữ liệu
                val aiMessage = ChatMessage(
                    message = aiResponse,
                    isUser = false
                )
                chatMessageDao.insert(aiMessage)

                emit(NetworkResult.Success(aiResponse))
            } else {
                emit(NetworkResult.Error("Lỗi từ API: ${response.status}"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error("Không thể nhận phản hồi: ${e.message}"))
        }
    }

    suspend fun clearChatHistory() {
        chatMessageDao.clearAllMessages()
    }
}
