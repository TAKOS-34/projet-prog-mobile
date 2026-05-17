package com.example.myapplication.fragment.trip

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.adapter.TripStepsAdapter
import com.example.myapplication.dto.trip.TripSuggestInfosDto
import com.example.myapplication.dto.trip.WeatherDto
import com.example.myapplication.fragment.post.LocalisationViewerFragment
import com.example.myapplication.utils.ApiClient
import com.example.myapplication.utils.LocalisationFormat
import com.example.myapplication.utils.toTripDuration
import com.example.myapplication.utils.toWeatherEmoji
import com.example.myapplication.utils.toWeatherLabel
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

class TripDetailFragment : Fragment() {

    private val gson = Gson()
    private lateinit var mapView: MapView
    private var mapVisible = false
    private var mapInitialized = false

    override fun onResume() {
        super.onResume()
        if (::mapView.isInitialized) mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        if (::mapView.isInitialized) mapView.onPause()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_trip_detail, container, false)

        val tripJson = arguments?.getString("tripJson") ?: return view
        val localisation = arguments?.getString("localisation") ?: ""
        val tripIndex = arguments?.getInt("tripIndex", 1) ?: 1
        val weatherJson = arguments?.getString("weatherJson")
        val startLat = arguments?.getDouble("startLat") ?: 0.0
        val startLong = arguments?.getDouble("startLong") ?: 0.0
        val startingTime = arguments?.getString("startingTime")

        val trip = gson.fromJson(tripJson, TripSuggestInfosDto::class.java)
        val weather = weatherJson?.let { gson.fromJson(it, WeatherDto::class.java) }

        view.findViewById<ImageView>(R.id.btnBack)
            .setOnClickListener { findNavController().navigateUp() }

        bindTitle(view, tripIndex)
        bindMeta(view, trip, weather)
        bindMap(view, trip, localisation, startLat, startLong)

        val rv = view.findViewById<RecyclerView>(R.id.rvSteps)
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
            startLocationName = localisation.ifBlank { null },
            startLocationLat = startLat,
            startLocationLong = startLong,
            startingTime = startingTime
        )

        view.findViewById<MaterialButton>(R.id.btnValidate).setOnClickListener {
            confirmTrip(trip.id, localisation)
        }

        return view
    }

    private fun bindMap(view: View, trip: TripSuggestInfosDto, locName: String, startLat: Double, startLong: Double) {
        val ctx = requireContext()
        mapView = view.findViewById(R.id.mvTripRoute)
        val llMapToggle = view.findViewById<View>(R.id.llMapToggle)
        val tvMapToggle = view.findViewById<TextView>(R.id.tvMapToggle)
        val ivChevron = view.findViewById<ImageView>(R.id.ivMapToggleChevron)

        val geoPoints = mutableListOf<GeoPoint>().also { pts ->
            if (startLat != 0.0 || startLong != 0.0) pts.add(GeoPoint(startLat, startLong))
            trip.steps.forEach { pts.add(GeoPoint(it.localisation.lat, it.localisation.long)) }
        }

        llMapToggle.setOnClickListener {
            mapVisible = !mapVisible
            mapView.visibility = if (mapVisible) View.VISIBLE else View.GONE
            ivChevron.rotation = if (mapVisible) 180f else 0f
            tvMapToggle.text = getString(if (mapVisible) R.string.trip_hide_map else R.string.trip_show_map)

            if (mapVisible && !mapInitialized) {
                mapInitialized = true
                initMap(ctx, geoPoints, trip, locName, startLat, startLong)
            }
        }
    }

    private fun initMap(ctx: Context, geoPoints: List<GeoPoint>, trip: TripSuggestInfosDto, locName: String, startLat: Double, startLong: Double) {
        Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = ctx.packageName

        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        if (startLat != 0.0 || startLong != 0.0) {
            mapView.overlays.add(Marker(mapView).apply {
                position = GeoPoint(startLat, startLong)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "${getString(R.string.trip_starting_point)} : ${LocalisationFormat.display(locName)}"
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

    private fun bindTitle(view: View, tripIndex: Int) {
        val isNormal = tripIndex == 1
        val label = if (isNormal) getString(R.string.trip_label_normal) else getString(R.string.trip_label_business)
        val color = if (isNormal) ContextCompat.getColor(requireContext(), R.color.secondary)
                    else Color.parseColor("#C8860A")
        view.findViewById<TextView>(R.id.tvDetailTitle).apply {
            text = label
            setTextColor(color)
        }
    }

    private fun bindMeta(view: View, trip: TripSuggestInfosDto, weather: WeatherDto?) {
        val ctx = requireContext()
        view.findViewById<TextView>(R.id.tvDetailDuration).text =
            trip.totalDuration.toTripDuration(ctx)
        view.findViewById<TextView>(R.id.tvDetailCost).text =
            getString(R.string.trip_total_cost, trip.totalCost)

        if (weather != null) {
            view.findViewById<TextView>(R.id.tvDetailWeather).text =
                getString(
                    R.string.trip_format_meteo,
                    weather.code.toWeatherEmoji(),
                    getString(R.string.trip_result_temperature, weather.temperature),
                    weather.code.toWeatherLabel(ctx)
                )
        }
    }

    private fun confirmTrip(tripId: Int?, localisation: String) {
        if (tripId == null) {
            Toast.makeText(context, R.string.error_trip_confirm, Toast.LENGTH_SHORT).show()
            return
        }

        val body = mapOf("localisation" to localisation)
        ApiClient.post("trip/confirm/$tripId", body) { _, code, error ->
            activity?.runOnUiThread {
                if (error != null || code >= 400) {
                    Toast.makeText(context, R.string.error_trip_confirm, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, R.string.success_trip_confirmed, Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.nav_menu)
                }
            }
        }
    }
}
