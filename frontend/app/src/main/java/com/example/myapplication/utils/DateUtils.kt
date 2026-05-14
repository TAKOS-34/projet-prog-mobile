package com.example.myapplication.utils

import android.content.Context
import com.example.myapplication.R
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

private const val ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
private const val SHORT_DATE_FORMAT = "dd/MM/yyyy"

private fun String.parseIsoDate(): Date? {
    val sdf = SimpleDateFormat(ISO_FORMAT, Locale.ROOT)
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return runCatching { sdf.parse(this) }.getOrNull()
}

fun String.toShortDate(): String {
    val date = parseIsoDate() ?: return this
    return SimpleDateFormat(SHORT_DATE_FORMAT, Locale.getDefault()).format(date)
}

object DateUtils {

    fun formatRelativeDate(context: Context, isoString: String): String {
        val date = isoString.parseIsoDate() ?: return isoString
        val diffInMs = Date().time - date.time

        val minutes = TimeUnit.MILLISECONDS.toMinutes(diffInMs)
        val hours = TimeUnit.MILLISECONDS.toHours(diffInMs)
        val days = TimeUnit.MILLISECONDS.toDays(diffInMs)

        return when {
            minutes < 1 -> context.getString(R.string.time_just_now)
            minutes < 60 -> context.resources.getQuantityString(R.plurals.time_minutes_ago, minutes.toInt(), minutes.toInt())
            hours < 24 -> context.resources.getQuantityString(R.plurals.time_hours_ago, hours.toInt(), hours.toInt())
            days <= 3 -> context.resources.getQuantityString(R.plurals.time_days_ago, days.toInt(), days.toInt())
            else -> SimpleDateFormat(SHORT_DATE_FORMAT, Locale.getDefault()).format(date)
        }
    }

    fun formatAbsoluteDate(isoString: String): String {
        val date = isoString.parseIsoDate() ?: return isoString
        return SimpleDateFormat("dd/MM/yy,\nHH:mm", Locale.getDefault()).format(date)
    }

    fun formatMinutes(context: Context, totalMinutes: Int): String {
        return if (totalMinutes < 60) {
            context.getString(R.string.trip_total_duration_m, totalMinutes)
        } else {
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            if (minutes == 0) {
                "${hours}h"
            } else {
                context.getString(R.string.trip_total_duration_hm, hours, minutes)
            }
        }
    }
}
