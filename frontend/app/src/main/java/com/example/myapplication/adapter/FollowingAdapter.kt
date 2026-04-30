package com.example.myapplication.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.myapplication.R
import com.example.myapplication.dto.notification.FollowingDto
import com.example.myapplication.utils.resolveBackendUrl
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView

class FollowingAdapter(
    private val onUnfollow: (FollowingDto) -> Unit
) : ListAdapter<FollowingDto, FollowingAdapter.FollowingViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FollowingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_following, parent, false)
        return FollowingViewHolder(view, onUnfollow)
    }

    override fun onBindViewHolder(holder: FollowingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class FollowingViewHolder(
        itemView: View,
        private val onUnfollow: (FollowingDto) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val ivAvatar: ShapeableImageView = itemView.findViewById(R.id.ivFollowingAvatar)
        private val tvName: TextView = itemView.findViewById(R.id.tvFollowingName)
        private val btnUnfollow: MaterialButton = itemView.findViewById(R.id.btnUnfollow)

        fun bind(item: FollowingDto) {
            when (item.type.lowercase()) {
                "group" -> {
                    tvName.text = item.targetGroupName.orEmpty()
                    loadAvatar(item.targetGroupAvatar)
                    ivAvatar.visibility = View.VISIBLE
                }
                "user" -> {
                    tvName.text = item.targetUsername.orEmpty()
                    loadAvatar(item.targetUserAvatar)
                    ivAvatar.visibility = View.VISIBLE
                }
                "tag" -> {
                    tvName.text = "#${item.targetTagName.orEmpty()}"
                    ivAvatar.visibility = View.GONE
                }
                "localisation" -> {
                    tvName.text = item.targetLocalisationName.orEmpty()
                    ivAvatar.visibility = View.GONE
                }
            }
            btnUnfollow.setOnClickListener { onUnfollow(item) }
        }

        private fun loadAvatar(url: String?) {
            url?.takeIf { it.isNotBlank() }?.let {
                ivAvatar.load(it.resolveBackendUrl()) {
                    crossfade(true)
                    placeholder(R.drawable.ic_launcher_background)
                    transformations(CircleCropTransformation())
                }
            } ?: ivAvatar.setImageDrawable(null)
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<FollowingDto>() {
            override fun areItemsTheSame(oldItem: FollowingDto, newItem: FollowingDto): Boolean {
                return oldItem.type == newItem.type && idOf(oldItem) == idOf(newItem)
            }
            override fun areContentsTheSame(oldItem: FollowingDto, newItem: FollowingDto) = oldItem == newItem
        }

        fun idOf(item: FollowingDto): Int? = when (item.type.lowercase()) {
            "group" -> item.targetGroupId
            "user" -> item.targetUserId
            "tag" -> item.targetTagId
            "localisation" -> item.targetLocalisationId
            else -> null
        }
    }
}
