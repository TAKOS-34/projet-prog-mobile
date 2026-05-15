package com.example.myapplication.fragment.post

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.adapter.PostsAdapter
import com.example.myapplication.dto.post.PostDto
import com.example.myapplication.utils.ApiClient
import com.example.myapplication.utils.SessionManager
import com.example.myapplication.utils.buildPostsAdapter
import com.google.gson.Gson

class PostViewerFragment : Fragment() {

    private lateinit var adapter: PostsAdapter
    private lateinit var tvTitle: TextView
    private var postId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_post_viewer, container, false)

        if (SessionManager.getUserId() == null) {
            findNavController().navigateUp()
            return view
        }

        postId = arguments?.getString(ARG_POST_ID)

        view.findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            findNavController().navigateUp()
        }

        tvTitle = view.findViewById(R.id.tvPostViewerTitle)

        adapter = buildPostsAdapter(onChanged = { findNavController().navigateUp() })
        view.findViewById<RecyclerView>(R.id.rvPostViewer).adapter = adapter

        return view
    }

    override fun onResume() {
        super.onResume()
        fetchPost()
    }

    private fun fetchPost() {
        val id = postId ?: return
        ApiClient.get("post/$id") { body, _, error ->
            activity?.runOnUiThread {
                if (error == null && body != null) {
                    try {
                        val post = Gson().fromJson(body, PostDto::class.java)
                        tvTitle.text = getString(R.string.post_viewer_title, post.username)
                        adapter.submitList(listOf(post))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    companion object {
        const val ARG_POST_ID = "postId"
    }
}
