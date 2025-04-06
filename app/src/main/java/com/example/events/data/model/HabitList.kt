package com.example.events.data.model

data class HabitList(
    val id: Int,
    val title: String,
    val description: String? = null,
    val items: List<HabitItem>
)

