package com.example.myapplication
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.activity.ComponentActivity
import com.example.myapplication.dto.auth.LoginRequestDto
import com.example.myapplication.utils.ApiClient
import com.example.myapplication.utils.FcmToken.fetchFcmToken
import com.example.myapplication.utils.TokenManager
import org.json.JSONObject

class MainActivity : ComponentActivity() {

    private var currentFcmToken: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        TokenManager.init(this)
        fetchFcmToken { currentFcmToken = it }

        setContentView(R.layout.fragment_login)

        findViewById<Button>(R.id.btnLogin).setOnClickListener {
            val loginData = LoginRequestDto(
                email = findViewById<EditText>(R.id.etEmail).text.toString(),
                password = findViewById<EditText>(R.id.etPassword).text.toString(),
                fcmToken = currentFcmToken
            )

            ApiClient.post("auth/login", loginData) { body, code, error ->
                runOnUiThread {
                    if (error == null && body != null) {
                        val token = JSONObject(body).optString("token")
                        TokenManager.saveToken(token)
                        Log.d("Login", "Success")
                    } else {
                        Log.e("Login", "Error $code: $error")
                    }
                }
            }
        }
    }
}
