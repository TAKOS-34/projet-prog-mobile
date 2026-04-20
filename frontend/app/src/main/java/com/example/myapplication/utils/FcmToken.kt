package com.example.myapplication.utils
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging

object FcmToken {
    fun fetchFcmToken(onSuccess: (String) -> Unit) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                onSuccess(token ?: "none")
            } else {
                onSuccess("none")
            }
        })
    }
}