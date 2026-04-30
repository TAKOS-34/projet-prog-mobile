package com.example.myapplication.dto.notification

data class FollowingDto(
    val type: String,

    val targetUserId: Int? = null,
    val targetUsername: String? = null,
    val targetUserAvatar: String? = null,

    val targetGroupId: Int? = null,
    val targetGroupName: String? = null,
    val targetGroupAvatar: String? = null,

    val targetTagId: Int? = null,
    val targetTagName: String? = null,

    val targetLocalisationId: Int? = null,
    val targetLocalisationName: String? = null
)
