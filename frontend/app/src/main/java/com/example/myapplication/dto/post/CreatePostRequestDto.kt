package com.example.myapplication.dto.post

data class CreatePostRequestDto(
    val title: String,
    val type: String,
    val localisation: String,
    val description: String? = null,
    val minPrice: Int?,
    val maxPrice: Int?,
    val minDuration: Int?,
    val maxDuration: Int?,
    val groupId: Int? = null,
    val tags: List<String>? = null
)
