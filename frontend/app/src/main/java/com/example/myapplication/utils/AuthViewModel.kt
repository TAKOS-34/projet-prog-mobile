package com.example.myapplication.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AuthViewModel : ViewModel() {
    private val _isLoggedIn = MutableLiveData<Boolean>(false)
    val isLoggedIn: LiveData<Boolean> get() = _isLoggedIn

    init {
        checkLoginStatus()
    }

    fun isAuthenticated(): Boolean {
        return _isLoggedIn.value ?: false
    }

    fun checkLoginStatus() {
        val token = TokenManager.getToken()
        _isLoggedIn.value = !token.isNullOrBlank()
    }

    fun logout(fcmToken: String, onComplete: () -> Unit) {
        if (TokenManager.getToken().isNullOrBlank()) {
            TokenManager.clearToken()
            _isLoggedIn.postValue(false)
            onComplete()
            return
        }

        val body = """{"fcmToken":"$fcmToken"}"""
        ApiClient.delete("auth/logout", body, "application/json; charset=utf-8") { _, _, _ ->
            TokenManager.clearToken()
            _isLoggedIn.postValue(false)
            onComplete()
        }
    }
}
