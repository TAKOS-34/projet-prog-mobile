package com.example.myapplication.utils

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.myapplication.R
import com.example.myapplication.adapter.TripFeedAdapter
import com.example.myapplication.dto.trip.TripFeedItemDto
import com.example.myapplication.fragment.post.LocalisationViewerFragment
import com.google.gson.Gson

fun Fragment.buildTripFeedAdapter(onDeleted: (TripFeedItemDto) -> Unit): TripFeedAdapter = TripFeedAdapter(
    onLike = { trip, liked -> toggleTripLike(trip, liked) },
    onBookmark = { trip, bookmarked -> toggleTripBookmark(trip, bookmarked) },
    onClick = { trip -> openTripDetail(trip) },
    onStartLocationClick = { name, lat, long -> openLocalisationViewer(name, lat, long) },
    onDelete = { trip -> confirmDeleteTrip(trip) { onDeleted(trip) } }
)

private fun Fragment.openTripDetail(trip: TripFeedItemDto) {
    val bundle = Bundle().apply { putString("tripFeedJson", Gson().toJson(trip)) }
    findNavController().navigate(R.id.tripFeedDetailFragment, bundle)
}

private fun Fragment.openLocalisationViewer(name: String, lat: Double, long: Double) {
    val bundle = Bundle().apply {
        putString(LocalisationViewerFragment.ARG_LOCALISATION, name)
        putFloat(LocalisationViewerFragment.ARG_LAT, lat.toFloat())
        putFloat(LocalisationViewerFragment.ARG_LONG, long.toFloat())
    }
    findNavController().navigate(R.id.localisationViewerFragment, bundle)
}

private fun toggleTripLike(trip: TripFeedItemDto, liked: Boolean) {
    val endpoint = "like/trip/${trip.id}"
    if (liked) ApiClient.post(endpoint, emptyMap<String, Any>()) { _, _, _ -> }
    else ApiClient.delete(endpoint) { _, _, _ -> }
}

private fun toggleTripBookmark(trip: TripFeedItemDto, bookmarked: Boolean) {
    val endpoint = "bookmark/trip/${trip.id}"
    if (bookmarked) ApiClient.post(endpoint, emptyMap<String, Any>()) { _, _, _ -> }
    else ApiClient.delete(endpoint) { _, _, _ -> }
}
