package com.example.myapplication.dto.auth

data class LoginRequestDto(
    val email: String,
    val password: String,
    val fcmToken: String
)