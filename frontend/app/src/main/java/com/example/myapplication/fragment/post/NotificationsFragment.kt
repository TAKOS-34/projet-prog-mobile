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
import com.example.myapplication.utils.NotificationsPaginator
import com.google.android.material.button.MaterialButton

class NotificationsFragment : Fragment() {

    private val authViewModel: AuthViewModel by activityViewModels()

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: NotificationsAdapter
    private lateinit var paginator: NotificationsPaginator

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

        paginator = NotificationsPaginator(
            recyclerView = recyclerView,
            adapter = adapter,
            onUi = { block -> activity?.runOnUiThread(block) },
            onResults = { isEmpty -> renderState(isEmpty) },
            onPageLoaded = { page -> markPageAsRead(page) }
        )
        paginator.reset()

        return view
    }

    private fun markPageAsRead(page: List<NotificationDto>) {
        val unreadIds = page.filter { !it.isRead }.map { it.id }
        if (unreadIds.isEmpty()) return

        val startedAt = System.currentTimeMillis()
        ApiClient.patch("notification/mark-as-read", mapOf("notificationIds" to unreadIds)) { _, _, error ->
            if (error == null) {
                val remaining = (1000 - (System.currentTimeMillis() - startedAt)).coerceAtLeast(0)
                Handler(Looper.getMainLooper()).postDelayed({
                    if (isAdded) {
                        paginator.markItemsAsRead(unreadIds.toSet())
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
