package com.example.myapplication.dto.trip

data class TripFeedResponseDto(
    val trips: List<TripFeedItemDto>,
    val nextCursor: Int?
)
