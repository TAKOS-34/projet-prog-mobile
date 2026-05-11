package com.example.myapplication.dto.post

data class UpdatePostRequestDto(
    val title: String? = null,
    val type: String? = null,
    val minPrice: Int?,
    val maxPrice: Int?,
    val minDuration: Int?,
    val maxDuration: Int?,
    val description: String? = null
)
