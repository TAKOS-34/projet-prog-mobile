package com.example.myapplication.fragment.profile

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
import com.example.myapplication.utils.PostFeedPaginator
import com.example.myapplication.utils.buildPostsAdapter

class BookmarksFragment : Fragment() {

    private lateinit var adapter: PostsAdapter
    private lateinit var paginator: PostFeedPaginator
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_bookmarks, container, false)

        recyclerView = view.findViewById(R.id.rvBookmarks)
        tvEmpty = view.findViewById(R.id.tvBookmarksEmpty)

        view.findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            findNavController().navigateUp()
        }

        adapter = buildPostsAdapter(onChanged = { paginator.reset() })
        recyclerView.adapter = adapter

        paginator = PostFeedPaginator(
            recyclerView = recyclerView,
            adapter = adapter,
            baseUrl = { "bookmark/post" },
            onUi = { block -> activity?.runOnUiThread(block) },
            onResults = { isEmpty ->
                tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
                recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
            }
        )
        paginator.reset()

        return view
    }
}
