package com.example.myapplication.dto.group

data class GroupInfosDto(
    val id: Int,
    val name: String,
    val avatar: String,
    val description: String? = null,
    val creationDate: String,
    val isGroupPrivate: Boolean,
    val nbMembers: Int,
    val nbPosts: Int,
    val isMember: Boolean,
    val isAdmin: Boolean,
    val isFollowing: Boolean? = null
)
