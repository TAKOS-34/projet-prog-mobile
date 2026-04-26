package com.example.myapplication.dto.profile

data class UserPublicProfileDto(
    val id: Int,
    val username: String,
    val creationDate: String,
    val avatar: String,
    val nbGroups: Int,
    val nbPosts: Int,
    val isFollowing: Boolean
)
