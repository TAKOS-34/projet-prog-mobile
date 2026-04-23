package com.example.myapplication.fragment.group

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.myapplication.R
import com.example.myapplication.adapter.PostsAdapter
import com.example.myapplication.dto.group.GroupCardInfosDto
import com.example.myapplication.dto.post.PostDto
import com.example.myapplication.utils.ApiClient
import com.google.gson.reflect.TypeToken
import com.example.myapplication.utils.DateUtils
import com.example.myapplication.utils.resolveBackendUrl
import com.example.myapplication.utils.toShortDate
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.card.MaterialCardView
import com.google.android.material.imageview.ShapeableImageView
import com.google.gson.Gson

class GroupDetailFragment : Fragment() {

    private lateinit var group: GroupCardInfosDto
    private lateinit var postsAdapter: PostsAdapter
    private lateinit var rvPosts: RecyclerView
    private lateinit var scrollInfo: NestedScrollView
    private var postsLoaded = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_group_detail, container, false)
        val json = arguments?.getString(ARG_GROUP) ?: return view
        group = Gson().fromJson(json, GroupCardInfosDto::class.java)

        view.findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            findNavController().navigateUp()
        }

        scrollInfo = view.findViewById(R.id.scrollInfo)
        rvPosts = view.findViewById(R.id.rvGroupPosts)

        setupInfoTab(view)
        setupPostsTab()
        setupTabs(view)

        return view
    }

    private fun setupInfoTab(view: View) {
        val ctx = requireContext()
        view.findViewById<ShapeableImageView>(R.id.ivDetailAvatar).load(group.avatar.resolveBackendUrl()) {
            crossfade(true)
            placeholder(R.drawable.ic_launcher_background)
            transformations(CircleCropTransformation())
        }
        view.findViewById<TextView>(R.id.tvDetailName).text = group.name
        view.findViewById<ImageView>(R.id.ivDetailLock).visibility =
            if (group.isGroupPrivate) View.VISIBLE else View.GONE

        val tvDescription = view.findViewById<TextView>(R.id.tvDetailDescription)
        if (!group.description.isNullOrBlank()) {
            tvDescription.visibility = View.VISIBLE
            tvDescription.text = group.description
        } else {
            tvDescription.visibility = View.GONE
        }

        view.findViewById<TextView>(R.id.tvDetailMembers).text =
            ctx.getString(R.string.groups_members_count, group.nbMembers)
        view.findViewById<TextView>(R.id.tvDetailPosts).text =
            ctx.getString(R.string.groups_posts_count, group.nbPosts)
        view.findViewById<TextView>(R.id.tvDetailCreated).text =
            ctx.getString(R.string.groups_created_on, group.creationDate.toShortDate())

        view.findViewById<MaterialButton>(R.id.btnViewMembers).setOnClickListener {
            val bundle = Bundle().apply {
                putInt(GroupMembersFragment.ARG_GROUP_ID, group.id)
                putBoolean(GroupMembersFragment.ARG_IS_ADMIN, group.isAdmin)
            }
            findNavController().navigate(R.id.groupMembersFragment, bundle)
        }

        val btnBanned = view.findViewById<MaterialButton>(R.id.btnViewBanned)
        val btnRequests = view.findViewById<MaterialButton>(R.id.btnViewRequests)

        if (group.isAdmin) {
            btnBanned.visibility = View.VISIBLE
            btnBanned.setOnClickListener {
                val bundle = Bundle().apply { putInt(GroupBannedFragment.ARG_GROUP_ID, group.id) }
                findNavController().navigate(R.id.groupBannedFragment, bundle)
            }
            if (group.isGroupPrivate) {
                btnRequests.visibility = View.VISIBLE
                btnRequests.setOnClickListener {
                    val bundle = Bundle().apply { putInt(GroupRequestsFragment.ARG_GROUP_ID, group.id) }
                    findNavController().navigate(R.id.groupRequestsFragment, bundle)
                }
            }
        }
    }

    private fun setupPostsTab() {
        postsAdapter = PostsAdapter(
            onLike = { post, isNowLiked -> toggleLike(post, isNowLiked) },
            onComment = { post ->
                val bundle = Bundle().apply { putString("postId", post.id) }
                findNavController().navigate(R.id.commentsFragment, bundle)
            },
            onReport = { post ->
                val bundle = Bundle().apply { putString("postId", post.id) }
                findNavController().navigate(R.id.reportFragment, bundle)
            },
            onLocation = { post -> showGpsDialog(post) }
        )
        rvPosts.adapter = postsAdapter
    }

    private fun setupTabs(view: View) {
        val toggle = view.findViewById<MaterialButtonToggleGroup>(R.id.tgGroupTabs)
        toggle.check(R.id.btnTabInfo)
        toggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            when (checkedId) {
                R.id.btnTabInfo -> {
                    scrollInfo.visibility = View.VISIBLE
                    rvPosts.visibility = View.GONE
                }
                R.id.btnTabPosts -> {
                    scrollInfo.visibility = View.GONE
                    rvPosts.visibility = View.VISIBLE
                    if (!postsLoaded) fetchPosts()
                }
            }
        }
    }

    private fun fetchPosts() {
        ApiClient.get("group/${group.id}/posts") { body, _, error ->
            activity?.runOnUiThread {
                if (error == null && body != null) {
                    try {
                        val type = object : TypeToken<List<PostDto>>() {}.type
                        val posts: List<PostDto> = Gson().fromJson(body, type)
                        postsAdapter.submitList(posts)
                        postsLoaded = true
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun toggleLike(post: PostDto, isNowLiked: Boolean) {
        if (isNowLiked) {
            ApiClient.post("like/post/${post.id}", emptyMap<String, String>()) { _, _, _ -> }
        } else {
            ApiClient.delete("like/post/${post.id}") { _, _, _ -> }
        }
    }

    private fun showGpsDialog(post: PostDto) {
        val ctx = context ?: return
        val dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_gps_info, null)

        dialogView.findViewById<TextView>(R.id.tvLocationTitle).text = post.localisation
        dialogView.findViewById<TextView>(R.id.tvDate).text = DateUtils.formatAbsoluteDate(post.creationDate)
        dialogView.findViewById<TextView>(R.id.tvCoordinates).text = "%.4f N\n%.4f E".format(post.lat, post.long)

        val dialog = Dialog(ctx, android.R.style.Theme_Translucent_NoTitleBar)
        dialog.setContentView(dialogView)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        dialogView.findViewById<MaterialCardView>(R.id.btnClosePopup).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<MaterialButton>(R.id.btnOpenMaps).setOnClickListener {
            val uri = Uri.parse("geo:${post.lat},${post.long}?q=${post.lat},${post.long}(${post.localisation})")
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
        dialogView.findViewById<MaterialButton>(R.id.btnTravelPathRoute).setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    companion object {
        const val ARG_GROUP = "group"
    }
}
