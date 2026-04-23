package com.example.myapplication.utils

import android.content.Context
import coil.ImageLoader
import okhttp3.OkHttpClient

object ImageLoaderUtil {
    fun build(context: Context): ImageLoader {
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor())
            .build()
        return ImageLoader.Builder(context)
            .okHttpClient(client)
            .crossfade(true)
            .build()
    }
}
