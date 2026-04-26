package com.example.myapplication.fragment.post

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.adapter.NotificationsAdapter
import com.example.myapplication.dto.notification.NotificationDto
import com.example.myapplication.utils.ApiClient
import com.example.myapplication.utils.AuthViewModel
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class NotificationsFragment : Fragment() {

    private val authViewModel: AuthViewModel by activityViewModels()

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: NotificationsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (!authViewModel.isAuthenticated()) {
            val guestView = inflater.inflate(R.layout.fragment_guest_prompt, container, false)
            guestView.findViewById<MaterialButton>(R.id.btnGuestLogin).setOnClickListener {
                findNavController().navigate(R.id.loginFragment)
            }
            return guestView
        }

        val view = inflater.inflate(R.layout.fragment_notifications, container, false)

        recyclerView = view.findViewById(R.id.rvNotifications)
        tvEmpty = view.findViewById(R.id.tvNotifEmpty)

        view.findViewById<android.widget.ImageView>(R.id.btnNotifSettings).setOnClickListener {
            findNavController().navigate(R.id.notificationFollowingFragment)
        }

        adapter = NotificationsAdapter { notif ->
            notif.postId?.let {
                val bundle = Bundle().apply { putString(PostViewerFragment.ARG_POST_ID, it) }
                findNavController().navigate(R.id.postViewerFragment, bundle)
            }
        }
        recyclerView.adapter = adapter

        fetchNotifications()

        return view
    }

    private fun fetchNotifications() {
        ApiClient.get("notification") { body, _, error ->
            activity?.runOnUiThread {
                if (error == null && body != null) {
                    try {
                        val type = object : TypeToken<List<NotificationDto>>() {}.type
                        val notifications: List<NotificationDto> = Gson().fromJson(body, type)
                        renderState(notifications.isEmpty())
                        adapter.submitList(notifications)

                        val unreadIds = notifications.filter { !it.isRead }.map { it.id }
                        if (unreadIds.isNotEmpty()) markAsRead(unreadIds, notifications)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun markAsRead(ids: List<Int>, current: List<NotificationDto>) {
        val startedAt = System.currentTimeMillis()
        ApiClient.patch("notification/mark-as-read", mapOf("notificationIds" to ids)) { _, _, error ->
            if (error == null) {
                val remaining = (1000 - (System.currentTimeMillis() - startedAt)).coerceAtLeast(0)
                Handler(Looper.getMainLooper()).postDelayed({
                    if (isAdded) {
                        adapter.submitList(current.map { it.copy(isRead = true) })
                        (activity as? com.example.myapplication.fragment.MainActivity)?.refreshNotificationBadge()
                    }
                }, remaining)
            }
        }
    }

    private fun renderState(isEmpty: Boolean) {
        tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
}
