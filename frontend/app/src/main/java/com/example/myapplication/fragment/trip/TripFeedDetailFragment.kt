package com.example.myapplication.fragment.trip

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.myapplication.R
import com.example.myapplication.adapter.TripStepsAdapter
import com.example.myapplication.dto.trip.TripFeedItemDto
import com.example.myapplication.fragment.post.LocalisationViewerFragment
import com.example.myapplication.utils.ApiClient
import com.example.myapplication.utils.DateUtils
import com.example.myapplication.utils.exportTripToPdf
import com.example.myapplication.utils.LocalisationFormat
import com.example.myapplication.utils.resolveBackendUrl
import com.example.myapplication.utils.SessionManager
import com.example.myapplication.utils.toTripDuration
import com.example.myapplication.utils.toWeatherEmoji
import com.example.myapplication.utils.toWeatherLabel
import com.google.gson.Gson
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

class TripFeedDetailFragment : Fragment() {

    private val gson = Gson()

    private lateinit var mapView: MapView
    private var mapVisible = false
    private var mapInitialized = false

    private var isLiked = false
    private var likeCount = 0
    private var isBookmarked = false
    private var tripId = 0

    private lateinit var btnLike: ImageView
    private lateinit var tvLikeCount: TextView
    private lateinit var btnBookmark: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_trip_feed_detail, container, false)

        view.findViewById<ImageView>(R.id.btnBack)
            .setOnClickListener { findNavController().navigateUp() }

        val tripJson = arguments?.getString("tripFeedJson")
        if (tripJson != null) {
            val trip = gson.fromJson(tripJson, TripFeedItemDto::class.java)
            bindTrip(view, trip)
        } else {
            val id = arguments?.getInt("tripId") ?: 0
            if (id > 0) fetchAndBind(view, id)
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        if (::mapView.isInitialized) mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        if (::mapView.isInitialized) mapView.onPause()
    }

    private fun bindMap(view: View, trip: TripFeedItemDto) {
        val ctx = requireContext()
        mapView = view.findViewById(R.id.mvTripRoute)
        val llMapToggle = view.findViewById<View>(R.id.llMapToggle)
        val tvMapToggle = view.findViewById<TextView>(R.id.tvMapToggle)
        val ivChevron = view.findViewById<ImageView>(R.id.ivMapToggleChevron)

        val geoPoints = mutableListOf<GeoPoint>().also { pts ->
            trip.startLocalisation?.let { pts.add(GeoPoint(it.lat, it.long)) }
            trip.steps.forEach { pts.add(GeoPoint(it.localisation.lat, it.localisation.long)) }
        }

        llMapToggle.setOnClickListener {
            mapVisible = !mapVisible
            mapView.visibility = if (mapVisible) View.VISIBLE else View.GONE
            ivChevron.rotation = if (mapVisible) 180f else 0f
            tvMapToggle.text = getString(if (mapVisible) R.string.trip_hide_map else R.string.trip_show_map)

            if (mapVisible && !mapInitialized) {
                mapInitialized = true
                initMap(ctx, geoPoints, trip)
            }
        }
    }

    private fun initMap(ctx: Context, geoPoints: List<GeoPoint>, trip: TripFeedItemDto) {
        Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = ctx.packageName

        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        trip.startLocalisation?.let { loc ->
            mapView.overlays.add(Marker(mapView).apply {
                position = GeoPoint(loc.lat, loc.long)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "${getString(R.string.trip_starting_point)} : ${LocalisationFormat.display(loc.name)}"
                icon = ContextCompat.getDrawable(ctx, R.drawable.ic_location)
            })
        }

        trip.steps.forEachIndexed { i, step ->
            mapView.overlays.add(Marker(mapView).apply {
                position = GeoPoint(step.localisation.lat, step.localisation.long)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "${i + 1}. ${LocalisationFormat.display(step.localisation.name)}"
                icon = ContextCompat.getDrawable(ctx, R.drawable.ic_location)
            })
        }

        if (geoPoints.size >= 2) {
            mapView.overlays.add(0, Polyline(mapView).apply {
                setPoints(geoPoints)
                outlinePaint.color = ContextCompat.getColor(ctx, R.color.primary)
                outlinePaint.strokeWidth = 6f
            })
        }

        mapView.post {
            when {
                geoPoints.isEmpty() -> {
                    mapView.controller.setZoom(6.0)
                    mapView.controller.setCenter(GeoPoint(46.2276, 2.2137))
                }
                geoPoints.size == 1 -> {
                    mapView.controller.setZoom(13.0)
                    mapView.controller.setCenter(geoPoints[0])
                }
                else -> {
                    val bbox = BoundingBox(
                        geoPoints.maxOf { it.latitude },
                        geoPoints.maxOf { it.longitude },
                        geoPoints.minOf { it.latitude },
                        geoPoints.minOf { it.longitude }
                    )
                    mapView.zoomToBoundingBox(bbox, true, 80)
                }
            }
        }

        mapView.invalidate()
    }

    private fun bindTrip(view: View, trip: TripFeedItemDto) {
        tripId = trip.id
        isLiked = trip.isLiked
        likeCount = trip.nbLikes
        isBookmarked = trip.isBookmarked
        bindHeader(view, trip)
        bindMeta(view, trip)
        bindMap(view, trip)
        bindSteps(view, trip)
        bindActions(view, trip)
    }

    private fun fetchAndBind(view: View, id: Int) {
        ApiClient.get("trip/$id") { body, _, error ->
            activity?.runOnUiThread {
                if (error == null && body != null) {
                    try {
                        val trip = gson.fromJson(body, TripFeedItemDto::class.java)
                        bindTrip(view, trip)
                    } catch (e: Exception) {
                        findNavController().navigateUp()
                    }
                } else {
                    findNavController().navigateUp()
                }
            }
        }
    }

    private fun bindHeader(view: View, trip: TripFeedItemDto) {
        val ctx = requireContext()
        view.findViewById<ImageView>(R.id.ivFeedDetailAvatar).load(trip.avatar.resolveBackendUrl()) {
            crossfade(true)
            transformations(CircleCropTransformation())
        }
        view.findViewById<TextView>(R.id.tvFeedDetailUsername).text = trip.username
        view.findViewById<TextView>(R.id.tvFeedDetailDate).text =
            DateUtils.formatRelativeDate(ctx, trip.creationDate)
    }

    private fun bindMeta(view: View, trip: TripFeedItemDto) {
        val ctx = requireContext()
        view.findViewById<TextView>(R.id.tvFeedDetailWeather).text =
            "${trip.weather.toWeatherEmoji()} ${trip.weather.toWeatherLabel(ctx)}"
        view.findViewById<TextView>(R.id.tvFeedDetailDuration).text =
            trip.totalDuration.toTripDuration(ctx)
        view.findViewById<TextView>(R.id.tvFeedDetailCost).text =
            ctx.getString(R.string.trip_total_cost, trip.totalCost)
        val dist = trip.totalDistance
        view.findViewById<TextView>(R.id.tvFeedDetailDistanceSep).visibility =
            if (dist != null) View.VISIBLE else View.GONE
        val tvDist = view.findViewById<TextView>(R.id.tvFeedDetailDistance)
        if (dist != null) {
            tvDist.visibility = View.VISIBLE
            tvDist.text = DateUtils.formatDistance(dist)
        } else {
            tvDist.visibility = View.GONE
        }
    }

    private fun bindSteps(view: View, trip: TripFeedItemDto) {
        val rv = view.findViewById<RecyclerView>(R.id.rvFeedDetailSteps)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = TripStepsAdapter(
            steps = trip.steps,
            onPostClick = { postId ->
                val bundle = Bundle().apply { putString("postId", postId) }
                findNavController().navigate(R.id.postViewerFragment, bundle)
            },
            onLocationClick = { name, lat, long ->
                val bundle = Bundle().apply {
                    putString(LocalisationViewerFragment.ARG_LOCALISATION, name)
                    putFloat(LocalisationViewerFragment.ARG_LAT, lat.toFloat())
                    putFloat(LocalisationViewerFragment.ARG_LONG, long.toFloat())
                }
                findNavController().navigate(R.id.localisationViewerFragment, bundle)
            },
            startLocationName = trip.startLocalisation?.name
        )
    }

    private fun bindActions(view: View, trip: TripFeedItemDto) {
        btnLike = view.findViewById(R.id.btnFeedDetailLike)
        tvLikeCount = view.findViewById(R.id.tvFeedDetailLikeCount)
        btnBookmark = view.findViewById(R.id.btnFeedDetailBookmark)

        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnExportPdf)
            .setOnClickListener { exportTripToPdf(trip) }

        val isLogged = SessionManager.getUserId() != null

        if (!isLogged) {
            btnLike.visibility = View.GONE
            tvLikeCount.visibility = View.GONE
            btnBookmark.visibility = View.GONE
        } else {
            applyLikeState()
            applyBookmarkState()
        }

        btnLike.setOnClickListener {
            if (!isLogged) return@setOnClickListener
            val newLiked = !isLiked
            val endpoint = "like/trip/$tripId"
            if (newLiked) ApiClient.post(endpoint, emptyMap<String, Any>()) { _, _, _ -> }
            else ApiClient.delete(endpoint) { _, _, _ -> }
            isLiked = newLiked
            likeCount += if (isLiked) 1 else -1
            applyLikeState()
        }

        btnBookmark.setOnClickListener {
            if (!isLogged) return@setOnClickListener
            val newBookmarked = !isBookmarked
            val endpoint = "bookmark/trip/$tripId"
            if (newBookmarked) {
                ApiClient.post(endpoint, emptyMap<String, Any>()) { _, _, _ -> }
            } else {
                ApiClient.delete(endpoint) { _, _, _ -> }
            }
            isBookmarked = newBookmarked
            applyBookmarkState()
        }
    }

    private fun applyLikeState() {
        val ctx = requireContext()
        btnLike.setImageResource(if (isLiked) R.drawable.ic_like_filled else R.drawable.ic_like)
        btnLike.setColorFilter(
            ContextCompat.getColor(ctx, if (isLiked) R.color.primary else R.color.text_secondary)
        )
        tvLikeCount.text = likeCount.toString()
    }

    private fun applyBookmarkState() {
        val ctx = requireContext()
        btnBookmark.setImageResource(if (isBookmarked) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark)
        btnBookmark.setColorFilter(
            ContextCompat.getColor(ctx, if (isBookmarked) R.color.primary else R.color.text_secondary)
        )
    }

}
