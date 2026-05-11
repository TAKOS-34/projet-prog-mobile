package com.example.myapplication.fragment.post

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.adapter.PostsAdapter
import com.example.myapplication.dto.post.TagDto
import com.example.myapplication.utils.ApiClient
import com.example.myapplication.utils.PostFeedPaginator
import com.example.myapplication.utils.buildPostsAdapter
import com.google.gson.Gson
import java.net.URLEncoder

class TagViewerFragment : Fragment() {

    private var tag: TagDto? = null

    private lateinit var btnFollow: ImageView
    private lateinit var rvPosts: RecyclerView
    private lateinit var postsAdapter: PostsAdapter
    private var paginator: PostFeedPaginator? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tag_viewer, container, false)

        view.findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            findNavController().navigateUp()
        }

        btnFollow = view.findViewById(R.id.btnFollowTag)
        btnFollow.setOnClickListener { toggleFollow() }

        rvPosts = view.findViewById(R.id.rvTagPosts)
        postsAdapter = buildPostsAdapter(onChanged = { paginator?.reset() })
        rvPosts.adapter = postsAdapter

        val identifier = arguments?.getString(ARG_TAG) ?: return view
        fetchTag(view, identifier)
        setupPostsPaginator(listOf(identifier))

        val nsv = view as NestedScrollView
        nsv.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
            val child = nsv.getChildAt(0) ?: return@OnScrollChangeListener
            val threshold = child.measuredHeight - nsv.measuredHeight - 400
            if (scrollY >= threshold) paginator?.tryLoadMore()
        })

        return view
    }

    private fun setupPostsPaginator(tagNames: List<String>) {
        val tagParams = tagNames.joinToString("&") {
            "tag=${URLEncoder.encode(it, Charsets.UTF_8.name())}"
        }
        paginator = PostFeedPaginator(
            recyclerView = rvPosts,
            adapter = postsAdapter,
            baseUrl = { "post?$tagParams" },
            onUi = { block -> activity?.runOnUiThread(block) }
        )
        paginator?.reset()
    }

    private fun fetchTag(view: View, identifier: String) {
        val encoded = URLEncoder.encode(identifier, Charsets.UTF_8.name())
        ApiClient.get("tag/$encoded") { body, _, error ->
            activity?.runOnUiThread {
                if (error == null && body != null) {
                    try {
                        tag = Gson().fromJson(body, TagDto::class.java)
                        renderTag(view, tag!!)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun renderTag(view: View, t: TagDto) {
        view.findViewById<TextView>(R.id.tvTagName).text = "#${t.name}"
        view.findViewById<TextView>(R.id.tvTagUsage).text =
            getString(R.string.tag_viewer_usage, t.nbUses)
        view.findViewById<ImageView>(R.id.ivFire).visibility =
            if (t.isPopular) View.VISIBLE else View.GONE
        applyFollowState(t.isFollowing)
    }

    private fun applyFollowState(isFollowing: Boolean) {
        btnFollow.setImageResource(if (isFollowing) R.drawable.ic_bell_filled else R.drawable.ic_bell)
        btnFollow.setColorFilter(
            ContextCompat.getColor(
                requireContext(),
                if (isFollowing) R.color.primary else R.color.text_secondary
            )
        )
    }

    private fun toggleFollow() {
        val current = tag ?: return
        val target = !current.isFollowing
        btnFollow.isEnabled = false

        val callback: (String?, Int, String?) -> Unit = { _, _, error ->
            activity?.runOnUiThread {
                btnFollow.isEnabled = true
                if (error == null) {
                    tag = current.copy(isFollowing = target)
                    applyFollowState(target)
                    val msg = if (target) R.string.success_followed else R.string.success_unfollowed
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, R.string.error_follow_action, Toast.LENGTH_SHORT).show()
                }
            }
        }

        if (target) {
            ApiClient.post("notification/tag/${current.id}", emptyMap<String, String>(), callback)
        } else {
            ApiClient.delete("notification/tag/${current.id}", callback)
        }
    }

    companion object {
        const val ARG_TAG = "tag"
    }
}
