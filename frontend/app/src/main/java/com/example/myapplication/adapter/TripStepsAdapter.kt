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
import com.example.myapplication.utils.resolveBackendUrl

class TripStepsAdapter(
    private val steps: List<TripStepDetailDto>,
    private val onPostClick: (postId: String) -> Unit,
    private val onLocationClick: (name: String, lat: Double, long: Double) -> Unit
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

    class StepViewHolder(
        itemView: View,
        private val onPostClick: (postId: String) -> Unit,
        private val onLocationClick: (name: String, lat: Double, long: Double) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val llTravelConnector: LinearLayout = itemView.findViewById(R.id.llTravelConnector)
        private val tvTravelTime: TextView = itemView.findViewById(R.id.tvTravelTime)
        private val ivTravelTrust: ImageView = itemView.findViewById(R.id.ivTravelTrust)
        private val vDot: TextView = itemView.findViewById(R.id.vDot)
        private val vBottomLine: View = itemView.findViewById(R.id.vBottomLine)
        private val ivImage: ImageView = itemView.findViewById(R.id.ivStepImage)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvStepTitle)
        private val tvLocation: TextView = itemView.findViewById(R.id.tvStepLocation)
        private val tvType: TextView = itemView.findViewById(R.id.tvStepType)
        private val tvVisit: TextView = itemView.findViewById(R.id.tvStepVisitDuration)
        private val ivVisitTrust: ImageView = itemView.findViewById(R.id.ivVisitTrust)

        fun bind(step: TripStepDetailDto, position: Int, isLast: Boolean) {
            val ctx = itemView.context

            if (position == 0) {
                llTravelConnector.visibility = View.GONE
            } else {
                llTravelConnector.visibility = View.VISIBLE
                tvTravelTime.text = ctx.getString(R.string.trip_step_travel_time, step.travelTimeFromPrevious)
                ivTravelTrust.visibility = if (step.isTravelTimeFromPreviousTrusted) View.VISIBLE else View.GONE
            }

            vDot.text = (position + 1).toString()
            vBottomLine.visibility = if (isLast) View.INVISIBLE else View.VISIBLE

            tvTitle.text = step.post.title
            tvLocation.text = step.localisation.name
            tvVisit.text = ctx.getString(R.string.trip_step_visit_duration, step.visitDuration)
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
