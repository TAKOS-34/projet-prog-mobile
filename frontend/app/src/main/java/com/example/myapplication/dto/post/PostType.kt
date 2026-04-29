package com.example.myapplication.dto.post

import androidx.annotation.StringRes
import com.example.myapplication.R

enum class PostType(@StringRes val labelRes: Int) {
    PANORAMA(R.string.post_type_panorama),
    HISTORIC_SITE(R.string.post_type_historic_site),
    NATURAL_AREA(R.string.post_type_natural_area),
    COASTAL_WATER(R.string.post_type_coastal_water),
    URBAN_ARCHITECTURE(R.string.post_type_urban_architecture),
    GASTRONOMY(R.string.post_type_gastronomy),
    UNIQUE_STAY(R.string.post_type_unique_stay),
    ART_CULTURE(R.string.post_type_art_culture),
    NIGHTLIFE(R.string.post_type_nightlife),
    HIDDEN_GEM(R.string.post_type_hidden_gem),
    OTHER(R.string.post_type_other);

    companion object {
        fun fromApiValue(value: String?): PostType? =
            PostType.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
    }
}
