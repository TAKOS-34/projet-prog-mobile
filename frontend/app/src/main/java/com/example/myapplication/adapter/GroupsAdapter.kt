package com.example.myapplication.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.myapplication.R
import com.example.myapplication.dto.group.GroupCardInfosDto
import com.example.myapplication.utils.resolveBackendUrl
import com.example.myapplication.utils.toShortDate
import com.google.android.material.imageview.ShapeableImageView

class GroupsAdapter(
    private val onClick: (GroupCardInfosDto) -> Unit
) : ListAdapter<GroupCardInfosDto, GroupsAdapter.GroupViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group, parent, false)
        return GroupViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class GroupViewHolder(
        itemView: View,
        private val onClick: (GroupCardInfosDto) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val ivAvatar: ShapeableImageView = itemView.findViewById(R.id.ivGroupAvatar)
        private val ivLock: ImageView = itemView.findViewById(R.id.ivGroupLock)
        private val tvName: TextView = itemView.findViewById(R.id.tvGroupName)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvGroupDescription)
        private val tvMembers: TextView = itemView.findViewById(R.id.tvGroupMembers)
        private val tvPosts: TextView = itemView.findViewById(R.id.tvGroupPosts)
        private val tvCreated: TextView = itemView.findViewById(R.id.tvGroupCreated)
        private val tvAdmin: TextView = itemView.findViewById(R.id.tvGroupAdmin)

        fun bind(group: GroupCardInfosDto) {
            val context = itemView.context
            tvName.text = group.name

            if (!group.description.isNullOrBlank()) {
                tvDescription.visibility = View.VISIBLE
                tvDescription.text = group.description
            } else {
                tvDescription.visibility = View.GONE
            }

            tvMembers.text = context.getString(R.string.groups_members_count, group.nbMembers)
            tvPosts.text = context.getString(R.string.groups_posts_count, group.nbPosts)
            tvCreated.text = context.getString(R.string.groups_created_on, group.creationDate.toShortDate())
            ivLock.visibility = if (group.isGroupPrivate) View.VISIBLE else View.GONE
            tvAdmin.visibility = if (group.isAdmin) View.VISIBLE else View.GONE

            ivAvatar.load(group.avatar.resolveBackendUrl()) {
                crossfade(true)
                placeholder(R.drawable.ic_launcher_background)
                transformations(CircleCropTransformation())
            }

            itemView.setOnClickListener { onClick(group) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<GroupCardInfosDto>() {
            override fun areItemsTheSame(oldItem: GroupCardInfosDto, newItem: GroupCardInfosDto) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: GroupCardInfosDto, newItem: GroupCardInfosDto) = oldItem == newItem
        }
    }
}
