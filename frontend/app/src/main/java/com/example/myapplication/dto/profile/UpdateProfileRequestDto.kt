package com.example.myapplication.dto.profile

data class UpdateProfileRequestDto(
    val email: String? = null,
    val username: String? = null,
    val password: String? = null
)
