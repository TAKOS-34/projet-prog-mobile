package com.example.myapplication.fragment.profile

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.myapplication.R
import com.example.myapplication.dto.profile.ProfileResponseDto
import com.example.myapplication.dto.profile.UpdateProfileRequestDto
import com.example.myapplication.utils.ApiClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson

class EditProfileFragment : Fragment() {

    private lateinit var tilUsername: TextInputLayout
    private lateinit var etUsername: TextInputEditText
    private lateinit var tilEmail: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etPassword: TextInputEditText
    private lateinit var tilConfirm: TextInputLayout
    private lateinit var etConfirm: TextInputEditText
    private lateinit var btnUpdate: MaterialButton

    private var initialUsername: String = ""
    private var initialEmail: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_edit_profile, container, false)

        initViews(view)

        view.findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            findNavController().navigateUp()
        }

        btnUpdate.setOnClickListener {
            if (validateFields()) {
                performUpdate()
            }
        }

        fetchCurrentProfile()

        return view
    }

    private fun initViews(view: View) {
        tilUsername = view.findViewById(R.id.tilLogin)
        etUsername = view.findViewById(R.id.etLogin)
        tilEmail = view.findViewById(R.id.tilEmail)
        etEmail = view.findViewById(R.id.etEmail)
        tilPassword = view.findViewById(R.id.tilPassword)
        etPassword = view.findViewById(R.id.etPassword)
        tilConfirm = view.findViewById(R.id.tilConfirmPassword)
        etConfirm = view.findViewById(R.id.etConfirmPassword)
        btnUpdate = view.findViewById(R.id.btnUpdate)
    }

    private fun fetchCurrentProfile() {
        ApiClient.get("profile") { body, _, error ->
            activity?.runOnUiThread {
                if (error == null && body != null) {
                    try {
                        val profile = Gson().fromJson(body, ProfileResponseDto::class.java)
                        initialUsername = profile.username
                        initialEmail = profile.email
                        etUsername.setText(profile.username)
                        etEmail.setText(profile.email)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun validateFields(): Boolean {
        var isValid = true

        val username = etUsername.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()
        val confirm = etConfirm.text.toString()

        if (username.isNotEmpty() && username.length !in 4..32) {
            tilUsername.error = getString(R.string.error_invalid_username)
            isValid = false
        } else {
            tilUsername.error = null
        }

        if (email.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = getString(R.string.error_invalid_email)
            isValid = false
        } else {
            tilEmail.error = null
        }

        if (password.isNotEmpty() && (password.length < 6 || !password.any { it.isDigit() })) {
            tilPassword.error = getString(R.string.error_invalid_password)
            isValid = false
        } else {
            tilPassword.error = null
        }

        if (password.isNotEmpty() && confirm != password) {
            tilConfirm.error = getString(R.string.error_password_mismatch)
            isValid = false
        } else {
            tilConfirm.error = null
        }

        return isValid
    }

    private fun performUpdate() {
        val username = etUsername.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()

        val dto = UpdateProfileRequestDto(
            username = username.takeIf { it.isNotEmpty() && it != initialUsername },
            email = email.takeIf { it.isNotEmpty() && it != initialEmail },
            password = password.takeIf { it.isNotEmpty() }
        )

        btnUpdate.isEnabled = false

        ApiClient.patch("profile/infos", dto) { _, code, error ->
            activity?.runOnUiThread {
                btnUpdate.isEnabled = true
                handleUpdateResponse(code, error)
            }
        }
    }

    private fun handleUpdateResponse(code: Int, error: String?) {
        if (error == null) {
            Toast.makeText(context, R.string.success_profile_update, Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        } else {
            val messageRes = when (code) {
                400 -> R.string.profile_update_already_taken
                0 -> R.string.error_network
                else -> R.string.error_profile_update
            }
            Toast.makeText(context, messageRes, Toast.LENGTH_LONG).show()
        }
    }
}