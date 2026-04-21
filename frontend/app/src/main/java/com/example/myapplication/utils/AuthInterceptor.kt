package com.example.myapplication.utils

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = TokenManager.getToken()

        val authHeader = if (!token.isNullOrBlank()) token else "Anonymous ${TokenManager.getOrCreateAnonymousId()}"

        Log.d("AUTH_DEBUG", "Sending Header -> Authorization: $authHeader")

        val request = chain.request().newBuilder()
            .addHeader("Authorization", authHeader)
            .build()
            
        return chain.proceed(request)
    }
}
