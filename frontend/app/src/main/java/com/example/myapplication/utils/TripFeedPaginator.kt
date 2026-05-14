package com.example.myapplication.utils

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.adapter.TripFeedAdapter
import com.example.myapplication.dto.trip.TripFeedItemDto
import com.example.myapplication.dto.trip.TripFeedResponseDto
import com.google.gson.Gson

class TripFeedPaginator(
    private val recyclerView: RecyclerView,
    private val adapter: TripFeedAdapter,
    private val url: String,
    private val onUi: (() -> Unit) -> Unit,
    private val onResults: (isEmpty: Boolean) -> Unit = {}
) {
    private val items = mutableListOf<TripFeedItemDto>()
    private var nextCursor: Int? = null
    private var isLoading = false
    private var hasMore = true

    init {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (rv.adapter !== adapter) return
                if (dy <= 0 || isLoading || !hasMore) return
                val lm = rv.layoutManager as? LinearLayoutManager ?: return
                if (lm.findLastVisibleItemPosition() >= lm.itemCount - 3) loadNext()
            }
        })
    }

    fun reattach(rv: RecyclerView) {
        rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (rv.adapter !== adapter) return
                if (dy <= 0 || isLoading || !hasMore) return
                val lm = rv.layoutManager as? LinearLayoutManager ?: return
                if (lm.findLastVisibleItemPosition() >= lm.itemCount - 3) loadNext()
            }
        })
    }

    fun reset() {
        nextCursor = null
        hasMore = true
        items.clear()
        adapter.submitList(emptyList())
        loadNext()
    }

    private fun loadNext() {
        if (isLoading || !hasMore) return
        isLoading = true

        val cursorPart = nextCursor?.let { "?cursor=$it" } ?: ""

        ApiClient.get("$url$cursorPart") { body, _, error ->
            onUi {
                isLoading = false
                if (error == null && body != null) {
                    try {
                        val response = Gson().fromJson(body, TripFeedResponseDto::class.java)
                        items.addAll(response.trips)
                        adapter.submitList(items.toList())
                        nextCursor = response.nextCursor
                        hasMore = response.nextCursor != null
                        onResults(items.isEmpty())
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    hasMore = false
                    onResults(items.isEmpty())
                }
            }
        }
    }
}
