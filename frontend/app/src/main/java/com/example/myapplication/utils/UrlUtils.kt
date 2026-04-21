package com.example.myapplication.utils

import com.example.myapplication.BuildConfig

fun String.resolveBackendUrl(): String {
    val backend = BuildConfig.BACKEND_URL.trimEnd('/')
    return this.replace(Regex("^https?://[^/]+"), backend)
}
