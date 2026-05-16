package com.example.myapplication.utils

import android.content.Context
import com.example.myapplication.R

fun String.toWeatherLabel(context: Context): String = when (this) {
    "CLEAR" -> context.getString(R.string.weather_clear)
    "CLOUDY" -> context.getString(R.string.weather_cloudy)
    "RAIN" -> context.getString(R.string.weather_rain)
    "SNOW" -> context.getString(R.string.weather_snow)
    "STORM" -> context.getString(R.string.weather_storm)
    else -> this
}

fun String.toWeatherEmoji(): String = when (this) {
    "CLEAR" -> "☀️"
    "CLOUDY" -> "☁️"
    "RAIN" -> "🌧️"
    "SNOW" -> "❄️"
    "STORM" -> "⛈️"
    else -> "🌡️"
}

fun Number.toTripDuration(context: Context): String {
    val total = this.toInt()
    val h = total / 60
    val m = total % 60
    return if (h > 0) context.getString(R.string.trip_total_duration_hm, h, m)
    else context.getString(R.string.trip_total_duration_m, m)
}
