package com.example.myapplication.utils

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.adapter.NotificationsAdapter
import com.example.myapplication.dto.notification.NotificationDto
import com.example.myapplication.dto.notification.NotificationsResponseDto
import com.google.gson.Gson

class NotificationsPaginator(
    private val recyclerView: RecyclerView,
    private val adapter: NotificationsAdapter,
    private val onUi: (() -> Unit) -> Unit,
    private val onResults: (isEmpty: Boolean) -> Unit = {},
    private val onPageLoaded: (List<NotificationDto>) -> Unit = {}
) {
    private val items = mutableListOf<NotificationDto>()
    private var nextCursor: Int? = null
    private var isLoading = false
    private var hasMore = true

    init {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
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

    fun markItemsAsRead(ids: Set<Int>) {
        if (ids.isEmpty()) return
        for (i in items.indices) {
            if (items[i].id in ids && !items[i].isRead) {
                items[i] = items[i].copy(isRead = true)
            }
        }
        adapter.submitList(items.toList())
    }

    private fun loadNext() {
        if (isLoading || !hasMore) return
        isLoading = true

        val path = nextCursor?.let { "notification?cursor=$it" } ?: "notification"

        ApiClient.get(path) { body, _, error ->
            onUi {
                isLoading = false
                if (error == null && body != null) {
                    try {
                        val response = Gson().fromJson(body, NotificationsResponseDto::class.java)
                        items.addAll(response.notifications)
                        adapter.submitList(items.toList())
                        nextCursor = response.nextCursor
                        hasMore = response.nextCursor != null
                        onResults(items.isEmpty())
                        onPageLoaded(response.notifications)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}
