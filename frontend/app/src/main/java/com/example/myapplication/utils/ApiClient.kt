package com.example.myapplication.utils
import com.example.myapplication.BuildConfig
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Request as OkHttpRequest
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

object ApiClient {
    private val client = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor())
        .build()
    private val gson = Gson()

    fun post(path: String, dto: Any, onResult: (String?, Int, String?) -> Unit) {
        val json = gson.toJson(dto)
        val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = OkHttpRequest.Builder()
            .url("${BuildConfig.BACKEND_URL}$path")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onResult(null, 500, "Network error")
            }

            override fun onResponse(call: Call, response: Response) {
                val bodyString = response.body?.string()

                var errorMessage: String? = null
                if (!response.isSuccessful) {
                    errorMessage = try {
                        val jsonErr = JSONObject(bodyString ?: "")
                        jsonErr.optString("error", jsonErr.optString("message", "Unknow error"))
                    } catch (e: Exception) {
                        "Server error : ${response.code}"
                    }
                }

                onResult(bodyString, response.code, errorMessage)
            }
        })
    }
}
