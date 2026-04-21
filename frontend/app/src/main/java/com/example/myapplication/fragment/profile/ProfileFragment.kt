package com.example.myapplication.fragment.profile

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import coil.load
import coil.transform.CircleCropTransformation
import com.example.myapplication.R
import com.example.myapplication.dto.profile.ProfileResponseDto
import com.example.myapplication.utils.ApiClient
import com.example.myapplication.utils.AuthViewModel
import com.example.myapplication.utils.FcmToken
import com.example.myapplication.utils.resolveBackendUrl
import com.example.myapplication.utils.toShortDate
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson

class ProfileFragment : Fragment() {

    private val authViewModel: AuthViewModel by activityViewModels()

    private var pickedAvatarUri: Uri? = null
    private var ivProfileCover: ImageView? = null
    private var btnSaveAvatar: MaterialButton? = null

    private val pickAvatarLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            pickedAvatarUri = uri
            ivProfileCover?.load(uri) {
                crossfade(true)
                transformations(CircleCropTransformation())
            }
            btnSaveAvatar?.visibility = View.VISIBLE
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val isLoggedIn = authViewModel.isAuthenticated()

        val layoutId = if (isLoggedIn) R.layout.fragment_profile else R.layout.fragment_profile_guest
        val view = inflater.inflate(layoutId, container, false)

        if (isLoggedIn) {
            setupProfileView(view)
            fetchProfileData(view)
        } else {
            setupGuestView(view)
        }

        return view
    }

    private fun setupGuestView(view: View) {
        view.findViewById<MaterialButton>(R.id.btnLoginGuest).setOnClickListener {
            findNavController().navigate(R.id.loginFragment)
        }
        view.findViewById<MaterialButton>(R.id.btnRegisterGuest).setOnClickListener {
            findNavController().navigate(R.id.registerFragment)
        }
    }

    private fun setupProfileView(view: View) {
        ivProfileCover = view.findViewById(R.id.ivProfileCover)
        btnSaveAvatar = view.findViewById(R.id.btnSaveAvatar)

        view.findViewById<FloatingActionButton>(R.id.fabEditAvatar).setOnClickListener {
            pickAvatarLauncher.launch("image/*")
        }

        btnSaveAvatar?.setOnClickListener {
            pickedAvatarUri?.let { uploadAvatar(it) }
        }

        view.findViewById<MaterialButton>(R.id.btnEditProfile).setOnClickListener {
            findNavController().navigate(R.id.editProfileFragment)
        }

        view.findViewById<MaterialButton>(R.id.btnSessions).setOnClickListener {
            findNavController().navigate(R.id.sessionsFragment)
        }

        view.findViewById<MaterialButton>(R.id.btnLogout).setOnClickListener {
            FcmToken.fetchFcmToken { fcmToken ->
                authViewModel.logout(fcmToken) {
                    activity?.runOnUiThread {
                        findNavController().navigate(R.id.loginFragment)
                    }
                }
            }
        }
    }

    private fun uploadAvatar(uri: Uri) {
        val ctx = context ?: return
        val bytes = runCatching {
            ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull() ?: run {
            Toast.makeText(ctx, R.string.error_avatar_upload, Toast.LENGTH_LONG).show()
            return
        }

        val mimeType = ctx.contentResolver.getType(uri) ?: "image/jpeg"
        val fileName = "avatar_${System.currentTimeMillis()}.jpg"

        btnSaveAvatar?.isEnabled = false
        ApiClient.patchMultipart("profile/avatar", "avatar", fileName, bytes, mimeType) { _, _, error ->
            activity?.runOnUiThread {
                btnSaveAvatar?.isEnabled = true
                if (error == null) {
                    Toast.makeText(ctx, R.string.success_avatar_upload, Toast.LENGTH_SHORT).show()
                    btnSaveAvatar?.visibility = View.GONE
                    pickedAvatarUri = null
                    view?.let { fetchProfileData(it) }
                } else {
                    Toast.makeText(ctx, R.string.error_avatar_upload, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun fetchProfileData(view: View) {
        ApiClient.get("profile") { body, code, error ->
            activity?.runOnUiThread {
                if (error == null && body != null) {
                    try {
                        val profile = Gson().fromJson(body, ProfileResponseDto::class.java)

                        view.findViewById<TextView>(R.id.tvProfileName).text = profile.username
                        view.findViewById<TextView>(R.id.tvProfileBio).text = profile.email
                        view.findViewById<TextView>(R.id.tvNbPosts).text = profile.nbPosts.toString()
                        view.findViewById<TextView>(R.id.tvNbGroups).text = profile.nbGroups.toString()

                        val memberSincePrefix = getString(R.string.profile_member_since)
                        view.findViewById<TextView>(R.id.tvMemberSince).text = "$memberSincePrefix ${profile.creationDate.toShortDate()}"

                        view.findViewById<ImageView>(R.id.ivProfileCover).load(profile.avatar.resolveBackendUrl()) {
                            crossfade(true)
                            placeholder(R.drawable.ic_launcher_background)
                            transformations(CircleCropTransformation())
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}