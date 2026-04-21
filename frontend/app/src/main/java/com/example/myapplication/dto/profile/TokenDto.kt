package com.example.myapplication.dto.profile

data class TokenDto(
    val id: Int,
    val creationDate: String,
    val ip: String,
    val device: String,
    val isYourSession: Boolean
)
