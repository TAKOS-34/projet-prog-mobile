package com.example.myapplication.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.dto.profile.TokenDto
import com.example.myapplication.utils.toShortDate
import com.google.android.material.card.MaterialCardView

class SessionsAdapter(
    private val onDelete: (TokenDto) -> Unit
) : ListAdapter<TokenDto, SessionsAdapter.SessionViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_session, parent, false)
        return SessionViewHolder(view, onDelete)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SessionViewHolder(
        itemView: View,
        private val onDelete: (TokenDto) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val card = itemView as MaterialCardView
        private val tvDevice: TextView = itemView.findViewById(R.id.tvSessionDevice)
        private val tvIp: TextView = itemView.findViewById(R.id.tvSessionIp)
        private val tvDate: TextView = itemView.findViewById(R.id.tvSessionDate)
        private val badge: TextView = itemView.findViewById(R.id.tvCurrentSessionBadge)
        private val btnDelete: ImageView = itemView.findViewById(R.id.btnDeleteSession)

        fun bind(token: TokenDto) {
            val context = itemView.context
            tvDevice.text = token.device.ifBlank { context.getString(R.string.session_unknown_device) }
            tvIp.text = token.ip
            tvDate.text = token.creationDate.toShortDate()

            if (token.isYourSession) {
                badge.visibility = View.VISIBLE
                card.strokeWidth = (2 * context.resources.displayMetrics.density).toInt()
                card.strokeColor = ContextCompat.getColor(context, R.color.primary)
                card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface_variant))
            } else {
                badge.visibility = View.GONE
                card.strokeWidth = 0
                card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface))
            }

            btnDelete.setOnClickListener { onDelete(token) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<TokenDto>() {
            override fun areItemsTheSame(oldItem: TokenDto, newItem: TokenDto) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: TokenDto, newItem: TokenDto) = oldItem == newItem
        }
    }
}
