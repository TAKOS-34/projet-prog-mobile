package com.example.myapplication.dto.post

data class LocalisationDto(
    val id: Int,
    val name: String,
    val long: Double,
    val lat: Double,
    val nbUses: Int,
    val isPopular: Boolean,
    val isFollowing: Boolean
)
