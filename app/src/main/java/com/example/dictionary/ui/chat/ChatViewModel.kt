package com.example.dictionary.ui.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dictionary.data.model.ChatMessage
import com.example.dictionary.data.repository.ChatRepository
import com.example.dictionary.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    val allMessages: LiveData<List<ChatMessage>> = chatRepository.allMessages

    private val _chatResponse = MutableStateFlow<NetworkResult<String>?>(null)
    val chatResponse: StateFlow<NetworkResult<String>?> = _chatResponse

    fun sendMessage(message: String) {
        viewModelScope.launch {
            chatRepository.sendMessage(message)
                .collectLatest { result ->
                    _chatResponse.value = result
                }
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            chatRepository.clearChatHistory()
        }
    }
}
