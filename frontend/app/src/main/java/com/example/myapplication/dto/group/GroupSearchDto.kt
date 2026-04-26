package com.example.myapplication.dto.group

data class GroupSearchDto(
    val id: Int,
    val name: String,
    val avatar: String,
    val creationDate: String,
    val isGroupPrivate: Boolean,
    val isMember: Boolean,
    val nbMembers: Int,
    val nbPosts: Int
)
