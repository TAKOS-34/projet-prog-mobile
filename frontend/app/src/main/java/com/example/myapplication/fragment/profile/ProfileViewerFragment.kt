package com.example.myapplication.fragment.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import coil.load
import coil.transform.CircleCropTransformation
import com.example.myapplication.R
import com.example.myapplication.dto.profile.UserPublicProfileDto
import com.example.myapplication.utils.ApiClient
import com.example.myapplication.utils.SessionManager
import com.example.myapplication.utils.resolveBackendUrl
import com.example.myapplication.utils.toShortDate
import com.google.gson.Gson

class ProfileViewerFragment : Fragment() {

    private var userId: Int = 0
    private var profile: UserPublicProfileDto? = null

    private lateinit var btnFollow: ImageView
    private lateinit var tvSelfBadge: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile_viewer, container, false)

        userId = arguments?.getInt(ARG_USER_ID) ?: return view

        view.findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            findNavController().navigateUp()
        }

        btnFollow = view.findViewById(R.id.btnFollow)
        tvSelfBadge = view.findViewById(R.id.tvSelfBadge)
        btnFollow.setOnClickListener { toggleFollow() }

        fetchProfile(view)

        return view
    }

    private fun fetchProfile(view: View) {
        ApiClient.get("profile/$userId") { body, _, error ->
            activity?.runOnUiThread {
                if (error == null && body != null) {
                    try {
                        profile = Gson().fromJson(body, UserPublicProfileDto::class.java)
                        renderProfile(view, profile!!)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun renderProfile(view: View, p: UserPublicProfileDto) {
        view.findViewById<TextView>(R.id.tvProfileName).text = p.username
        view.findViewById<TextView>(R.id.tvNbPosts).text = p.nbPosts.toString()
        view.findViewById<TextView>(R.id.tvNbGroups).text = p.nbGroups.toString()

        val memberPrefix = getString(R.string.profile_member_since)
        view.findViewById<TextView>(R.id.tvMemberSince).text = "$memberPrefix ${p.creationDate.toShortDate()}"

        view.findViewById<ImageView>(R.id.ivProfileCover).load(p.avatar.resolveBackendUrl()) {
            crossfade(true)
            placeholder(R.drawable.ic_launcher_background)
            transformations(CircleCropTransformation())
        }

        val isSelf = p.id == SessionManager.getUserId()
        if (isSelf) {
            tvSelfBadge.visibility = View.VISIBLE
            btnFollow.visibility = View.GONE
        } else {
            tvSelfBadge.visibility = View.GONE
            btnFollow.visibility = View.VISIBLE
            applyFollowState(p.isFollowing)
        }
    }

    private fun applyFollowState(isFollowing: Boolean) {
        btnFollow.setImageResource(if (isFollowing) R.drawable.ic_bell_filled else R.drawable.ic_bell)
        btnFollow.setColorFilter(
            androidx.core.content.ContextCompat.getColor(
                requireContext(),
                if (isFollowing) R.color.primary else R.color.text_secondary
            )
        )
    }

    private fun toggleFollow() {
        val current = profile ?: return
        val target = !current.isFollowing
        btnFollow.isEnabled = false

        val callback: (String?, Int, String?) -> Unit = { _, _, error ->
            activity?.runOnUiThread {
                btnFollow.isEnabled = true
                if (error == null) {
                    profile = current.copy(isFollowing = target)
                    applyFollowState(target)
                    val msg = if (target) R.string.success_followed else R.string.success_unfollowed
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, R.string.error_follow_action, Toast.LENGTH_SHORT).show()
                }
            }
        }

        if (target) {
            ApiClient.post("notification/user/${current.id}", emptyMap<String, String>(), callback)
        } else {
            ApiClient.delete("notification/user/${current.id}", callback)
        }
    }

    companion object {
        const val ARG_USER_ID = "userId"
    }
}
