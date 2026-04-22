package com.example.myapplication.dto.post

data class CreatePostRequestDto(
    val title: String,
    val localisation: String,
    val description: String? = null,
    val groupId: Int? = null,
    val tags: List<String>? = null
)
