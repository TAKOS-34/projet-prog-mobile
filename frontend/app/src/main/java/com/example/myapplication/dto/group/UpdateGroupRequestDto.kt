package com.example.myapplication.dto.group

data class UpdateGroupRequestDto(
    val name: String? = null,
    val isGroupPrivate: Boolean? = null,
    val description: String? = null
)
