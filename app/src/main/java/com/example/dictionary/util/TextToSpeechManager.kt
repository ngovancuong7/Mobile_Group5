package com.example.dictionary.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.widget.Toast
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TextToSpeechManager @Inject constructor(
    private val context: Context
) {
    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false

    init {
        initTextToSpeech()
    }

    private fun initTextToSpeech() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
            } else {
                Toast.makeText(context, "Không thể khởi tạo Text-to-Speech", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun speak(text: String, language: String = "en") {
        if (!isInitialized) {
            Toast.makeText(context, "Text-to-Speech đang khởi tạo, vui lòng thử lại sau", Toast.LENGTH_SHORT).show()
            return
        }

        val locale = when (language) {
            "en" -> Locale.US
            "vi" -> Locale("vi", "VN")
            else -> Locale.US
        }

        val result = textToSpeech?.setLanguage(locale)

        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Toast.makeText(context, "Ngôn ngữ không được hỗ trợ", Toast.LENGTH_SHORT).show()
            return
        }

        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts1")
    }

    fun shutdown() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        isInitialized = false
    }
}
