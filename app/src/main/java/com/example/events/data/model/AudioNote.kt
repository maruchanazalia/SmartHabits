package com.example.events.data.model

data class AudioNote(
    val id: Int,
    val audioFile: String,
    val title: String? = null,
    val recordedAt: String
)