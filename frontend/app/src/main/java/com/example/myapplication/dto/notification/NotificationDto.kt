package com.example.myapplication.dto.notification

data class NotificationDto(
    val id: Int,
    val type: String,
    val creationDate: String,
    val isRead: Boolean,

    val postId: String? = null,
    val postImage: String? = null,

    val targetUserId: Int? = null,
    val targetUsername: String? = null,
    val targetUserAvatar: String? = null,

    val groupId: Int? = null,
    val groupName: String? = null,
    val groupAvatar: String? = null,

    val tagId: Int? = null,
    val tagName: String? = null,

    val localisationId: Int? = null,
    val localisationName: String? = null
)
