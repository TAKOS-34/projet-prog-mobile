package com.example.myapplication.utils

import android.content.Context
import com.example.myapplication.R
import com.example.myapplication.dto.post.PostType

object PostMetrics {

    val PRICE_RANGES: List<Pair<Int, Int>> = listOf(
        0 to 0,
        0 to 10,
        10 to 20,
        20 to 30,
        40 to 60,
        60 to 100,
        100 to 200,
        200 to 500,
        500 to 1000
    )

    val DURATION_RANGES_MIN: List<Pair<Int, Int>> = listOf(
        0 to 15,
        15 to 30,
        30 to 45,
        45 to 60,
        60 to 120,
        120 to 240,
        240 to 480
    )

    private val PRICE_TYPES = setOf(
        PostType.HISTORIC_SITE,
        PostType.NATURAL_AREA,
        PostType.URBAN_ARCHITECTURE,
        PostType.GASTRONOMY,
        PostType.UNIQUE_STAY,
        PostType.ART_CULTURE,
        PostType.NIGHTLIFE
    )

    private val DURATION_TYPES = setOf(
        PostType.HISTORIC_SITE,
        PostType.NATURAL_AREA,
        PostType.URBAN_ARCHITECTURE,
        PostType.ART_CULTURE,
        PostType.HIDDEN_GEM
    )

    fun supportsPrice(type: PostType?): Boolean = type != null && type in PRICE_TYPES
    fun supportsDuration(type: PostType?): Boolean = type != null && type in DURATION_TYPES

    fun formatPrice(context: Context, min: Int?, max: Int?): String? {
        if (min == null || max == null) return null
        if (min == 0 && max == 0) return context.getString(R.string.price_free)
        return context.getString(R.string.price_range_format, min, max)
    }

    fun formatDuration(context: Context, min: Int?, max: Int?): String? {
        if (min == null || max == null) return null
        return if (min >= 60 && max >= 60 && min % 60 == 0 && max % 60 == 0) {
            context.getString(R.string.duration_hours_format, min / 60, max / 60)
        } else {
            context.getString(R.string.duration_minutes_format, min, max)
        }
    }
}
