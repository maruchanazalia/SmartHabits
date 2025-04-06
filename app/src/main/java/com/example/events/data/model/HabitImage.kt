package com.example.events.data.model

data class HabitImage(
    val id: Int,
    val image: String,
    val caption: String? = null,
    val uploadedAt: String
)