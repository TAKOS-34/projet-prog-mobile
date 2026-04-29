package com.example.myapplication.utils
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.myapplication.R

fun Fragment.requestToJoinGroup(groupId: Int, onSuccess: (() -> Unit)? = null) {
    ApiClient.post("group/request-to-join/$groupId", emptyMap<String, String>()) { res, code, error ->
        activity?.runOnUiThread {
            val msg = when {
                res == """{"status":true,"message":"Group join"}""" -> R.string.success_join_request
                res == """{"status":true,"message":"A request to join the group has been sent"}""" -> R.string.ask_join_request
                code == 401 -> R.string.error_join_group_banned
                else -> R.string.error_join_group
            }
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            if (error == null) {
                onSuccess?.invoke()
            }
        }
    }
}
