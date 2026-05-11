package com.example.myapplication.utils

import android.util.Log
import com.example.myapplication.BuildConfig
import com.google.gson.Gson
import com.google.gson.GsonBuilder
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
    private val gsonWithNulls = GsonBuilder().serializeNulls().create()

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

    fun patchMap(path: String, body: Map<String, Any?>, onResult: (String?, Int, String?) -> Unit) {
        val json = gsonWithNulls.toJson(body)
        val url = "${BuildConfig.BACKEND_URL}$path"
        val reqBody = json.toRequestBody("application/json; charset=utf-8".toMediaType())

        Log.d(TAG, ">>> EXECUTING PATCH $url")

        val request = OkHttpRequest.Builder()
            .url(url)
            .patch(reqBody)
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

    fun postMultipart(
        path: String,
        parts: Map<String, Any?>,
        imageBytes: ByteArray?,
        imageName: String?,
        imageMediaType: String?,
        audioBytes: ByteArray? = null,
        audioName: String? = null,
        audioMediaType: String? = null,
        imageFieldKey: String = "image",
        onResult: (String?, Int, String?) -> Unit
    ) {
        val url = "${BuildConfig.BACKEND_URL}$path"
        val builder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)

        parts.forEach { (key, value) ->
            if (value != null) {
                val stringValue = if (value is List<*>) gson.toJson(value) else value.toString()
                builder.addFormDataPart(key, stringValue)
            }
        }

        if (imageBytes != null && imageName != null && imageMediaType != null) {
            builder.addFormDataPart(
                imageFieldKey,
                imageName,
                imageBytes.toRequestBody(imageMediaType.toMediaType())
            )
        }

        if (audioBytes != null && audioName != null && audioMediaType != null) {
            builder.addFormDataPart(
                "audio",
                audioName,
                audioBytes.toRequestBody(audioMediaType.toMediaType())
            )
        }

        val body = builder.build()
        Log.d(TAG, ">>> EXECUTING POST MULTIPART $url")

        val request = OkHttpRequest.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).enqueue(createCallback(url, onResult))
    }

    fun getMultipart(
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

        Log.d(TAG, ">>> EXECUTING POST (IA SUGGESTIONS) $url")

        val request = OkHttpRequest.Builder()
            .url(url)
            .post(body)
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
            Log.d(TAG, "<<< RESPONSE RECEIVED | Code: $code | URL: $url | Message : ${bodyString}")
            var errorMessage: String? = null
            if (!response.isSuccessful) {
                errorMessage = try {
                    val jsonErr = JSONObject(bodyString ?: "")
                    jsonErr.optString("error", jsonErr.optString("message", "Unknown error"))
                } catch (e: Exception) {
                    "Server error : $code"
                }
            }
            onResult(bodyString, code, errorMessage)
        }
    }
}
