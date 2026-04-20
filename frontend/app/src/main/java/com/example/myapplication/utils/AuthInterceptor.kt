package com.example.myapplication.utils
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = TokenManager.getToken()
        val authHeader = if (token != null) "Bearer $token" else "Anonymous ${TokenManager.getOrCreateAnonymousId()}"

        val request = chain.request().newBuilder()
            .addHeader("Authorization", authHeader)
            .build()
        return chain.proceed(request)
    }
}
