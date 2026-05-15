package com.example.myapplication.dto.trip

data class TripStepDetailDto(
    val post: TripPostDto,
    val localisation: TripLocalisationDto,
    val travelTimeFromPrevious: Int,
    val travelDistanceFromPrevious: Int?,
    val isTravelTimeFromPreviousTrusted: Boolean,
    val visitDuration: Int,
    val isVisitDurationTrusted: Boolean
)
