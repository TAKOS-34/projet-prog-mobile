package com.example.myapplication.fragment.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.adapter.SessionsAdapter
import com.example.myapplication.dto.profile.TokenDto
import com.example.myapplication.utils.ApiClient
import com.example.myapplication.utils.AuthViewModel
import com.example.myapplication.utils.FcmToken
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SessionsFragment : Fragment() {

    private val authViewModel: AuthViewModel by activityViewModels()

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: SessionsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sessions, container, false)

        recyclerView = view.findViewById(R.id.rvSessions)
        tvEmpty = view.findViewById(R.id.tvEmptySessions)

        adapter = SessionsAdapter { token ->
            if (token.isYourSession) performLogout() else deleteSession(token.id)
        }
        recyclerView.adapter = adapter

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
        tvEmpty.visibility = if (tokens.isEmpty()) View.VISIBLE else View.GONE
        recyclerView.visibility = if (tokens.isEmpty()) View.GONE else View.VISIBLE
        adapter.submitList(tokens)
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
