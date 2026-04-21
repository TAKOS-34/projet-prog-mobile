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
import androidx.navigation.fragment.findNavController
import com.example.myapplication.R
import com.example.myapplication.dto.auth.RegisterRequestDto
import com.example.myapplication.utils.ApiClient
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class RegisterFragment : Fragment() {
    private lateinit var tilLogin: TextInputLayout
    private lateinit var etLogin: TextInputEditText
    private lateinit var tilEmail: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etPassword: TextInputEditText
    private lateinit var tilConfirm: TextInputLayout
    private lateinit var etConfirm: TextInputEditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_register, container, false)

        initViews(view)

        view.findViewById<Button>(R.id.btnRegister).setOnClickListener {
            if (validateFields()) {
                performRegister()
            }
        }

        view.findViewById<ImageView>(R.id.btnClose).setOnClickListener {
            findNavController().navigateUp()
        }

        view.findViewById<TextView>(R.id.tvGoToLogin).setOnClickListener {
            findNavController().navigateUp()
        }

        return view
    }

    private fun initViews(view: View) {
        tilLogin = view.findViewById(R.id.tilLogin)
        etLogin = view.findViewById(R.id.etLogin)
        tilEmail = view.findViewById(R.id.tilEmail)
        etEmail = view.findViewById(R.id.etEmail)
        tilPassword = view.findViewById(R.id.tilPassword)
        etPassword = view.findViewById(R.id.etPassword)
        tilConfirm = view.findViewById(R.id.tilConfirm)
        etConfirm = view.findViewById(R.id.etConfirm)
    }

    private fun validateFields(): Boolean {
        var isValid = true

        val username = etLogin.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()
        val confirm = etConfirm.text.toString()

        if (username.length !in 4..32) {
            tilLogin.error = getString(R.string.error_invalid_username)
            isValid = false
        } else {
            tilLogin.error = null
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = getString(R.string.error_invalid_email)
            isValid = false
        } else {
            tilEmail.error = null
        }

        if (password.length < 6 || !password.any { it.isDigit() }) {
            tilPassword.error = getString(R.string.error_invalid_password)
            isValid = false
        } else {
            tilPassword.error = null
        }

        if (confirm != password) {
            tilConfirm.error = getString(R.string.error_password_mismatch)
            isValid = false
        } else {
            tilConfirm.error = null
        }

        return isValid
    }

    private fun performRegister() {
        val registerData = RegisterRequestDto(
            username = etLogin.text.toString().trim(),
            email = etEmail.text.toString().trim(),
            password = etPassword.text.toString()
        )

        view?.findViewById<Button>(R.id.btnRegister)?.isEnabled = false

        ApiClient.post("auth/signup", registerData) { body, code, error ->
            activity?.runOnUiThread {
                view?.findViewById<Button>(R.id.btnRegister)?.isEnabled = true
                handleRegisterResponse(body, code, error)
            }
        }
    }

    private fun handleRegisterResponse(body: String?, code: Int, error: String?) {
        if (error == null && body != null) {
            Toast.makeText(context, R.string.success_register, Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.loginFragment)
        } else {
            val messageRes = when (code) {
                400 -> R.string.error_signup_failed
                0 -> R.string.error_network
                else -> R.string.error_global
            }
            Toast.makeText(context, messageRes, Toast.LENGTH_LONG).show()
        }
    }
}