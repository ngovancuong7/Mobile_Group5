package com.example.dictionary.ui.scan

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dictionary.data.repository.TranslationRepository
import com.example.dictionary.util.NetworkResult
import com.example.dictionary.util.NetworkUtils
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val translationRepository: TranslationRepository,
    @ApplicationContext private val context: Context,
    private val networkUtils: NetworkUtils
) : ViewModel() {

    private val _recognizedText = MutableStateFlow<NetworkResult<String>>(NetworkResult.Loading())
    val recognizedText: StateFlow<NetworkResult<String>> = _recognizedText

    private val _translationResult = MutableStateFlow<NetworkResult<String>>(NetworkResult.Loading())
    val translationResult: StateFlow<NetworkResult<String>> = _translationResult

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun recognizeTextFromImage(uri: Uri, contentResolver: ContentResolver) {
        _recognizedText.value = NetworkResult.Loading()

        try {
            val image = InputImage.fromFilePath(context, uri)

            textRecognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val text = visionText.text
                    if (text.isNotEmpty()) {
                        _recognizedText.value = NetworkResult.Success(text)
                    } else {
                        _recognizedText.value = NetworkResult.Error("Không phát hiện văn bản trong ảnh")
                    }
                }
                .addOnFailureListener { e ->
                    _recognizedText.value = NetworkResult.Error("Lỗi nhận dạng văn bản: ${e.message}")
                }
        } catch (e: Exception) {
            _recognizedText.value = NetworkResult.Error("Lỗi xử lý ảnh: ${e.message}")
        }
    }

    fun translateText(text: String, sourceLanguage: String, targetLanguage: String) {
        if (!networkUtils.isNetworkAvailable()) {
            _translationResult.value = NetworkResult.Error("Không có kết nối mạng. Vui lòng kiểm tra lại.")
            return
        }

        viewModelScope.launch {
            translationRepository.translateText(text, sourceLanguage, targetLanguage)
                .collectLatest { result ->
                    _translationResult.value = result
                }
        }
    }
}
