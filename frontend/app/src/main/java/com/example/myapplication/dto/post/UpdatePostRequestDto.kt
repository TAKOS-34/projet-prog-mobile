package com.example.myapplication.dto.post

data class UpdatePostRequestDto(
    val title: String? = null,
    val localisation: String? = null,
    val description: String? = null
)
