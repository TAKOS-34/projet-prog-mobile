package com.example.myapplication.dto.trip

data class TripFeedItemDto(
    val id: Int,
    val startLocalisation: TripLocalisationDto?,
    val creationDate: String,
    val category: String,
    val startingTime: String,
    val transportMode: String,
    val totalDuration: Int,
    val totalCost: Int,
    val totalStep: Int,
    val totalDistance: Int?,
    val weather: String,
    val difficulty: Int?,
    val totalAscent: Int?,
    val nbLikes: Int,
    val nbBookmarks: Int,
    val isLiked: Boolean,
    val isBookmarked: Boolean,
    val isYours: Boolean,
    val userId: Int,
    val username: String,
    val avatar: String,
    val steps: List<TripStepDetailDto>
)
