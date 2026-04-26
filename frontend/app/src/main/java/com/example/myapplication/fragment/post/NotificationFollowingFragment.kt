package com.example.myapplication.fragment.post

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.adapter.FollowingAdapter
import com.example.myapplication.dto.notification.FollowingDto
import com.example.myapplication.utils.ApiClient
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class NotificationFollowingFragment : Fragment() {

    private lateinit var groupsAdapter: FollowingAdapter
    private lateinit var usersAdapter: FollowingAdapter
    private lateinit var tagsAdapter: FollowingAdapter

    private lateinit var rvGroups: RecyclerView
    private lateinit var rvUsers: RecyclerView
    private lateinit var rvTags: RecyclerView
    private lateinit var tvGroupsEmpty: TextView
    private lateinit var tvUsersEmpty: TextView
    private lateinit var tvTagsEmpty: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notification_following, container, false)

        rvGroups = view.findViewById(R.id.rvGroupsFollowing)
        rvUsers = view.findViewById(R.id.rvUsersFollowing)
        rvTags = view.findViewById(R.id.rvTagsFollowing)
        tvGroupsEmpty = view.findViewById(R.id.tvGroupsEmpty)
        tvUsersEmpty = view.findViewById(R.id.tvUsersEmpty)
        tvTagsEmpty = view.findViewById(R.id.tvTagsEmpty)

        groupsAdapter = FollowingAdapter { unfollow(it) }
        usersAdapter = FollowingAdapter { unfollow(it) }
        tagsAdapter = FollowingAdapter { unfollow(it) }
        rvGroups.adapter = groupsAdapter
        rvUsers.adapter = usersAdapter
        rvTags.adapter = tagsAdapter

        view.findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            findNavController().navigateUp()
        }

        fetchFollowing()

        return view
    }

    private fun fetchFollowing() {
        ApiClient.get("notification/following") { body, _, error ->
            activity?.runOnUiThread {
                if (error == null && body != null) {
                    try {
                        val type = object : TypeToken<List<FollowingDto>>() {}.type
                        val all: List<FollowingDto> = Gson().fromJson(body, type)
                        renderSection(all.filter { it.type.equals("group", true) }, groupsAdapter, rvGroups, tvGroupsEmpty)
                        renderSection(all.filter { it.type.equals("user", true) }, usersAdapter, rvUsers, tvUsersEmpty)
                        renderSection(all.filter { it.type.equals("tag", true) }, tagsAdapter, rvTags, tvTagsEmpty)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun renderSection(
        items: List<FollowingDto>,
        adapter: FollowingAdapter,
        rv: RecyclerView,
        empty: TextView
    ) {
        empty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        rv.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
        adapter.submitList(items)
    }

    private fun unfollow(item: FollowingDto) {
        val id = FollowingAdapter.idOf(item) ?: return
        ApiClient.delete("notification/${item.type.lowercase()}/$id") { _, _, error ->
            activity?.runOnUiThread {
                if (error == null) {
                    Toast.makeText(context, R.string.success_unfollowed, Toast.LENGTH_SHORT).show()
                    fetchFollowing()
                } else {
                    Toast.makeText(context, R.string.error_follow_action, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
