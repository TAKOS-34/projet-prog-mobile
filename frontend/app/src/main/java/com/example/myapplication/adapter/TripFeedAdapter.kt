package com.example.myapplication.adapter

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.myapplication.R
import com.example.myapplication.dto.trip.StartingTime
import com.example.myapplication.dto.trip.TransportMode
import com.example.myapplication.dto.trip.TripFeedItemDto
import com.example.myapplication.utils.DateUtils
import com.example.myapplication.utils.resolveBackendUrl
import com.example.myapplication.utils.LocalisationFormat
import com.example.myapplication.utils.toTripDuration
import com.example.myapplication.utils.toWeatherEmoji
import com.example.myapplication.utils.toWeatherLabel

class TripFeedAdapter(
    private val onLike: (TripFeedItemDto, Boolean) -> Unit,
    private val onBookmark: (TripFeedItemDto, Boolean) -> Unit,
    private val onClick: (TripFeedItemDto) -> Unit,
    private val onStartLocationClick: ((name: String, lat: Double, long: Double) -> Unit)? = null,
    private val onDelete: ((TripFeedItemDto) -> Unit)? = null
) : ListAdapter<TripFeedItemDto, TripFeedAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_trip_feed, parent, false)
        return ViewHolder(view, onLike, onBookmark, onClick, onStartLocationClick, onDelete)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        itemView: View,
        private val onLike: (TripFeedItemDto, Boolean) -> Unit,
        private val onBookmark: (TripFeedItemDto, Boolean) -> Unit,
        private val onClick: (TripFeedItemDto) -> Unit,
        private val onStartLocationClick: ((name: String, lat: Double, long: Double) -> Unit)? = null,
        private val onDelete: ((TripFeedItemDto) -> Unit)? = null
    ) : RecyclerView.ViewHolder(itemView) {

        private val ivCover: ImageView = itemView.findViewById(R.id.ivTripFeedCover)
        private val tvCategory: TextView = itemView.findViewById(R.id.tvTripFeedCategory)
        private val tvWeather: TextView = itemView.findViewById(R.id.tvTripFeedWeather)
        private val ivAvatar: ImageView = itemView.findViewById(R.id.ivTripFeedAvatar)
        private val tvUsername: TextView = itemView.findViewById(R.id.tvTripFeedUsername)
        private val tvDate: TextView = itemView.findViewById(R.id.tvTripFeedDate)
        private val tvDuration: TextView = itemView.findViewById(R.id.tvTripFeedDuration)
        private val tvCost: TextView = itemView.findViewById(R.id.tvTripFeedCost)
        private val tvSteps: TextView = itemView.findViewById(R.id.tvTripFeedSteps)
        private val ivTransport: ImageView = itemView.findViewById(R.id.ivTripFeedTransport)
        private val tvTransport: TextView = itemView.findViewById(R.id.tvTripFeedTransport)
        private val ivStartingTime: ImageView = itemView.findViewById(R.id.ivTripFeedStartingTime)
        private val tvStartingTime: TextView = itemView.findViewById(R.id.tvTripFeedStartingTime)
        private val llStartLocation: LinearLayout = itemView.findViewById(R.id.llTripFeedStartLocation)
        private val tvStartLocation: TextView = itemView.findViewById(R.id.tvTripFeedStartLocation)
        private val tvRoute: TextView = itemView.findViewById(R.id.tvTripFeedRoute)
        private val btnDelete: ImageView = itemView.findViewById(R.id.btnDelete)
        private val btnLike: ImageView = itemView.findViewById(R.id.btnTripFeedLike)
        private val tvLikeCount: TextView = itemView.findViewById(R.id.tvTripFeedLikeCount)
        private val btnBookmark: ImageView = itemView.findViewById(R.id.btnTripFeedBookmark)

        private var isLiked = false
        private var likeCount = 0
        private var isBookmarkedLocal = false

        fun bind(trip: TripFeedItemDto) {
            val ctx = itemView.context
            val dp = ctx.resources.displayMetrics.density

            isLiked = trip.isLiked
            likeCount = trip.nbLikes
            isBookmarkedLocal = trip.isBookmarked

            val deleteVisible = trip.isYours && onDelete != null
            if (deleteVisible) {
                btnDelete.visibility = View.VISIBLE
                btnDelete.setOnClickListener { onDelete!!.invoke(trip) }
            } else {
                btnDelete.visibility = View.GONE
            }

            (tvWeather.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams)?.let {
                it.marginEnd = ((if (deleteVisible) 48 else 12) * dp).toInt()
                tvWeather.layoutParams = it
            }

            trip.steps.firstOrNull()?.post?.image?.let { url ->
                ivCover.load(url.resolveBackendUrl()) { crossfade(true) }
            }

            val isNormal = trip.category == "NORMAL"
            tvCategory.text = if (isNormal) ctx.getString(R.string.trip_label_normal)
                              else ctx.getString(R.string.trip_label_business)
            tvCategory.background = GradientDrawable().apply {
                setColor(if (isNormal) ContextCompat.getColor(ctx, R.color.secondary)
                         else Color.parseColor("#C8860A"))
                cornerRadius = 6f * dp
            }

            tvWeather.text = "${trip.weather.toWeatherEmoji()} ${trip.weather.toWeatherLabel(ctx)}"

            ivAvatar.load(trip.avatar.resolveBackendUrl()) {
                crossfade(true)
                transformations(CircleCropTransformation())
            }
            tvUsername.text = trip.username
            tvDate.text = DateUtils.formatRelativeDate(ctx, trip.creationDate)

            tvDuration.text = trip.totalDuration.toTripDuration(ctx)
            tvCost.text = ctx.getString(R.string.trip_total_cost, trip.totalCost)
            tvSteps.text = ctx.getString(R.string.trip_result_n_steps, trip.totalStep)

            val transport = TransportMode.fromApiValue(trip.transportMode)
            if (transport != null) {
                ivTransport.setImageResource(transport.iconRes)
                tvTransport.text = ctx.getString(transport.labelRes)
            }

            val startingTime = StartingTime.fromApiValue(trip.startingTime)
            if (startingTime != null) {
                ivStartingTime.setImageResource(startingTime.iconRes)
                tvStartingTime.text = ctx.getString(startingTime.labelRes)
            }

            val startLoc = trip.startLocalisation
            if (startLoc != null) {
                llStartLocation.visibility = View.VISIBLE
                tvStartLocation.text = LocalisationFormat.display(startLoc.name)
                llStartLocation.setOnClickListener {
                    onStartLocationClick?.invoke(startLoc.name, startLoc.lat, startLoc.long)
                }
            } else {
                llStartLocation.visibility = View.GONE
                llStartLocation.setOnClickListener(null)
            }

            tvRoute.text = trip.steps.joinToString(" → ") { LocalisationFormat.display(it.localisation.name) }

            applyLikeState()
            applyBookmarkState()

            btnLike.setOnClickListener {
                isLiked = !isLiked
                likeCount += if (isLiked) 1 else -1
                applyLikeState()
                onLike(trip, isLiked)
            }

            btnBookmark.setOnClickListener {
                isBookmarkedLocal = !isBookmarkedLocal
                applyBookmarkState()
                onBookmark(trip, isBookmarkedLocal)
            }

            itemView.setOnClickListener { onClick(trip) }
        }

        private fun applyLikeState() {
            val ctx = itemView.context
            btnLike.setImageResource(if (isLiked) R.drawable.ic_like_filled else R.drawable.ic_like)
            btnLike.setColorFilter(
                ContextCompat.getColor(ctx, if (isLiked) R.color.primary else R.color.text_secondary)
            )
            tvLikeCount.text = likeCount.toString()
        }

        private fun applyBookmarkState() {
            val ctx = itemView.context
            btnBookmark.setImageResource(if (isBookmarkedLocal) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark)
            btnBookmark.setColorFilter(
                ContextCompat.getColor(ctx, if (isBookmarkedLocal) R.color.primary else R.color.text_secondary)
            )
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<TripFeedItemDto>() {
            override fun areItemsTheSame(a: TripFeedItemDto, b: TripFeedItemDto) = a.id == b.id
            override fun areContentsTheSame(a: TripFeedItemDto, b: TripFeedItemDto) = a == b
        }
    }
}
