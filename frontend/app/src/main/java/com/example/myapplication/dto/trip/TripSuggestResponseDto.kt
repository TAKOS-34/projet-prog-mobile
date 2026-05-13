package com.example.myapplication.dto.trip

data class TripSuggestResponseDto(
    val trips: List<TripSuggestInfosDto>,
    val weather: WeatherDto
)
