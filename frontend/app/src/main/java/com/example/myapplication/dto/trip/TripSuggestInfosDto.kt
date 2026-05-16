package com.example.myapplication.dto.trip

data class TripSuggestInfosDto(
    val id: Int?,
    val steps: List<TripStepDetailDto>,
    val totalDuration: Double,
    val totalCost: Int,
    val totalStep: Int,
    val weather: String,
    val difficulty: Int?,
    val totalAscent: Int?
)
