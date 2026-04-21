package com.example.myapplication.dto.auth

data class RegisterRequestDto(
    val username: String,
    val email: String,
    val password: String
)
