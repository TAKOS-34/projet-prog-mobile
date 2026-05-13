package com.example.myapplication.dto.trip

data class TripLocalisationDto(
    val id: Int,
    val name: String,
    val long: Double,
    val lat: Double,
    val nbUses: Int
)
