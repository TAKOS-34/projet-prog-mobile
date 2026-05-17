package com.example.myapplication.fragment.profile

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.myapplication.R
import com.example.myapplication.dto.profile.UpdateProfileRequestDto
import com.example.myapplication.utils.ApiClient
import com.example.myapplication.utils.AuthViewModel
import com.example.myapplication.utils.FcmToken
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class EditProfileFragment : Fragment() {

    private val authViewModel: AuthViewModel by activityViewModels()

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
        applyInitialHints()

        view.findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            findNavController().navigateUp()
        }

        btnUpdate.setOnClickListener {
            if (validateFields()) {
                performUpdate()
            }
        }

        view.findViewById<MaterialButton>(R.id.tvDeleteAccount).setOnClickListener {
            confirmDeleteAccount()
        }

        return view
    }

    private fun applyInitialHints() {
        initialUsername = arguments?.getString(ARG_USERNAME).orEmpty()
        initialEmail = arguments?.getString(ARG_EMAIL).orEmpty()
        etUsername.setText(initialUsername)
        etEmail.setText(initialEmail)
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

    private fun confirmDeleteAccount() {
        val ctx = context ?: return
        MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.delete_account_confirm_title)
            .setMessage(R.string.delete_account_confirm_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.btn_delete_account) { _, _ -> performDeleteAccount() }
            .show()
    }

    private fun performDeleteAccount() {
        ApiClient.delete("profile") { _, _, error ->
            activity?.runOnUiThread {
                if (error == null) {
                    performLogout()
                } else {
                    Toast.makeText(context, R.string.error_global, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun performLogout() {
        FcmToken.fetchFcmToken { fcmToken ->
            authViewModel.logout(fcmToken) {
                activity?.runOnUiThread {
                    findNavController().navigate(R.id.loginFragment)
                }
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

    companion object {
        const val ARG_USERNAME = "username"
        const val ARG_EMAIL = "email"
    }
}