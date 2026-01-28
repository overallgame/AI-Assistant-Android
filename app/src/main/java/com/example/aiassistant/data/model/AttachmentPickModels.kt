package com.example.aiassistant.data.model

data class PickedImage(
    val contentUri: String,
    val mimeType: String? = null,
    val widthPx: Int? = null,
    val heightPx: Int? = null,
)

data class PickedFile(
    val contentUri: String,
    val fileName: String,
    val mimeType: String? = null,
    val sizeBytes: Long? = null,
)
