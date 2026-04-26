package com.example.myapplication.dto.group

data class GroupCardInfosDto(
    val id: Int,
    val name: String,
    val avatar: String,
    val description: String? = null,
    val creationDate: String,
    val isGroupPrivate: Boolean,
    val nbMembers: Int,
    val nbPosts: Int,
    val isAdmin: Boolean,
    val isFollowing: Boolean? = null
)
