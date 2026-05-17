package com.example.myapplication.utils

import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import java.io.IOException
import java.net.URLEncoder

data class LocalisationSuggestion(val name: String, val label: String, val lat: Double = 0.0, val long: Double = 0.0)

object LocalisationSuggester {
    private val client = OkHttpClient()

    fun suggest(query: String, limit: Int = 6, onResult: (List<LocalisationSuggestion>) -> Unit) {
        val q = URLEncoder.encode(query, Charsets.UTF_8.name())
        val url = "https://nominatim.openstreetmap.org/search?q=$q&format=json&limit=$limit&addressdetails=1"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "TravelApp/1.0")
            .get()
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onResult(emptyList())
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) { onResult(emptyList()); return }
                val responseBody = response.body?.string().orEmpty()
                onResult(parseSuggestions(responseBody))
            }
        })
    }

    private fun parseSuggestions(body: String): List<LocalisationSuggestion> {
        return try {
            val json = JSONArray(body)
            val results = mutableListOf<LocalisationSuggestion>()
            val seen = HashSet<String>()
            for (i in 0 until json.length()) {
                val item = json.getJSONObject(i)
                val displayName = item.optString("display_name", "").trim()
                val primaryName = displayName.split(",").firstOrNull()?.trim() ?: continue
                if (primaryName.isBlank()) continue
                val address = item.optJSONObject("address")
                val city = address?.run {
                    optString("city", "").ifEmpty {
                        optString("town", "").ifEmpty {
                            optString("village", "").ifEmpty { optString("municipality", "") }
                        }
                    }
                }?.trim().orEmpty()
                val country = address?.optString("country", "")?.trim().orEmpty()
                val parts = mutableListOf(primaryName)
                if (city.isNotBlank() && city != primaryName) parts += city
                if (country.isNotBlank()) parts += country
                val label = parts.joinToString(", ")
                val lat = item.optString("lat", "0").toDoubleOrNull() ?: 0.0
                val lon = item.optString("lon", "0").toDoubleOrNull() ?: 0.0
                if (seen.add(label)) results += LocalisationSuggestion(primaryName, label, lat, lon)
            }
            results
        } catch (e: Exception) {
            emptyList()
        }
    }
}
