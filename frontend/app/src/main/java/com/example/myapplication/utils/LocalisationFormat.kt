package com.example.myapplication.utils

import java.util.Locale

object LocalisationFormat {
    private val locale = Locale.FRENCH

    fun display(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return raw.split(' ').joinToString(" ") { word ->
            word.split('-').joinToString("-") { part ->
                if (part.isEmpty()) part
                else part[0].titlecase(locale) + part.substring(1)
            }
        }
    }
}
