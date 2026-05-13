package com.example.myapplication.fragment

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
import com.example.myapplication.utils.PostFeedPaginator
import com.example.myapplication.utils.SessionManager
import com.example.myapplication.utils.buildPostsAdapter
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup

class HomeFragment : Fragment() {

    private lateinit var adapter: PostsAdapter
    private lateinit var paginator: PostFeedPaginator
    private lateinit var rvPosts: RecyclerView
    private lateinit var rvTrips: RecyclerView
    private lateinit var tvTripsEmpty: TextView
    private lateinit var fabNewPost: View
    private lateinit var fabNewTrip: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        rvPosts = view.findViewById(R.id.rvPosts)
        rvTrips = view.findViewById(R.id.rvTrips)
        tvTripsEmpty = view.findViewById(R.id.tvTripsEmpty)
        fabNewPost = view.findViewById(R.id.fabNewPost)
        fabNewTrip = view.findViewById(R.id.fabNewTrip)

        adapter = buildPostsAdapter(onChanged = { paginator.reset() })
        rvPosts.adapter = adapter

        paginator = PostFeedPaginator(
            recyclerView = rvPosts,
            adapter = adapter,
            baseUrl = { "post" },
            onUi = { block -> activity?.runOnUiThread(block) }
        )
        paginator.reset()

        val isLogged = SessionManager.getUserId() != null

        fabNewPost.visibility = if (isLogged) View.VISIBLE else View.GONE
        fabNewPost.setOnClickListener { findNavController().navigate(R.id.createPostFragment) }

        fabNewTrip.setOnClickListener { findNavController().navigate(R.id.createTripFragment) }

        view.findViewById<MaterialButtonToggleGroup>(R.id.tgFeedTabs)
            .addOnButtonCheckedListener { _, checkedId, isChecked ->
                if (!isChecked) return@addOnButtonCheckedListener
                when (checkedId) {
                    R.id.btnTabTravelShare -> showTravelShare()
                    R.id.btnTabTravelPath -> showTravelPath(isLogged)
                }
            }

        view.findViewById<MaterialButtonToggleGroup>(R.id.tgFeedTabs).check(R.id.btnTabTravelShare)

        return view
    }

    private fun showTravelShare() {
        rvPosts.visibility = View.VISIBLE
        rvTrips.visibility = View.GONE
        tvTripsEmpty.visibility = View.GONE
        fabNewPost.visibility = if (SessionManager.getUserId() != null) View.VISIBLE else View.GONE
        fabNewTrip.visibility = View.GONE
    }

    private fun showTravelPath(isLogged: Boolean) {
        rvPosts.visibility = View.GONE
        rvTrips.visibility = View.GONE
        tvTripsEmpty.visibility = View.VISIBLE
        fabNewPost.visibility = View.GONE
        fabNewTrip.visibility = if (SessionManager.getUserId() != null) View.VISIBLE else View.GONE
    }
}
