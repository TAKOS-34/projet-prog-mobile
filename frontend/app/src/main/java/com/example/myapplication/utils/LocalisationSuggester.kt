package com.example.myapplication.utils

import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder

data class LocalisationSuggestion(val name: String, val label: String, val lat: Double = 0.0, val long: Double = 0.0)

object LocalisationSuggester {
    private val client = OkHttpClient()

    fun suggest(query: String, limit: Int = 6, onResult: (List<LocalisationSuggestion>) -> Unit) {
        val q = URLEncoder.encode(query, Charsets.UTF_8.name())
        val url = "https://photon.komoot.io/api/?q=$q&limit=$limit"
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onResult(emptyList())
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string().orEmpty()
                onResult(parseSuggestions(responseBody))
            }
        })
    }

    private fun parseSuggestions(body: String): List<LocalisationSuggestion> {
        return try {
            val json = JSONObject(body)
            val features = json.optJSONArray("features") ?: return emptyList()
            val results = mutableListOf<LocalisationSuggestion>()
            val seen = HashSet<String>()
            for (i in 0 until features.length()) {
                val feature = features.getJSONObject(i)
                val props = feature.optJSONObject("properties") ?: continue
                val name = props.optString("name", "").trim()
                if (name.isBlank()) continue
                val parts = mutableListOf(name)
                val city = props.optString("city", "").trim()
                if (city.isNotBlank() && city != name) parts += city
                val label = parts.joinToString(", ")
                val coords = feature.optJSONObject("geometry")?.optJSONArray("coordinates")
                val lon = coords?.optDouble(0) ?: 0.0
                val lat = coords?.optDouble(1) ?: 0.0
                if (seen.add(label)) results += LocalisationSuggestion(name, label, lat, lon)
            }
            results
        } catch (e: Exception) {
            emptyList()
        }
    }
}
