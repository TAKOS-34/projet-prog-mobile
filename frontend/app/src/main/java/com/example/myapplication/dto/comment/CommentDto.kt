package com.example.myapplication.dto.comment

data class CommentDto(
    val id: Int,
    val content: String,
    val creationDate: String,
    val updatedAt: String?,
    val isEdited: Boolean,
    val nbLikes: Int,
    val nbReplies: Int,
    val userId: Int,
    val username: String,
    val avatar: String,
    val parentId: Int?,
    val isYours: Boolean,
    val isLiked: Boolean
)

data class CreateCommentDto(
    val content: String,
    val parentId: Int? = null
)

data class UpdateCommentDto(
    val content: String
)
