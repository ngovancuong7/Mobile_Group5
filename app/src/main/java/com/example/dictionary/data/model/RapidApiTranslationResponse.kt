package com.example.dictionary.data.model

import com.google.gson.annotations.SerializedName

// Lưu ý: API trả về JSON object tương ứng với input
// Vì vậy chúng ta cần một class để xử lý response dạng text đơn giản
data class RapidApiTranslationResponse(
    @SerializedName("text")
    val text: String
)
