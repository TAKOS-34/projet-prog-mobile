package com.example.myapplication.dto.trip

data class TripStepDetailDto(
    val post: TripPostDto,
    val localisation: TripLocalisationDto,
    val travelTimeFromPrevious: Double,
    val travelDistanceFromPrevious: Int?,
    val isTravelTimeFromPreviousTrusted: Boolean,
    val visitDuration: Double?,
    val isVisitDurationTrusted: Boolean
)
