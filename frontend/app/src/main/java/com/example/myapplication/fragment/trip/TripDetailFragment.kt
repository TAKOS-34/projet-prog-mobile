package com.example.myapplication.fragment.trip

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
import com.example.myapplication.utils.toTripDuration
import com.example.myapplication.utils.toWeatherEmoji
import com.example.myapplication.utils.toWeatherLabel
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson

class TripDetailFragment : Fragment() {

    private val gson = Gson()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_trip_detail, container, false)

        val tripJson = arguments?.getString("tripJson") ?: return view
        val localisation = arguments?.getString("localisation") ?: ""
        val tripIndex = arguments?.getInt("tripIndex", 1) ?: 1
        val weatherJson = arguments?.getString("weatherJson")

        val trip = gson.fromJson(tripJson, TripSuggestInfosDto::class.java)
        val weather = weatherJson?.let { gson.fromJson(it, WeatherDto::class.java) }

        view.findViewById<ImageView>(R.id.btnBack)
            .setOnClickListener { findNavController().navigateUp() }

        bindTitle(view, tripIndex)
        bindMeta(view, trip, weather)

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
            }
        )

        view.findViewById<MaterialButton>(R.id.btnValidate).setOnClickListener {
            confirmTrip(trip.id, localisation)
        }

        return view
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
