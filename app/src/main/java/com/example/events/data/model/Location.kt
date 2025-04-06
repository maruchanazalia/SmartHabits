package com.example.events.data.model

data class Location(
    val id: Int,
    val latitude: Double,
    val longitude: Double,
    val name: String? = null
)