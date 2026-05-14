package com.example.myapplication.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.adapter.PostsAdapter
import com.example.myapplication.adapter.TripFeedAdapter
import com.example.myapplication.dto.trip.TripFeedItemDto
import com.example.myapplication.utils.PostFeedPaginator
import com.example.myapplication.utils.SessionManager
import com.example.myapplication.utils.TripFeedPaginator
import com.example.myapplication.utils.buildPostsAdapter
import com.example.myapplication.utils.buildTripFeedAdapter
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.gson.Gson

class HomeFragment : Fragment() {

    private enum class FeedMode { TRAVEL_SHARE, TRAVEL_PATH }
    private enum class TripTab { WORLD, MINE }

    private lateinit var postsAdapter: PostsAdapter
    private lateinit var postPaginator: PostFeedPaginator

    private lateinit var worldTripAdapter: TripFeedAdapter
    private lateinit var worldTripPaginator: TripFeedPaginator

    private lateinit var myTripAdapter: TripFeedAdapter
    private lateinit var myTripPaginator: TripFeedPaginator

    private lateinit var rvPosts: RecyclerView
    private lateinit var rvTrips: RecyclerView
    private lateinit var tvTripsEmpty: TextView
    private lateinit var tvFeedTitle: TextView
    private lateinit var btnToggleFeed: ImageView
    private lateinit var tgTripTabs: MaterialButtonToggleGroup
    private lateinit var fabNewPost: View
    private lateinit var fabNewTrip: View

    private var feedMode = FeedMode.TRAVEL_SHARE
    private var tripTab = TripTab.MINE
    private val gson = Gson()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        rvPosts = view.findViewById(R.id.rvPosts)
        rvTrips = view.findViewById(R.id.rvTrips)
        tvTripsEmpty = view.findViewById(R.id.tvTripsEmpty)
        tvFeedTitle = view.findViewById(R.id.tvFeedTitle)
        btnToggleFeed = view.findViewById(R.id.btnToggleFeed)
        tgTripTabs = view.findViewById(R.id.tgTripTabs)
        fabNewPost = view.findViewById(R.id.fabNewPost)
        fabNewTrip = view.findViewById(R.id.fabNewTrip)

        val isLogged = SessionManager.getUserId() != null

        if (!::postsAdapter.isInitialized) {
            setupPostsFeed()
            setupTripFeeds(isLogged)
        } else {
            rebindViews(isLogged)
        }

        setupToggleButton(isLogged)
        setupTripTabs(isLogged)
        applyFeedMode(isLogged)

        return view
    }

    private fun rebindViews(isLogged: Boolean) {
        rvPosts.adapter = postsAdapter
        postPaginator.reattach(rvPosts)

        fabNewPost.visibility = if (isLogged) View.VISIBLE else View.GONE
        fabNewPost.setOnClickListener { findNavController().navigate(R.id.createPostFragment) }
        fabNewTrip.setOnClickListener { findNavController().navigate(R.id.createTripFragment) }

        worldTripPaginator.reattach(rvTrips)
        myTripPaginator.reattach(rvTrips)
    }

    private fun setupPostsFeed() {
        postsAdapter = buildPostsAdapter(onChanged = { postPaginator.reset() })
        rvPosts.adapter = postsAdapter

        postPaginator = PostFeedPaginator(
            recyclerView = rvPosts,
            adapter = postsAdapter,
            baseUrl = { "post" },
            onUi = { block -> activity?.runOnUiThread(block) }
        )
        postPaginator.reset()
    }

    private fun setupTripFeeds(isLogged: Boolean) {
        worldTripAdapter = buildTripFeedAdapter { trip ->
            worldTripAdapter.submitList(worldTripAdapter.currentList.filter { it.id != trip.id })
        }
        worldTripPaginator = TripFeedPaginator(
            recyclerView = rvTrips,
            adapter = worldTripAdapter,
            url = { "trip" },
            onUi = { block -> activity?.runOnUiThread(block) },
            onResults = { isEmpty -> updateEmptyState(isEmpty) }
        )

        myTripAdapter = buildTripFeedAdapter { trip ->
            myTripAdapter.submitList(myTripAdapter.currentList.filter { it.id != trip.id })
        }
        myTripPaginator = TripFeedPaginator(
            recyclerView = rvTrips,
            adapter = myTripAdapter,
            url = { "trip/my-trips" },
            onUi = { block -> activity?.runOnUiThread(block) },
            onResults = { isEmpty -> updateEmptyState(isEmpty) }
        )

        fabNewPost.visibility = if (isLogged) View.VISIBLE else View.GONE
        fabNewPost.setOnClickListener { findNavController().navigate(R.id.createPostFragment) }

        fabNewTrip.visibility = View.GONE
        fabNewTrip.setOnClickListener { findNavController().navigate(R.id.createTripFragment) }
    }

    private fun setupToggleButton(isLogged: Boolean) {
        btnToggleFeed.setOnClickListener {
            feedMode = if (feedMode == FeedMode.TRAVEL_SHARE) FeedMode.TRAVEL_PATH else FeedMode.TRAVEL_SHARE
            applyFeedMode(isLogged)
        }
    }

    private fun setupTripTabs(isLogged: Boolean) {
        if (!isLogged) {
            tgTripTabs.findViewById<View>(R.id.btnTabMyTrips).visibility = View.GONE
        }

        tgTripTabs.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            tripTab = if (checkedId == R.id.btnTabWorldTrips) TripTab.WORLD else TripTab.MINE
            activateTripTab()
        }
    }

    private fun applyFeedMode(isLogged: Boolean) {
        when (feedMode) {
            FeedMode.TRAVEL_SHARE -> {
                tvFeedTitle.text = getString(R.string.home_tab_travelshare)
                btnToggleFeed.setImageResource(R.drawable.ic_location)
                btnToggleFeed.imageTintList = ContextCompat.getColorStateList(requireContext(), R.color.primary)

                rvPosts.visibility = View.VISIBLE
                rvTrips.visibility = View.GONE
                tvTripsEmpty.visibility = View.GONE
                tgTripTabs.visibility = View.GONE

                fabNewPost.visibility = if (isLogged) View.VISIBLE else View.GONE
                fabNewTrip.visibility = View.GONE
            }
            FeedMode.TRAVEL_PATH -> {
                tvFeedTitle.text = getString(R.string.home_tab_travelpath)
                btnToggleFeed.setImageResource(R.drawable.ic_fire)
                btnToggleFeed.imageTintList = ContextCompat.getColorStateList(requireContext(), R.color.primary)

                rvPosts.visibility = View.GONE
                tgTripTabs.visibility = View.VISIBLE

                fabNewPost.visibility = View.GONE
                fabNewTrip.visibility = if (isLogged) View.VISIBLE else View.GONE

                tgTripTabs.check(if (tripTab == TripTab.WORLD) R.id.btnTabWorldTrips else R.id.btnTabMyTrips)
                activateTripTab()
            }
        }
    }

    private fun activateTripTab() {
        rvTrips.visibility = View.VISIBLE
        tvTripsEmpty.visibility = View.GONE

        when (tripTab) {
            TripTab.WORLD -> {
                rvTrips.adapter = worldTripAdapter
                if (worldTripAdapter.itemCount == 0) worldTripPaginator.reset()
            }
            TripTab.MINE -> {
                rvTrips.adapter = myTripAdapter
                if (myTripAdapter.itemCount == 0) myTripPaginator.reset()
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        tvTripsEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        rvTrips.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

}
