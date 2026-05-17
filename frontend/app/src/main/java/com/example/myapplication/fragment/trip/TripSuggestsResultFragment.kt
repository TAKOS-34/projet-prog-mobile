package com.example.myapplication.fragment.trip

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.adapter.TripSuggestsAdapter
import com.example.myapplication.dto.trip.SuggestTripRequestDto
import com.example.myapplication.dto.trip.TripSuggestResponseDto
import com.example.myapplication.utils.ApiClient
import com.example.myapplication.utils.toWeatherEmoji
import com.example.myapplication.utils.toWeatherLabel
import com.google.gson.Gson

class TripSuggestsResultFragment : Fragment() {

    private val gson = Gson()
    private var lastRefreshTime = 0L
    private val cooldownMs = 2 * 60 * 1000L

    private lateinit var rv: RecyclerView
    private lateinit var btnRefresh: ImageView
    private lateinit var localisation: String
    private var requestDto: SuggestTripRequestDto? = null
    private var startLat = 0.0
    private var startLong = 0.0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_trip_suggests_result, container, false)

        val tripsJson = arguments?.getString("tripsJson") ?: return view
        localisation = arguments?.getString("localisation") ?: ""
        startLat = arguments?.getDouble("startLat") ?: 0.0
        startLong = arguments?.getDouble("startLong") ?: 0.0
        val requestJson = arguments?.getString("requestJson")
        requestDto = requestJson?.let { gson.fromJson(it, SuggestTripRequestDto::class.java) }

        rv = view.findViewById(R.id.rvTrips)
        btnRefresh = view.findViewById(R.id.btnRefresh)

        view.findViewById<ImageView>(R.id.btnBack)
            .setOnClickListener { findNavController().navigateUp() }

        val response = gson.fromJson(tripsJson, TripSuggestResponseDto::class.java)
        bindWeather(view, response)
        bindList(response)

        btnRefresh.setOnClickListener { onRefreshClicked(view) }

        return view
    }

    private fun onRefreshClicked(view: View) {
        val dto = requestDto ?: return
        val now = System.currentTimeMillis()
        val elapsed = now - lastRefreshTime

        if (lastRefreshTime > 0 && elapsed < cooldownMs) {
            val remaining = ((cooldownMs - elapsed) / 1000).toInt()
            Toast.makeText(context,
                getString(R.string.trip_regenerate_timer, remaining), Toast.LENGTH_SHORT).show()
            return
        }

        btnRefresh.isEnabled = false
        btnRefresh.alpha = 0.4f

        ApiClient.post("trip/suggest", dto.copy(isRegenerated = true)) { body, _, error ->
            activity?.runOnUiThread {
                btnRefresh.isEnabled = true
                btnRefresh.alpha = 1f
                if (error != null || body == null) {
                    Toast.makeText(context, error ?: getString(R.string.error_network), Toast.LENGTH_LONG).show()
                } else {
                    lastRefreshTime = System.currentTimeMillis()
                    val response = gson.fromJson(body, TripSuggestResponseDto::class.java)
                    bindWeather(view, response)
                    bindList(response)
                }
            }
        }
    }

    private fun bindList(response: TripSuggestResponseDto) {
        rv.layoutManager = rv.layoutManager ?: LinearLayoutManager(requireContext())
        rv.adapter = TripSuggestsAdapter(response.trips, response.weather) { trip, index ->
            val bundle = Bundle().apply {
                putString("tripJson", gson.toJson(trip))
                putString("localisation", localisation)
                putInt("tripIndex", index + 1)
                putString("weatherJson", gson.toJson(response.weather))
                putDouble("startLat", startLat)
                putDouble("startLong", startLong)
                putString("startingTime", requestDto?.startingTime)
            }
            findNavController().navigate(R.id.action_tripResult_to_tripDetail, bundle)
        }
    }

    private fun bindWeather(view: View, response: TripSuggestResponseDto) {
        val ctx = requireContext()
        view.findViewById<TextView>(R.id.tvWeatherEmoji).text =
            response.weather.code.toWeatherEmoji()
        view.findViewById<TextView>(R.id.tvWeatherLabel).text =
            response.weather.code.toWeatherLabel(ctx)
        view.findViewById<TextView>(R.id.tvWeatherTemp).text =
            getString(R.string.trip_result_temperature, response.weather.temperature)
    }
}
