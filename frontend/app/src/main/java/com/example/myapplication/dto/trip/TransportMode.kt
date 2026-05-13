package com.example.myapplication.dto.trip

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.myapplication.R

enum class TransportMode(@StringRes val labelRes: Int, @DrawableRes val iconRes: Int) {
    WALK(R.string.transport_mode_walk, R.drawable.ic_transport_walk),
    CAR(R.string.transport_mode_car, R.drawable.ic_transport_car);

    companion object {
        fun fromApiValue(value: String?): TransportMode? =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
    }
}
