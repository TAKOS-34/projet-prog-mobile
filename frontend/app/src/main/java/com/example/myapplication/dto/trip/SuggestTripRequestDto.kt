package com.example.myapplication.dto.trip

data class SuggestTripRequestDto(
    val localisation: String,
    val maxBudget: Int,
    val maxDuration: Int,
    val startingTime: String,
    val transportMode: String,
    val preferredTypes: List<String>
)
