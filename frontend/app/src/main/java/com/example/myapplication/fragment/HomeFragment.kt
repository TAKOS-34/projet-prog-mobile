package com.example.myapplication.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.adapter.PostsAdapter
import com.example.myapplication.utils.PostFeedPaginator
import com.example.myapplication.utils.SessionManager
import com.example.myapplication.utils.buildPostsAdapter

class HomeFragment : Fragment() {

    private lateinit var adapter: PostsAdapter
    private lateinit var paginator: PostFeedPaginator

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        adapter = buildPostsAdapter(onChanged = { paginator.reset() })
        val rv = view.findViewById<RecyclerView>(R.id.rvPosts)
        rv.adapter = adapter

        paginator = PostFeedPaginator(
            recyclerView = rv,
            adapter = adapter,
            baseUrl = { "post" },
            onUi = { block -> activity?.runOnUiThread(block) }
        )
        paginator.reset()

        val fab = view.findViewById<View>(R.id.fabNewPost)
        if (SessionManager.getUserId() != null) {
            fab.visibility = View.VISIBLE
            fab.setOnClickListener { findNavController().navigate(R.id.createPostFragment) }
        } else {
            fab.visibility = View.GONE
        }

        return view
    }
}
