package com.example.events.data.model

data class HabitItem(
    val id: Int,
    val name: String,
    val responsible: User,
    val status: String,
    val addedAt: String = "",
    val updatedAt: String = "", // Agregado updatedAt con valor predeterminado
    val description: String? = null
)