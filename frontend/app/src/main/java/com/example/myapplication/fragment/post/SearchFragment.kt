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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Locale
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.myapplication.R
import com.example.myapplication.adapter.PostsAdapter
import com.example.myapplication.dto.post.PostDto
import com.example.myapplication.dto.post.PostType
import com.example.myapplication.dto.post.PostsResponseDto
import com.example.myapplication.utils.ApiClient
import com.example.myapplication.utils.LocalisationFormat
import com.example.myapplication.utils.LocalisationSuggester
import com.example.myapplication.utils.LocalisationSuggestion
import com.example.myapplication.utils.PostFeedPaginator
import com.example.myapplication.utils.SessionManager
import com.example.myapplication.utils.buildPostsAdapter
import com.example.myapplication.utils.resolveBackendUrl
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import java.net.URLEncoder
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class SearchFragment : Fragment() {

    private lateinit var adapter: PostsAdapter
    private lateinit var paginator: PostFeedPaginator
    private lateinit var recyclerView: RecyclerView
    private lateinit var nsv: NestedScrollView
    private lateinit var nsvSearch: NestedScrollView
    private lateinit var mapView: MapView
    private lateinit var tvEmpty: TextView
    private var pendingScrollToResults = false
    private lateinit var etQ: TextInputEditText
    private lateinit var etTag: TextInputEditText
    private lateinit var etType: TextInputEditText
    private lateinit var etLoc: TextInputEditText
    private lateinit var etDist: TextInputEditText
    private lateinit var cgTags: ChipGroup
    private lateinit var cgLoc: ChipGroup

    private lateinit var llFilters: LinearLayout
    private lateinit var llFilterToggle: View
    private lateinit var ivFilterChevron: ImageView
    private var filtersExpanded = false

    private var selectedType: PostType? = null
    private var selectedDistanceKm: Int? = null
    private var ignoreNextLocChange = false
    private var popularTagsCache: List<String> = emptyList()
    private var mapInitialized = false

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

        nsvSearch = view.findViewById(R.id.nsvSearch)
        mapView = view.findViewById(R.id.mapView)

        recyclerView = view.findViewById(R.id.rvSearchResults)
        tvEmpty = view.findViewById(R.id.tvSearchEmpty)
        etQ = view.findViewById(R.id.etSearchQ)
        etTag = view.findViewById(R.id.etSearchTag)
        etType = view.findViewById(R.id.etSearchType)
        etLoc = view.findViewById(R.id.etSearchLoc)
        etDist = view.findViewById(R.id.etSearchDist)
        cgTags = view.findViewById(R.id.cgSearchTags)
        cgLoc = view.findViewById(R.id.cgSearchLoc)
        llFilters = view.findViewById(R.id.llFilters)
        llFilterToggle = view.findViewById(R.id.llFilterToggle)
        ivFilterChevron = view.findViewById(R.id.ivFilterChevron)

        llFilterToggle.setOnClickListener {
            filtersExpanded = !filtersExpanded
            llFilters.visibility = if (filtersExpanded) View.VISIBLE else View.GONE
            ivFilterChevron.animate().rotation(if (filtersExpanded) 180f else 0f).setDuration(200).start()
        }

        setupTypeDropdown()
        setupLocSuggestions()
        setupDistanceDropdown()

        view.findViewById<MaterialButton>(R.id.btnAroundMe).setOnClickListener { onAroundMeClicked() }
        view.findViewById<MaterialButton>(R.id.btnSearch).setOnClickListener { performSearch() }

        nsv = nsvSearch
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

        view.findViewById<MaterialButtonToggleGroup>(R.id.tgSearchTabs)
            .addOnButtonCheckedListener { _, checkedId, isChecked ->
                if (!isChecked) return@addOnButtonCheckedListener
                when (checkedId) {
                    R.id.btnTabSearch -> showSearchTab()
                    R.id.btnTabMap -> showMapTab()
                }
            }
        view.findViewById<MaterialButtonToggleGroup>(R.id.tgSearchTabs).check(R.id.btnTabSearch)

        return view
    }

    override fun onResume() {
        super.onResume()
        if (mapInitialized) mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        if (mapInitialized) mapView.onPause()
    }

    private fun showSearchTab() {
        nsvSearch.visibility = View.VISIBLE
        mapView.visibility = View.GONE
    }

    private fun showMapTab() {
        nsvSearch.visibility = View.GONE
        mapView.visibility = View.VISIBLE
        if (!mapInitialized) {
            mapInitialized = true
            initMap()
        }
    }

    @SuppressLint("MissingPermission")
    private fun initMap() {
        val ctx = requireContext()
        Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = ctx.packageName

        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(6.0)
        mapView.controller.setCenter(GeoPoint(46.2276, 2.2137))

        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val hasCoarse = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasCoarse && lm != null) {
            val loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: runCatching { lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) }.getOrNull()
            if (loc != null) {
                val userPos = GeoPoint(loc.latitude, loc.longitude)
                mapView.controller.setZoom(12.0)
                mapView.controller.setCenter(userPos)
                val userMarker = Marker(mapView).apply {
                    position = userPos
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = "Ma position"
                    icon = ContextCompat.getDrawable(ctx, R.drawable.ic_location)
                }
                mapView.overlays.add(userMarker)
            }
        }

        loadPostsOnMap()
    }

    private fun loadPostsOnMap() {
        ApiClient.get("post") { body, _, _ ->
            body?.let { json ->
                try {
                    val response = Gson().fromJson(json, PostsResponseDto::class.java)
                    activity?.runOnUiThread { addPostMarkers(response.posts) }
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }

    private fun addPostMarkers(posts: List<PostDto>) {
        val ctx = context ?: return
        posts.forEach { post ->
            if (post.lat == 0.0 && post.long == 0.0) return@forEach
            val marker = Marker(mapView).apply {
                position = GeoPoint(post.lat, post.long)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = post.title
                icon = ContextCompat.getDrawable(ctx, R.drawable.ic_location)
                setOnMarkerClickListener { _, _ ->
                    showPostBottomSheet(post)
                    true
                }
            }
            mapView.overlays.add(marker)
        }
        mapView.invalidate()
    }

    private fun showPostBottomSheet(post: PostDto) {
        val ctx = context ?: return
        val dialog = BottomSheetDialog(ctx)
        val sheetView = LayoutInflater.from(ctx).inflate(R.layout.bottom_sheet_map_post, null)

        sheetView.findViewById<ShapeableImageView>(R.id.ivMapPostImage).load(post.image.resolveBackendUrl()) {
            crossfade(true)
            placeholder(R.drawable.ic_launcher_background)
        }
        sheetView.findViewById<TextView>(R.id.tvMapPostTitle).text = post.title
        val typeLabel = PostType.fromApiValue(post.type)?.let { getString(it.labelRes) } ?: post.type
        sheetView.findViewById<TextView>(R.id.tvMapPostMeta).text =
            getString(R.string.map_post_by, post.username) + " · $typeLabel"
        sheetView.findViewById<TextView>(R.id.tvMapPostLocation).text =
            LocalisationFormat.display(post.localisation)
        sheetView.findViewById<TextView>(R.id.tvMapPostLikes).text = post.nbLikes.toString()
        sheetView.findViewById<TextView>(R.id.tvMapPostComments).text = post.nbComments.toString()

        sheetView.setOnClickListener {
            dialog.dismiss()
            val bundle = Bundle().apply { putString("postId", post.id) }
            findNavController().navigate(R.id.postViewerFragment, bundle)
        }

        dialog.setContentView(sheetView)
        dialog.show()
    }

    private fun setupTagSuggestions() {
        etTag.filters = arrayOf(android.text.InputFilter { source, start, end, _, _, _ ->
            val filtered = StringBuilder()
            var changed = false
            for (i in start until end) {
                val c = source[i]
                if (c.isLetterOrDigit() || c == '-' || c == '_' || c == ',') filtered.append(c)
                else changed = true
            }
            if (changed) filtered.toString() else null
        })

        etTag.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                suggestRunnable?.let { handler.removeCallbacks(it) }
                val query = s?.toString()?.split(",")?.lastOrNull()?.trim() ?: ""
                if (query.length >= 2) {
                    suggestRunnable = Runnable { fetchSuggestions(query) }
                    handler.postDelayed(suggestRunnable!!, 300)
                }
            }
            override fun afterTextChanged(s: Editable?) {
                val currentText = s?.toString() ?: return
                val committedTags = if (currentText.endsWith(","))
                    currentText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                else
                    currentText.split(",").map { it.trim() }.dropLast(1).filter { it.isNotEmpty() }
                for (i in 0 until cgTags.childCount) {
                    val chip = cgTags.getChildAt(i) as? Chip ?: continue
                    chip.isChecked = committedTags.contains(chip.text.toString())
                }
            }
        })

        etTag.imeOptions = EditorInfo.IME_ACTION_DONE
        etTag.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val current = etTag.text.toString()
                if (current.isNotEmpty() && !current.endsWith(",")) etTag.append(",")
                true
            } else false
        }
    }

    private fun fetchPopularTags() {
        ApiClient.get("tag/popular") { body, _, _ ->
            body?.let { json ->
                try { popularTagsCache = Gson().fromJson(json, Array<String>::class.java).toList() } catch (e: Exception) {}
                updateTagChips(json)
            }
        }
    }

    private fun fetchSuggestions(query: String) {
        ApiClient.get("tag/suggest?tag=${URLEncoder.encode(query, Charsets.UTF_8.name())}") { body, _, _ ->
            body?.let { updateTagChips(it) }
        }
    }

    private fun updateTagChips(json: String) {
        activity?.runOnUiThread {
            try {
                val suggestions = Gson().fromJson(json, Array<String>::class.java).toMutableList()
                popularTagsCache.forEach { if (!suggestions.contains(it)) suggestions.add(it) }
                val currentText = etTag.text.toString()
                val committedTags = if (currentText.endsWith(","))
                    currentText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                else
                    currentText.split(",").map { it.trim() }.dropLast(1).filter { it.isNotEmpty() }
                committedTags.forEach { if (!suggestions.contains(it)) suggestions.add(it) }
                val partial = if (!currentText.endsWith(",")) currentText.split(",").lastOrNull()?.trim() ?: "" else ""
                if (partial.isNotEmpty() && !suggestions.contains(partial)) suggestions.add(0, partial)
                cgTags.removeAllViews()
                suggestions.forEach { tag ->
                    val chip = Chip(context).apply {
                        text = tag
                        isClickable = true
                        isCheckable = true
                        isChecked = committedTags.contains(tag)
                        setOnClickListener { addTagToInput(tag) }
                    }
                    cgTags.addView(chip)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun addTagToInput(tag: String) {
        val currentText = etTag.text.toString()
        val committed = if (currentText.endsWith(","))
            currentText.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
        else
            currentText.split(",").map { it.trim() }.dropLast(1).filter { it.isNotEmpty() }.toMutableList()
        if (committed.contains(tag)) committed.remove(tag) else committed.add(tag)
        etTag.setText(if (committed.isEmpty()) "" else committed.joinToString(",") + ",")
        etTag.setSelection(etTag.text?.length ?: 0)
        for (i in 0 until cgTags.childCount) {
            val chip = cgTags.getChildAt(i) as? Chip ?: continue
            chip.isChecked = committed.contains(chip.text.toString())
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
                    etLoc.setText(suggestion.label.lowercase())
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
        val tagText = etTag.text.toString()
        val confirmedTags = if (tagText.endsWith(","))
            tagText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        else
            tagText.split(",").map { it.trim() }.dropLast(1).filter { it.isNotEmpty() }
        val type = selectedType?.name
        val loc = etLoc.text.toString().trim()
        val params = mutableListOf<String>()
        if (q.isNotEmpty()) params += "q=${URLEncoder.encode(q, Charsets.UTF_8.name())}"
        if (confirmedTags.isNotEmpty()) {
            val tagsParam = confirmedTags.joinToString("&") {
                "tag=${URLEncoder.encode(it, Charsets.UTF_8.name())}"
            }
            params += tagsParam
        }
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
