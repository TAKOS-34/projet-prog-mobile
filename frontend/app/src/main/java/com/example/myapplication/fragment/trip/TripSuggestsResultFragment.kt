package com.example.myapplication.fragment.trip

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.adapter.TripSuggestsAdapter
import com.example.myapplication.dto.trip.TripSuggestResponseDto
import com.example.myapplication.utils.toWeatherEmoji
import com.example.myapplication.utils.toWeatherLabel
import com.google.gson.Gson

class TripSuggestsResultFragment : Fragment() {

    private val gson = Gson()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_trip_suggests_result, container, false)

        val tripsJson = arguments?.getString("tripsJson") ?: return view
        val localisation = arguments?.getString("localisation") ?: ""
        val response = gson.fromJson(tripsJson, TripSuggestResponseDto::class.java)

        view.findViewById<ImageView>(R.id.btnBack)
            .setOnClickListener { findNavController().navigateUp() }

        bindWeather(view, response)

        val rv = view.findViewById<RecyclerView>(R.id.rvTrips)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = TripSuggestsAdapter(response.trips, response.weather) { trip, index ->
            val bundle = Bundle().apply {
                putString("tripJson", gson.toJson(trip))
                putString("localisation", localisation)
                putInt("tripIndex", index + 1)
                putString("weatherJson", gson.toJson(response.weather))
            }
            findNavController().navigate(R.id.action_tripResult_to_tripDetail, bundle)
        }

        return view
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
