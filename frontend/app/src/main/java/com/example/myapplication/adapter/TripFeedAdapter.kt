package com.example.myapplication.adapter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
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
import com.example.myapplication.utils.SessionManager
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
        private val tvDistanceSep: TextView = itemView.findViewById(R.id.tvTripFeedDistanceSep)
        private val tvDistance: TextView = itemView.findViewById(R.id.tvTripFeedDistance)
        private val llDifficulty: View = itemView.findViewById(R.id.llTripFeedDifficulty)
        private val ivDifficulty: ImageView = itemView.findViewById(R.id.ivTripFeedDifficulty)
        private val tvDifficulty: TextView = itemView.findViewById(R.id.tvTripFeedDifficulty)
        private val tvDifficultySep: TextView = itemView.findViewById(R.id.tvTripFeedDifficultySep)
        private val ivAscent: ImageView = itemView.findViewById(R.id.ivTripFeedAscent)
        private val tvAscent: TextView = itemView.findViewById(R.id.tvTripFeedAscent)
        private val btnLike: ImageView = itemView.findViewById(R.id.btnTripFeedLike)
        private val tvLikeCount: TextView = itemView.findViewById(R.id.tvTripFeedLikeCount)
        private val btnShare: ImageView = itemView.findViewById(R.id.btnTripFeedShare)
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
                val baseHour = DateUtils.startingTimeToBaseHour(trip.startingTime)
                tvStartingTime.text = "${ctx.getString(startingTime.labelRes)} (${baseHour}h)"
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

            val dist = trip.totalDistance
            if (dist != null) {
                tvDistanceSep.visibility = View.VISIBLE
                tvDistance.visibility = View.VISIBLE
                tvDistance.text = DateUtils.formatDistance(dist)
            } else {
                tvDistanceSep.visibility = View.GONE
                tvDistance.visibility = View.GONE
            }

            val hasDifficulty = trip.difficulty != null
            val hasAscent = trip.totalAscent != null
            llDifficulty.visibility = if (hasDifficulty || hasAscent) View.VISIBLE else View.GONE
            if (hasDifficulty) {
                ivDifficulty.visibility = View.VISIBLE
                tvDifficulty.visibility = View.VISIBLE
                tvDifficulty.text = "${trip.difficulty}/5"
            } else {
                ivDifficulty.visibility = View.GONE
                tvDifficulty.visibility = View.GONE
            }
            tvDifficultySep.visibility = if (hasDifficulty && hasAscent) View.VISIBLE else View.GONE
            if (hasAscent) {
                ivAscent.visibility = View.VISIBLE
                tvAscent.visibility = View.VISIBLE
                tvAscent.text = "+${DateUtils.formatDistance(trip.totalAscent!!)}"
            } else {
                ivAscent.visibility = View.GONE
                tvAscent.visibility = View.GONE
            }

            val isLogged = SessionManager.getUserId() != null
            if (!isLogged) {
                btnLike.visibility = View.GONE
                tvLikeCount.visibility = View.GONE
                btnBookmark.visibility = View.GONE
            } else {
                btnLike.visibility = View.VISIBLE
                tvLikeCount.visibility = View.VISIBLE
                btnBookmark.visibility = View.VISIBLE
                applyLikeState()
                applyBookmarkState()
            }

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

            btnShare.setOnClickListener {
                val ctx = itemView.context
                val link = "travelpath://trip/${trip.id}"
                val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("TravelPath", link))
                Toast.makeText(ctx, ctx.getString(R.string.trip_link_copied), Toast.LENGTH_SHORT).show()
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
