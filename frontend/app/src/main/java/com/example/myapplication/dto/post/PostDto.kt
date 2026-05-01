package com.example.myapplication.dto.post

data class PostDto(
    val id: String,
    val image: String,
    val creationDate: String,
    val isEdited: Boolean,
    val updatedAt: String?,
    val title: String,
    val description: String?,
    val type: String,
    val audio: String?,
    val audioDuration: Int?,
    val localisation: String,
    val long: Double,
    val lat: Double,
    val nbLikes: Int,
    val nbComments: Int,
    val userId: Int,
    val username: String,
    val avatar: String,
    val groupId: Int?,
    val groupName: String?,
    val groupAvatar: String?,
    val isMember: Boolean,
    val tags: List<String>,
    val isLiked: Boolean,
    val isBookmarked: Boolean
)

data class PostsResponseDto(
    val posts: List<PostDto>,
    val nextCursor: String?
)
