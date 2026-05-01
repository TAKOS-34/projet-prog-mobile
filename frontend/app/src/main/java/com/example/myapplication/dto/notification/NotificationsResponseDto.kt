package com.example.myapplication.dto.notification

data class NotificationsResponseDto(
    val notifications: List<NotificationDto>,
    val nextCursor: Int?
)
