package com.example.myapplication.fragment.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.myapplication.R
import com.example.myapplication.dto.profile.TokenDto
import com.example.myapplication.utils.ApiClient
import com.example.myapplication.utils.AuthViewModel
import com.example.myapplication.utils.FcmToken
import com.example.myapplication.utils.toShortDate
import com.google.android.material.card.MaterialCardView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SessionsFragment : Fragment() {

    private val authViewModel: AuthViewModel by activityViewModels()

    private lateinit var container: LinearLayout
    private lateinit var tvEmpty: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sessions, container, false)

        this.container = view.findViewById(R.id.sessionsContainer)
        tvEmpty = view.findViewById(R.id.tvEmptySessions)

        view.findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            findNavController().navigateUp()
        }

        fetchSessions()

        return view
    }

    private fun fetchSessions() {
        ApiClient.get("profile/tokens") { body, _, error ->
            activity?.runOnUiThread {
                if (error == null && body != null) {
                    try {
                        val type = object : TypeToken<List<TokenDto>>() {}.type
                        val tokens: List<TokenDto> = Gson().fromJson(body, type)
                        renderSessions(tokens)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun renderSessions(tokens: List<TokenDto>) {
        container.removeAllViews()
        tvEmpty.visibility = if (tokens.isEmpty()) View.VISIBLE else View.GONE

        val inflater = LayoutInflater.from(context)
        tokens.forEach { token ->
            val item = inflater.inflate(R.layout.item_session, container, false)
            item.findViewById<TextView>(R.id.tvSessionDevice).text = token.device.ifBlank {
                getString(R.string.session_unknown_device)
            }
            item.findViewById<TextView>(R.id.tvSessionIp).text = token.ip
            item.findViewById<TextView>(R.id.tvSessionDate).text = token.creationDate.toShortDate()

            val badge = item.findViewById<TextView>(R.id.tvCurrentSessionBadge)
            val card = item as MaterialCardView
            if (token.isYourSession) {
                badge.visibility = View.VISIBLE
                card.strokeWidth = (2 * resources.displayMetrics.density).toInt()
                card.strokeColor = ContextCompat.getColor(requireContext(), R.color.primary)
                card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.surface_variant))
            } else {
                badge.visibility = View.GONE
            }

            item.findViewById<ImageView>(R.id.btnDeleteSession).setOnClickListener {
                if (token.isYourSession) {
                    performLogout()
                } else {
                    deleteSession(token.id)
                }
            }
            container.addView(item)
        }
    }

    private fun deleteSession(tokenId: Int) {
        ApiClient.delete("profile/token/$tokenId") { _, _, error ->
            activity?.runOnUiThread {
                if (error == null) {
                    Toast.makeText(context, R.string.success_session_deleted, Toast.LENGTH_SHORT).show()
                    fetchSessions()
                } else {
                    Toast.makeText(context, R.string.error_session_delete, Toast.LENGTH_LONG).show()
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
}
