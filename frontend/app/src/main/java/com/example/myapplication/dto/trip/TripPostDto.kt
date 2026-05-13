package com.example.myapplication.dto.trip

data class TripPostDto(
    val id: String,
    val image: String,
    val imageExt: String?,
    val title: String,
    val description: String?,
    val type: String,
    val minPrice: Int?,
    val maxPrice: Int?,
    val minDuration: Int?,
    val maxDuration: Int?,
    val nbLikes: Int,
    val nbComments: Int,
    val localisationId: Int,
    val userId: Int
)
