package com.example.myapplication.adapter

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.myapplication.BuildConfig
import com.example.myapplication.utils.resolveBackendUrl
import com.example.myapplication.R
import com.example.myapplication.dto.trip.TripSuggestInfosDto
import com.example.myapplication.dto.trip.WeatherDto
import com.example.myapplication.utils.toTripDuration
import com.example.myapplication.utils.toWeatherEmoji
import com.example.myapplication.utils.toWeatherLabel

class TripSuggestsAdapter(
    private val trips: List<TripSuggestInfosDto>,
    private val weather: WeatherDto,
    private val onClick: (TripSuggestInfosDto, Int) -> Unit
) : RecyclerView.Adapter<TripSuggestsAdapter.TripViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trip_suggest, parent, false)
        return TripViewHolder(view)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        holder.bind(trips[position], position, weather)
        holder.itemView.setOnClickListener { onClick(trips[position], position) }
    }

    override fun getItemCount() = trips.size

    class TripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivCover: ImageView = itemView.findViewById(R.id.ivTripCover)
        private val tvWeather: TextView = itemView.findViewById(R.id.tvWeatherBadge)
        private val tvIndex: TextView = itemView.findViewById(R.id.tvTripIndex)
        private val tvDuration: TextView = itemView.findViewById(R.id.tvTripDuration)
        private val tvCost: TextView = itemView.findViewById(R.id.tvTripCost)
        private val tvSteps: TextView = itemView.findViewById(R.id.tvTripSteps)
        private val tvFirstLocation: TextView = itemView.findViewById(R.id.tvFirstLocation)

        fun bind(trip: TripSuggestInfosDto, index: Int, weather: WeatherDto) {
            val ctx = itemView.context

            val isNormal = index == 0
            tvIndex.text = if (isNormal) ctx.getString(R.string.trip_label_normal) else ctx.getString(R.string.trip_label_business)
            tvIndex.background = GradientDrawable().apply {
                setColor(if (isNormal) ContextCompat.getColor(ctx, R.color.secondary) else Color.parseColor("#C8860A"))
                cornerRadius = 6f * ctx.resources.displayMetrics.density
            }

            tvDuration.text = trip.totalDuration.toTripDuration(ctx)
            tvCost.text = ctx.getString(R.string.trip_total_cost, trip.totalCost)
            tvSteps.text = ctx.getString(R.string.trip_result_n_steps, trip.totalStep)
            tvWeather.text = "${weather.code.toWeatherEmoji()} ${ctx.getString(R.string.trip_result_temperature, weather.temperature)}"

            tvFirstLocation.text = trip.steps.joinToString("  →  ") { it.localisation.name }

            val firstStep = trip.steps.firstOrNull()
            if (firstStep != null) {
                ivCover.load(firstStep.post.image.resolveBackendUrl()) { crossfade(true) }
            }
        }
    }
}
