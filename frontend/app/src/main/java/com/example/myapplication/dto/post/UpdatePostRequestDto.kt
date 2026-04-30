package com.example.myapplication.dto.post

data class UpdatePostRequestDto(
    val title: String? = null,
    val type: String? = null,
    val description: String? = null
)
