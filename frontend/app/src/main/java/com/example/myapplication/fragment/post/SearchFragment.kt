package com.example.myapplication.fragment.post

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
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
import androidx.core.widget.NestedScrollView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Locale
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.adapter.PostsAdapter
import com.example.myapplication.dto.post.PostType
import com.example.myapplication.utils.ApiClient
import com.example.myapplication.utils.LocalisationSuggester
import com.example.myapplication.utils.LocalisationSuggestion
import com.example.myapplication.utils.PostFeedPaginator
import com.example.myapplication.utils.SessionManager
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
    private lateinit var nsv: NestedScrollView
    private lateinit var tvEmpty: TextView
    private var pendingScrollToResults = false
    private lateinit var etQ: TextInputEditText
    private lateinit var etTag: TextInputEditText
    private lateinit var etType: TextInputEditText
    private lateinit var etLoc: TextInputEditText
    private lateinit var etDist: TextInputEditText
    private lateinit var cgTags: ChipGroup
    private lateinit var cgLoc: ChipGroup

    private var selectedType: PostType? = null
    private var selectedDistanceKm: Int? = null
    private var ignoreNextLocChange = false

    private val handler = Handler(Looper.getMainLooper())
    private var suggestRunnable: Runnable? = null
    private var locRunnable: Runnable? = null

    private val distanceOptionsKm = listOf(5, 10, 20, 50, 100, 200, 500)

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) fetchLastKnownCity()
        else Toast.makeText(context, R.string.error_location_unavailable, Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)

        recyclerView = view.findViewById(R.id.rvSearchResults)
        tvEmpty = view.findViewById(R.id.tvSearchEmpty)
        etQ = view.findViewById(R.id.etSearchQ)
        etTag = view.findViewById(R.id.etSearchTag)
        etType = view.findViewById(R.id.etSearchType)
        etLoc = view.findViewById(R.id.etSearchLoc)
        etDist = view.findViewById(R.id.etSearchDist)
        cgTags = view.findViewById(R.id.cgSearchTags)
        cgLoc = view.findViewById(R.id.cgSearchLoc)
        setupTypeDropdown()
        setupLocSuggestions()
        setupDistanceDropdown()

        view.findViewById<MaterialButton>(R.id.btnAroundMe).setOnClickListener { onAroundMeClicked() }
        view.findViewById<MaterialButton>(R.id.btnSearch).setOnClickListener { performSearch() }

        nsv = view as NestedScrollView
        nsv.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
            val child = nsv.getChildAt(0) ?: return@OnScrollChangeListener
            val threshold = child.measuredHeight - nsv.measuredHeight - 400
            if (scrollY >= threshold) paginator.tryLoadMore()
        })

        adapter = buildPostsAdapter(onChanged = { paginator.reset() })
        recyclerView.adapter = adapter

        paginator = PostFeedPaginator(
            recyclerView = recyclerView,
            adapter = adapter,
            baseUrl = { buildSearchUrl() },
            onUi = { block -> activity?.runOnUiThread(block) },
            onResults = { isEmpty -> renderState(isEmpty) }
        )

        if (SessionManager.getUserId() != null) {
            setupTagSuggestions()
            fetchPopularTags()
        } else {
            view.findViewById<View>(R.id.tilSearchTag).visibility = View.GONE
            cgTags.visibility = View.GONE
        }

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
                        }
                    }
                    cgTags.addView(chip)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun setupTypeDropdown() {
        val labels = listOf(getString(R.string.search_type_all)) +
                PostType.entries.map { getString(it.labelRes) }

        selectedType?.let {
            etType.setText(getString(it.labelRes))
        } ?: etType.setText("")

        etType.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.search_type_hint)
                .setItems(labels.toTypedArray()) { _, position ->
                    selectedType = if (position == 0) null else PostType.entries[position - 1]
                    etType.setText(if (position == 0) "" else labels[position])
                }
                .show()
        }
    }

    private fun setupLocSuggestions() {
        etLoc.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                locRunnable?.let { handler.removeCallbacks(it) }
                if (ignoreNextLocChange) { ignoreNextLocChange = false; return }
                val q = s?.toString()?.trim().orEmpty()
                if (q.length < 2) {
                    cgLoc.removeAllViews()
                    return
                }
                locRunnable = Runnable {
                    LocalisationSuggester.suggest(q) { results ->
                        activity?.runOnUiThread { renderLocChips(results) }
                    }
                }
                handler.postDelayed(locRunnable!!, 350)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun renderLocChips(results: List<LocalisationSuggestion>) {
        cgLoc.removeAllViews()
        results.forEach { suggestion ->
            val chip = Chip(context).apply {
                text = suggestion.label
                isClickable = true
                setOnClickListener {
                    ignoreNextLocChange = true
                    etLoc.setText(suggestion.name)
                    etLoc.setSelection(etLoc.text?.length ?: 0)
                    cgLoc.removeAllViews()
                }
            }
            cgLoc.addView(chip)
        }
    }

    private fun setupDistanceDropdown() {
        val labels = listOf(getString(R.string.search_dist_any)) +
                distanceOptionsKm.map { "$it km" }

        selectedDistanceKm?.let { etDist.setText("$it km") } ?: etDist.setText("")

        etDist.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.search_dist_hint)
                .setItems(labels.toTypedArray()) { _, position ->
                    selectedDistanceKm = if (position == 0) null else distanceOptionsKm[position - 1]
                    etDist.setText(if (position == 0) "" else labels[position])
                }
                .show()
        }
    }

    private fun onAroundMeClicked() {
        val granted = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) fetchLastKnownCity()
        else locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    @SuppressLint("MissingPermission")
    private fun fetchLastKnownCity() {
        val ctx = context ?: return
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val location: Location? = lm?.let {
            it.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: runCatching { it.getLastKnownLocation(LocationManager.GPS_PROVIDER) }.getOrNull()
                ?: runCatching { it.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER) }.getOrNull()
        }
        if (location == null) {
            Toast.makeText(ctx, R.string.error_location_unavailable, Toast.LENGTH_SHORT).show()
            return
        }
        if (!Geocoder.isPresent()) {
            Toast.makeText(ctx, R.string.error_location_unavailable, Toast.LENGTH_SHORT).show()
            return
        }
        val geocoder = Geocoder(ctx, Locale.getDefault())
        try {
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            val city = addresses?.firstOrNull()?.let {
                it.locality ?: it.subAdminArea ?: it.adminArea
            }
            if (city.isNullOrBlank()) {
                Toast.makeText(ctx, R.string.error_location_unavailable, Toast.LENGTH_SHORT).show()
            } else {
                ignoreNextLocChange = true
                etLoc.setText(city)
                etLoc.setSelection(etLoc.text?.length ?: 0)
                cgLoc.removeAllViews()
            }
        } catch (e: Exception) {
            Toast.makeText(ctx, R.string.error_location_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildSearchUrl(): String {
        val q = etQ.text.toString().trim()
        val tag = etTag.text.toString().trim()
        val type = selectedType?.name
        val loc = etLoc.text.toString().trim()
        val params = mutableListOf<String>()
        if (q.isNotEmpty()) params += "q=${URLEncoder.encode(q, Charsets.UTF_8.name())}"
        if (tag.isNotEmpty()) params += "tag=${URLEncoder.encode(tag, Charsets.UTF_8.name())}"
        if (type != null) params += "type=$type"
        if (loc.isNotEmpty()) params += "loc=${URLEncoder.encode(loc, Charsets.UTF_8.name())}"
        if (loc.isNotEmpty() && selectedDistanceKm != null) params += "dist=$selectedDistanceKm"
        return if (params.isEmpty()) "post" else "post?" + params.joinToString("&")
    }

    private fun performSearch() {
        pendingScrollToResults = true
        paginator.reset()
    }

    private fun renderState(isEmpty: Boolean) {
        tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        if (pendingScrollToResults && !isEmpty) {
            pendingScrollToResults = false
            recyclerView.post { nsv.smoothScrollTo(0, recyclerView.top) }
        }
    }
}
