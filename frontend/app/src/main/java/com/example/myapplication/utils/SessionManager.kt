package com.example.myapplication.utils
import android.content.Context
import android.content.SharedPreferences
import java.util.UUID
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SessionManager {
    private const val PREFS_NAME = "secure_prefs"
    private const val KEY_TOKEN = "bearer_token"
    private const val KEY_ANONYMOUS_ID = "anonymous_id"
    private const val KEY_USER_ID = "user_id"
    private lateinit var sharedPreferences: SharedPreferences

    fun init(context: Context) {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        sharedPreferences = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveToken(token: String) = sharedPreferences.edit { putString(KEY_TOKEN, token) }
    fun getToken(): String? = sharedPreferences.getString(KEY_TOKEN, null)

    fun saveUserId(userId: Int) = sharedPreferences.edit { putInt(KEY_USER_ID, userId) }
    fun getUserId(): Int? = sharedPreferences.getInt(KEY_USER_ID, -1).takeIf { it != -1 }

    fun getOrCreateAnonymousId(): String {
        var id = sharedPreferences.getString(KEY_ANONYMOUS_ID, null)
        if (id == null) {
            id = UUID.randomUUID().toString()
            sharedPreferences.edit { putString(KEY_ANONYMOUS_ID, id) }
        }
        return id
    }

    fun clearToken() = sharedPreferences.edit {
        remove(KEY_TOKEN)
        remove(KEY_USER_ID)
    }
}