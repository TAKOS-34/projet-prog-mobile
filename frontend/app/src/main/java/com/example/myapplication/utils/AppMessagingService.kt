package com.example.myapplication.utils

import com.google.firebase.messaging.FirebaseMessagingService

class AppMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        if (SessionManager.getToken().isNullOrBlank()) return
        ApiClient.patch("notification/fcm-token", mapOf("fcmToken" to token)) { _, _, _ -> }
    }
}
