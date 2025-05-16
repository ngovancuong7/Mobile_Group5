package com.example.dictionary

import android.app.Application
import android.util.Log
import com.example.dictionary.util.TextToSpeechManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class DictionaryApplication : Application() {

    @Inject
    lateinit var textToSpeechManager: TextToSpeechManager

    override fun onCreate() {
        super.onCreate()

        // Xử lý lỗi không bắt được
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("DictionaryApp", "Uncaught exception in thread $thread", throwable)
            // Có thể thêm code để lưu log lỗi vào file hoặc gửi về server
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        textToSpeechManager.shutdown()
    }
}
