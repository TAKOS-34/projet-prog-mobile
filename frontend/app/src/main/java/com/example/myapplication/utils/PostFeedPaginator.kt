package com.example.myapplication.utils

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.adapter.PostsAdapter
import com.example.myapplication.dto.post.PostDto
import com.example.myapplication.dto.post.PostsResponseDto
import com.google.gson.Gson
import java.net.URLEncoder

class PostFeedPaginator(
    private val recyclerView: RecyclerView,
    private val adapter: PostsAdapter,
    private val baseUrl: () -> String,
    private val onUi: (() -> Unit) -> Unit,
    private val onResults: (isEmpty: Boolean) -> Unit = {}
) {
    private val items = mutableListOf<PostDto>()
    private var nextCursor: String? = null
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

    private fun loadNext() {
        if (isLoading || !hasMore) return
        isLoading = true

        val url = baseUrl()
        val cursorPart = nextCursor?.takeIf { it.isNotEmpty() }?.let {
            val sep = if (url.contains('?')) '&' else '?'
            "${sep}cursor=${URLEncoder.encode(it, Charsets.UTF_8.name())}"
        } ?: ""

        ApiClient.get("$url$cursorPart") { body, _, error ->
            onUi {
                isLoading = false
                if (error == null && body != null) {
                    try {
                        val response = Gson().fromJson(body, PostsResponseDto::class.java)
                        items.addAll(response.posts)
                        adapter.submitList(items.toList())
                        nextCursor = response.nextCursor
                        hasMore = !response.nextCursor.isNullOrEmpty()
                        onResults(items.isEmpty())
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}
