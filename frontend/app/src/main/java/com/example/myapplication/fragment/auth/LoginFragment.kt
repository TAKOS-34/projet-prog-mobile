package com.example.myapplication.fragment.auth

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.myapplication.R
import com.example.myapplication.dto.auth.LoginRequestDto
import com.example.myapplication.utils.ApiClient
import com.example.myapplication.utils.AuthViewModel
import com.example.myapplication.utils.FcmToken
import com.example.myapplication.utils.TokenManager
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.json.JSONObject

class LoginFragment : Fragment() {

    private val authViewModel: AuthViewModel by activityViewModels()
    private var currentFcmToken: String = ""

    private lateinit var tilEmail: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etPassword: TextInputEditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        tilEmail = view.findViewById(R.id.tilEmail)
        etEmail = view.findViewById(R.id.etEmail)
        tilPassword = view.findViewById(R.id.tilPassword)
        etPassword = view.findViewById(R.id.etPassword)

        FcmToken.fetchFcmToken { currentFcmToken = it }

        view.findViewById<Button>(R.id.btnLogin).setOnClickListener {
            if (validateFields()) {
                performLogin()
            }
        }

        view.findViewById<ImageView>(R.id.btnClose).setOnClickListener {
            findNavController().navigateUp()
        }

        view.findViewById<TextView>(R.id.tvGoToRegister).setOnClickListener {
            findNavController().navigate(R.id.registerFragment)
        }

        return view
    }

    private fun validateFields(): Boolean {
        var isValid = true

        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()

        if (email.isEmpty()) {
            tilEmail.error = getString(R.string.error_field_required)
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = getString(R.string.error_invalid_email)
            isValid = false
        } else {
            tilEmail.error = null
        }

        if (password.isEmpty()) {
            tilPassword.error = getString(R.string.error_field_required)
            isValid = false
        } else if (password.length < 6 || !password.any { it.isDigit() }) {
            tilPassword.error = getString(R.string.error_invalid_password)
            isValid = false
        } else {
            tilPassword.error = null
        }

        return isValid
    }

    private fun performLogin() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()
        val loginData = LoginRequestDto(email, password, currentFcmToken)

        view?.findViewById<Button>(R.id.btnLogin)?.isEnabled = false

        ApiClient.post("auth/login", loginData) { body, code, error ->
            activity?.runOnUiThread {
                view?.findViewById<Button>(R.id.btnLogin)?.isEnabled = true
                handleLoginResponse(body, code, error)
            }
        }
    }

    private fun handleLoginResponse(body: String?, code: Int, error: String?) {
        if (error == null && body != null) {
            val token = JSONObject(body).optString("token")
            TokenManager.saveToken(token)
            authViewModel.checkLoginStatus()

            Toast.makeText(context, R.string.success_login, Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.nav_profile)
        } else {
            val messageRes = when (code) {
                400 -> R.string.error_login_failed
                401 -> R.string.error_login_unauthorized
                0 -> R.string.error_network
                else -> R.string.error_global
            }
            Toast.makeText(context, messageRes, Toast.LENGTH_LONG).show()
        }
    }
}