package com.example.myapplication.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.myapplication.R
import com.example.myapplication.dto.notification.NotificationDto
import com.example.myapplication.utils.DateUtils
import com.example.myapplication.utils.resolveBackendUrl
import com.google.android.material.card.MaterialCardView
import com.google.android.material.imageview.ShapeableImageView

class NotificationsAdapter(
    private val onClick: (NotificationDto) -> Unit
) : ListAdapter<NotificationDto, NotificationsAdapter.NotifViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotifViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotifViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: NotifViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class NotifViewHolder(
        itemView: View,
        private val onClick: (NotificationDto) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val card = itemView as MaterialCardView
        private val ivAvatar: ShapeableImageView = itemView.findViewById(R.id.ivNotifAvatar)
        private val ivPostImage: ShapeableImageView = itemView.findViewById(R.id.ivNotifPostImage)
        private val tvMessage: TextView = itemView.findViewById(R.id.tvNotifMessage)
        private val tvDate: TextView = itemView.findViewById(R.id.tvNotifDate)
        private val dotUnread: View = itemView.findViewById(R.id.dotUnread)

        fun bind(notif: NotificationDto) {
            val context = itemView.context

            val avatarUrl = notif.postUserAvatar ?: notif.groupAvatar
            if (!avatarUrl.isNullOrBlank()) {
                ivAvatar.visibility = View.VISIBLE
                ivAvatar.load(avatarUrl.resolveBackendUrl()) {
                    crossfade(true)
                    placeholder(R.drawable.ic_launcher_background)
                    transformations(CircleCropTransformation())
                }
            } else {
                ivAvatar.setImageResource(R.drawable.ic_launcher_background)
            }

            tvMessage.text = formatMessage(notif)
            tvDate.text = DateUtils.formatRelativeDate(context, notif.creationDate)

            if (!notif.postImage.isNullOrBlank()) {
                ivPostImage.visibility = View.VISIBLE
                ivPostImage.load(notif.postImage.resolveBackendUrl()) { crossfade(true) }
            } else {
                ivPostImage.visibility = View.GONE
            }

            dotUnread.visibility = if (notif.isRead) View.GONE else View.VISIBLE
            val bgColor = if (notif.isRead) R.color.surface else R.color.surface_variant
            card.setCardBackgroundColor(ContextCompat.getColor(context, bgColor))

            itemView.setOnClickListener { onClick(notif) }
        }

        private fun formatMessage(notif: NotificationDto): String {
            val context = itemView.context
            return when {
                !notif.postUsername.isNullOrBlank() && notif.groupName.isNullOrBlank() && notif.tagName.isNullOrBlank() ->
                    context.getString(R.string.notif_user_post, notif.postUsername)
                !notif.groupName.isNullOrBlank() ->
                    context.getString(R.string.notif_group_post, notif.groupName)
                !notif.tagName.isNullOrBlank() ->
                    context.getString(R.string.notif_tag_post, notif.tagName)
                else -> ""
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<NotificationDto>() {
            override fun areItemsTheSame(oldItem: NotificationDto, newItem: NotificationDto) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: NotificationDto, newItem: NotificationDto) = oldItem == newItem
        }
    }
}
