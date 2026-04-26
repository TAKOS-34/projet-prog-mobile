package com.example.myapplication.dto.post

data class TagDto(
    val id: Int,
    val name: String,
    val nbUses: Int,
    val isPopular: Boolean,
    val isFollowing: Boolean
)
