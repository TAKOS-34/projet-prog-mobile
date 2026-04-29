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
import com.example.myapplication.dto.group.GroupSearchDto
import com.example.myapplication.utils.resolveBackendUrl
import com.example.myapplication.utils.toShortDate
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView

class GroupSearchAdapter(
    private val onJoin: (GroupSearchDto) -> Unit,
    private val onGroupClick: (GroupSearchDto) -> Unit
) : ListAdapter<GroupSearchDto, GroupSearchAdapter.SearchViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group_search, parent, false)
        return SearchViewHolder(view, onJoin, onGroupClick)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SearchViewHolder(
        itemView: View,
        private val onJoin: (GroupSearchDto) -> Unit,
        private val onGroupClick: (GroupSearchDto) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val ivAvatar: ShapeableImageView = itemView.findViewById(R.id.ivGroupAvatar)
        private val ivLock: ImageView = itemView.findViewById(R.id.ivGroupLock)
        private val tvName: TextView = itemView.findViewById(R.id.tvGroupName)
        private val tvMembers: TextView = itemView.findViewById(R.id.tvGroupMembers)
        private val tvPosts: TextView = itemView.findViewById(R.id.tvGroupPosts)
        private val tvCreated: TextView = itemView.findViewById(R.id.tvGroupCreated)
        private val btnJoin: MaterialButton = itemView.findViewById(R.id.btnJoinGroup)

        fun bind(group: GroupSearchDto) {
            val context = itemView.context
            tvName.text = group.name
            tvMembers.text = context.getString(R.string.groups_members_count, group.nbMembers)
            tvPosts.text = context.getString(R.string.groups_posts_count, group.nbPosts)
            tvCreated.text = context.getString(R.string.groups_created_on, group.creationDate.toShortDate())
            ivLock.visibility = if (group.isGroupPrivate) View.VISIBLE else View.GONE

            ivAvatar.load(group.avatar.resolveBackendUrl()) {
                crossfade(true)
                placeholder(R.drawable.ic_launcher_background)
                transformations(CircleCropTransformation())
            }

            if (group.isMember) {
                btnJoin.visibility = View.GONE
            } else {
                btnJoin.visibility = View.VISIBLE
                btnJoin.setOnClickListener { onJoin(group) }
            }

            itemView.setOnClickListener { onGroupClick(group) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<GroupSearchDto>() {
            override fun areItemsTheSame(oldItem: GroupSearchDto, newItem: GroupSearchDto) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: GroupSearchDto, newItem: GroupSearchDto) = oldItem == newItem
        }
    }
}
