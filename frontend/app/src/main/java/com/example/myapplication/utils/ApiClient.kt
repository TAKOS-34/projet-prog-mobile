package com.example.myapplication.utils

import android.util.Log
import com.example.myapplication.BuildConfig
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Request as OkHttpRequest
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

object ApiClient {
    private const val TAG = "API_DEBUG"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor())
            .build()
    }
    
    private val gson = Gson()

    fun post(path: String, dto: Any, onResult: (String?, Int, String?) -> Unit) {
        val json = gson.toJson(dto)
        val url = "${BuildConfig.BACKEND_URL}$path"
        val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())

        Log.d(TAG, ">>> EXECUTING POST $url")

        val request = OkHttpRequest.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).enqueue(createCallback(url, onResult))
    }

    fun patch(path: String, dto: Any, onResult: (String?, Int, String?) -> Unit) {
        val json = gson.toJson(dto)
        val url = "${BuildConfig.BACKEND_URL}$path"
        val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())

        Log.d(TAG, ">>> EXECUTING PATCH $url")

        val request = OkHttpRequest.Builder()
            .url(url)
            .patch(body)
            .build()

        client.newCall(request).enqueue(createCallback(url, onResult))
    }

    fun get(path: String, onResult: (String?, Int, String?) -> Unit) {
        val url = "${BuildConfig.BACKEND_URL}$path"
        Log.d(TAG, ">>> EXECUTING GET $url")

        val request = OkHttpRequest.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(createCallback(url, onResult))
    }

    fun delete(path: String, bodyString: String, mediaType: String, onResult: (String?, Int, String?) -> Unit) {
        val url = "${BuildConfig.BACKEND_URL}$path"
        val body = bodyString.toRequestBody(mediaType.toMediaType())

        Log.d(TAG, ">>> EXECUTING DELETE $url")

        val request = OkHttpRequest.Builder()
            .url(url)
            .delete(body)
            .build()

        client.newCall(request).enqueue(createCallback(url, onResult))
    }

    fun delete(path: String, onResult: (String?, Int, String?) -> Unit) {
        val url = "${BuildConfig.BACKEND_URL}$path"
        Log.d(TAG, ">>> EXECUTING DELETE $url")

        val request = OkHttpRequest.Builder()
            .url(url)
            .delete()
            .build()

        client.newCall(request).enqueue(createCallback(url, onResult))
    }

    fun patchMultipart(
        path: String,
        fileKey: String,
        fileName: String,
        fileBytes: ByteArray,
        fileMediaType: String,
        onResult: (String?, Int, String?) -> Unit
    ) {
        val url = "${BuildConfig.BACKEND_URL}$path"
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                fileKey,
                fileName,
                fileBytes.toRequestBody(fileMediaType.toMediaType())
            )
            .build()

        Log.d(TAG, ">>> EXECUTING PATCH MULTIPART $url")

        val request = OkHttpRequest.Builder()
            .url(url)
            .patch(body)
            .build()

        client.newCall(request).enqueue(createCallback(url, onResult))
    }

    private fun createCallback(url: String, onResult: (String?, Int, String?) -> Unit) = object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e(TAG, "<<< FAILURE (Network/Connection) $url", e)
            onResult(null, 500, "Network error: ${e.message}")
        }

        override fun onResponse(call: Call, response: Response) {
            val bodyString = response.body?.string()
            val code = response.code

            Log.d(TAG, "<<< RESPONSE RECEIVED | Code: $code | URL: $url")

            var errorMessage: String? = null
            if (!response.isSuccessful) {
                errorMessage = try {
                    val jsonErr = JSONObject(bodyString ?: "")
                    jsonErr.optString("error", jsonErr.optString("message", "Unknown error"))
                } catch (e: Exception) {
                    "Server error : $code"
                }
                Log.e(TAG, "Error Response: $errorMessage")
            }

            onResult(bodyString, code, errorMessage)
        }
    }
}
