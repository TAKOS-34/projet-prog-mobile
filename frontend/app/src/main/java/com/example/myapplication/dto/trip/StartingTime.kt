package com.example.myapplication.dto.trip

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.myapplication.R

enum class StartingTime(@StringRes val labelRes: Int, @DrawableRes val iconRes: Int) {
    MORNING(R.string.starting_time_morning, R.drawable.ic_time_morning),
    MIDDAY(R.string.starting_time_midday, R.drawable.ic_time_midday),
    AFTERNOON(R.string.starting_time_afternoon, R.drawable.ic_time_afternoon),
    EVENING(R.string.starting_time_evening, R.drawable.ic_time_evening);

    companion object {
        fun fromApiValue(value: String?): StartingTime? =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
    }
}
