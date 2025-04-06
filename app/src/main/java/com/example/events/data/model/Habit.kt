package com.example.events.data.model

data class Habit(
    val id: Int,
    val name: String,
    val description: String? = null,
    val frequency: String,
    val daysOfWeek: List<Int>? = null,
    val timeOfDay: String? = null,
    val location: Location? = null,
    val audioNote: AudioNote? = null,
    val images: List<HabitImage>? = null,
    val createdAt: String,
    val updatedAt: String,
    val habitList: HabitList
)

