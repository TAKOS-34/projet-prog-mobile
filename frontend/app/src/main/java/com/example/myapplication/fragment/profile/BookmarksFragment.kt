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
import com.example.myapplication.adapter.TripFeedAdapter
import com.example.myapplication.dto.trip.TripFeedItemDto
import com.example.myapplication.utils.PostFeedPaginator
import com.example.myapplication.utils.TripFeedPaginator
import com.example.myapplication.utils.buildPostsAdapter
import com.example.myapplication.utils.buildTripFeedAdapter
import com.google.android.material.button.MaterialButtonToggleGroup

class BookmarksFragment : Fragment() {

    private enum class Tab { POSTS, TRIPS }

    private lateinit var postsAdapter: PostsAdapter
    private lateinit var postsPaginator: PostFeedPaginator

    private lateinit var tripsAdapter: TripFeedAdapter
    private lateinit var tripsPaginator: TripFeedPaginator

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var tgTabs: MaterialButtonToggleGroup

    private var currentTab = Tab.POSTS

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_bookmarks, container, false)

        recyclerView = view.findViewById(R.id.rvBookmarks)
        tvEmpty = view.findViewById(R.id.tvBookmarksEmpty)
        tgTabs = view.findViewById(R.id.tgBookmarkTabs)

        view.findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            findNavController().navigateUp()
        }

        setupPostsTab()
        setupTripsTab()

        tgTabs.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            currentTab = if (checkedId == R.id.btnBookmarkPosts) Tab.POSTS else Tab.TRIPS
            activateTab()
        }

        tgTabs.check(R.id.btnBookmarkPosts)

        return view
    }

    private fun setupPostsTab() {
        postsAdapter = buildPostsAdapter(onChanged = { postsPaginator.reset() })
        postsPaginator = PostFeedPaginator(
            recyclerView = recyclerView,
            adapter = postsAdapter,
            baseUrl = { "bookmark/post" },
            onUi = { block -> activity?.runOnUiThread(block) },
            onResults = { isEmpty -> updateEmptyState(isEmpty) }
        )
    }

    private fun setupTripsTab() {
        tripsAdapter = buildTripFeedAdapter { trip ->
            tripsAdapter.submitList(tripsAdapter.currentList.filter { it.id != trip.id })
        }
        tripsPaginator = TripFeedPaginator(
            recyclerView = recyclerView,
            adapter = tripsAdapter,
            url = { "bookmark/trip" },
            onUi = { block -> activity?.runOnUiThread(block) },
            onResults = { isEmpty -> updateEmptyState(isEmpty) }
        )
    }

    private fun activateTab() {
        when (currentTab) {
            Tab.POSTS -> {
                recyclerView.adapter = postsAdapter
                if (postsAdapter.itemCount == 0) postsPaginator.reset()
            }
            Tab.TRIPS -> {
                recyclerView.adapter = tripsAdapter
                if (tripsAdapter.itemCount == 0) tripsPaginator.reset()
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

}
