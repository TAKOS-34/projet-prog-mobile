package com.example.myapplication.fragment

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.adapter.PostsAdapter
import com.example.myapplication.dto.post.PostDto
import com.example.myapplication.dto.post.PostsResponseDto
import com.example.myapplication.utils.ApiClient
import com.example.myapplication.utils.DateUtils
import com.google.android.material.card.MaterialCardView
import com.google.gson.Gson

class HomeFragment : Fragment() {

    private lateinit var adapter: PostsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        adapter = PostsAdapter(
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
        view.findViewById<RecyclerView>(R.id.rvPosts).adapter = adapter

        view.findViewById<View>(R.id.fabNewPost).setOnClickListener {
            findNavController().navigate(R.id.createPostFragment)
        }

        fetchPosts()

        return view
    }

    private fun fetchPosts() {
        ApiClient.get("post") { body, _, error ->
            activity?.runOnUiThread {
                if (error == null && body != null) {
                    try {
                        val response = Gson().fromJson(body, PostsResponseDto::class.java)
                        adapter.submitList(response.posts)
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

        dialogView.findViewById<MaterialCardView>(R.id.btnClosePopup).setOnClickListener {
            dialog.dismiss()
        }
        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnOpenMaps).setOnClickListener {
            val uri = Uri.parse("geo:${post.lat},${post.long}?q=${post.lat},${post.long}(${post.localisation})")
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnTravelPathRoute).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
