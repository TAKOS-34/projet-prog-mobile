package com.example.myapplication.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import com.example.myapplication.R
import com.example.myapplication.dto.post.PostType
import com.example.myapplication.dto.trip.TripLocalisationDto
import com.example.myapplication.dto.trip.TripStepDetailDto
import com.example.myapplication.utils.DateUtils
import com.example.myapplication.utils.resolveBackendUrl

class TripStepsAdapter(
    private val steps: List<TripStepDetailDto>,
    private val onPostClick: (postId: String) -> Unit,
    private val onLocationClick: (name: String, lat: Double, long: Double) -> Unit,
    private val startLocationName: String? = null
) : RecyclerView.Adapter<TripStepsAdapter.StepViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StepViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trip_step, parent, false)
        return StepViewHolder(view, onPostClick, onLocationClick)
    }

    override fun onBindViewHolder(holder: StepViewHolder, position: Int) {
        holder.bind(steps[position], position, position == steps.size - 1)
    }

    override fun getItemCount() = steps.size

    inner class StepViewHolder(
        itemView: View,
        private val onPostClick: (postId: String) -> Unit,
        private val onLocationClick: (name: String, lat: Double, long: Double) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val vStartDot: View = itemView.findViewById(R.id.vStartDot)
        private val tvStartLocation: TextView = itemView.findViewById(R.id.tvStartLocation)
        private val tvTravelTime: TextView = itemView.findViewById(R.id.tvTravelTime)
        private val ivTravelTrust: ImageView = itemView.findViewById(R.id.ivTravelTrust)
        private val ivConnector: ImageView = itemView.findViewById(R.id.ivConnector)
        private val vDot: TextView = itemView.findViewById(R.id.vDot)
        private val vBottomLine: View = itemView.findViewById(R.id.vBottomLine)
        private val ivImage: ImageView = itemView.findViewById(R.id.ivStepImage)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvStepTitle)
        private val tvLocation: TextView = itemView.findViewById(R.id.tvStepLocation)
        private val tvType: TextView = itemView.findViewById(R.id.tvStepType)
        private val tvPriceSeparator: TextView = itemView.findViewById(R.id.tvPriceSeparator)
        private val ivPriceIcon: ImageView = itemView.findViewById(R.id.ivPriceIcon)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvStepPrice)
        private val tvVisit: TextView = itemView.findViewById(R.id.tvStepVisitDuration)
        private val ivVisitTrust: ImageView = itemView.findViewById(R.id.ivVisitTrust)

        fun bind(step: TripStepDetailDto, position: Int, isLast: Boolean) {
            val ctx = itemView.context

            if (position == 0) {
                vStartDot.visibility = View.VISIBLE
                tvStartLocation.visibility = View.VISIBLE
                tvStartLocation.text = startLocationName ?: ctx.getString(R.string.trip_start_position)
            } else {
                vStartDot.visibility = View.GONE
                tvStartLocation.visibility = View.GONE
            }

            tvTravelTime.text = ctx.getString(R.string.trip_step_travel_time, DateUtils.formatMinutes(ctx, step.travelTimeFromPrevious))
            ivTravelTrust.visibility = if (step.isTravelTimeFromPreviousTrusted) View.VISIBLE else View.GONE

            vDot.text = (position + 1).toString()
            vBottomLine.visibility = if (isLast) View.INVISIBLE else View.VISIBLE

            tvTitle.text = step.post.title
            tvLocation.text = step.localisation.name

            // Price binding
            if (step.post.minPrice != null && step.post.maxPrice != null) {
                tvPriceSeparator.visibility = View.VISIBLE
                ivPriceIcon.visibility = View.VISIBLE
                tvPrice.visibility = View.VISIBLE
                if (step.post.minPrice == 0 && step.post.maxPrice == 0) {
                    tvPrice.text = ctx.getString(R.string.price_free)
                } else {
                    tvPrice.text = ctx.getString(R.string.price_range_format, step.post.minPrice, step.post.maxPrice)
                }
            } else {
                tvPriceSeparator.visibility = View.GONE
                ivPriceIcon.visibility = View.GONE
                tvPrice.visibility = View.GONE
            }

            tvVisit.text = ctx.getString(R.string.trip_step_visit_duration, DateUtils.formatMinutes(ctx, step.visitDuration))
            ivVisitTrust.visibility = if (step.isVisitDurationTrusted) View.VISIBLE else View.GONE

            val postType = PostType.entries.firstOrNull { it.name == step.post.type }
            tvType.text = postType?.let { ctx.getString(it.labelRes) } ?: step.post.type

            ivImage.load(step.post.image.resolveBackendUrl()) {
                crossfade(true)
                transformations(RoundedCornersTransformation(8f))
            }

            ivImage.setOnClickListener { onPostClick(step.post.id) }
            tvLocation.setOnClickListener {
                onLocationClick(step.localisation.name, step.localisation.lat, step.localisation.long)
            }
        }
    }
}
