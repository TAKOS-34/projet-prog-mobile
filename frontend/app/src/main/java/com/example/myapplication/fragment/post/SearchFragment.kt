package com.example.myapplication.fragment.post

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.adapter.PostsAdapter
import com.example.myapplication.utils.ApiClient
import com.example.myapplication.utils.PostFeedPaginator
import com.example.myapplication.utils.buildPostsAdapter
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import java.net.URLEncoder

class SearchFragment : Fragment() {

    private lateinit var adapter: PostsAdapter
    private lateinit var paginator: PostFeedPaginator
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var etQ: TextInputEditText
    private lateinit var etTag: TextInputEditText
    private lateinit var cgTags: ChipGroup

    private val handler = Handler(Looper.getMainLooper())
    private var suggestRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)

        recyclerView = view.findViewById(R.id.rvSearchResults)
        tvEmpty = view.findViewById(R.id.tvSearchEmpty)
        etQ = view.findViewById(R.id.etSearchQ)
        etTag = view.findViewById(R.id.etSearchTag)
        cgTags = view.findViewById(R.id.cgSearchTags)

        adapter = buildPostsAdapter(onChanged = { paginator.reset() })
        recyclerView.adapter = adapter

        paginator = PostFeedPaginator(
            recyclerView = recyclerView,
            adapter = adapter,
            baseUrl = { buildSearchUrl() },
            onUi = { block -> activity?.runOnUiThread(block) },
            onResults = { isEmpty -> renderState(isEmpty) }
        )

        val submit: () -> Unit = { performSearch() }
        etQ.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) { submit(); true } else false
        }
        etTag.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) { submit(); true } else false
        }

        setupTagSuggestions()
        fetchPopularTags()

        return view
    }

    private fun setupTagSuggestions() {
        etTag.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                suggestRunnable?.let { handler.removeCallbacks(it) }
                val query = s?.toString()?.trim().orEmpty()
                if (query.length >= 2) {
                    suggestRunnable = Runnable { fetchSuggestions(query) }
                    handler.postDelayed(suggestRunnable!!, 300)
                } else if (query.isEmpty()) {
                    fetchPopularTags()
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun fetchPopularTags() {
        ApiClient.get("tag/popular") { body, _, _ ->
            body?.let { renderTagChips(it) }
        }
    }

    private fun fetchSuggestions(query: String) {
        ApiClient.get("tag/suggest?tag=${URLEncoder.encode(query, Charsets.UTF_8.name())}") { body, _, _ ->
            body?.let { renderTagChips(it) }
        }
    }

    private fun renderTagChips(json: String) {
        activity?.runOnUiThread {
            try {
                val tags = Gson().fromJson(json, Array<String>::class.java)
                cgTags.removeAllViews()
                tags.forEach { tag ->
                    val chip = Chip(context).apply {
                        text = tag
                        isClickable = true
                        setOnClickListener {
                            etTag.setText(tag)
                            etTag.setSelection(etTag.text?.length ?: 0)
                            performSearch()
                        }
                    }
                    cgTags.addView(chip)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun buildSearchUrl(): String {
        val q = etQ.text.toString().trim()
        val tag = etTag.text.toString().trim()
        val params = mutableListOf<String>()
        if (q.isNotEmpty()) params += "q=${URLEncoder.encode(q, Charsets.UTF_8.name())}"
        if (tag.isNotEmpty()) params += "tag=${URLEncoder.encode(tag, Charsets.UTF_8.name())}"
        return if (params.isEmpty()) "post" else "post?" + params.joinToString("&")
    }

    private fun performSearch() {
        paginator.reset()
    }

    private fun renderState(isEmpty: Boolean) {
        tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
}
