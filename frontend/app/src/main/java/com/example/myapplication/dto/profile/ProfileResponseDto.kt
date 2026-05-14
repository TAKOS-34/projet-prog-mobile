package com.example.myapplication.dto.profile

data class ProfileResponseDto(
    val email: String,
    val username: String,
    val creationDate: String,
    val avatar: String,
    val nbGroups: Int,
    val nbPosts: Int,
    val nbFollowers: Int,
    val nbFollowing: Int
)
