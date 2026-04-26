package com.example.myapplication.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.myapplication.R
import com.example.myapplication.dto.group.GroupMemberDto
import com.example.myapplication.utils.SessionManager
import com.example.myapplication.utils.resolveBackendUrl
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView

class GroupMembersAdapter(
    private val mode: Mode = Mode.Plain
) : ListAdapter<GroupMemberDto, GroupMembersAdapter.MemberViewHolder>(DIFF) {

    sealed class Mode {
        data object Plain : Mode()
        data class Admin(val onBan: (GroupMemberDto) -> Unit, val onTransfer: (GroupMemberDto) -> Unit) : Mode()
        data class Banned(val onDeban: (GroupMemberDto) -> Unit) : Mode()
        data class Requests(val onAccept: (GroupMemberDto) -> Unit, val onRefuse: (GroupMemberDto) -> Unit) : Mode()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group_member, parent, false)
        return MemberViewHolder(view, mode)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MemberViewHolder(
        itemView: View,
        private val mode: Mode
    ) : RecyclerView.ViewHolder(itemView) {

        private val ivAvatar: ShapeableImageView = itemView.findViewById(R.id.ivMemberAvatar)
        private val tvUsername: TextView = itemView.findViewById(R.id.tvMemberUsername)
        private val btnMenu: ImageView = itemView.findViewById(R.id.btnMemberMenu)
        private val btnDeban: MaterialButton = itemView.findViewById(R.id.btnMemberDeban)
        private val btnAccept: MaterialButton = itemView.findViewById(R.id.btnMemberAccept)
        private val btnRefuse: MaterialButton = itemView.findViewById(R.id.btnMemberRefuse)

        fun bind(member: GroupMemberDto) {
            tvUsername.text = member.username
            ivAvatar.load(member.avatar.resolveBackendUrl()) {
                crossfade(true)
                placeholder(R.drawable.ic_launcher_background)
                transformations(CircleCropTransformation())
            }

            btnMenu.visibility = View.GONE
            btnDeban.visibility = View.GONE
            btnAccept.visibility = View.GONE
            btnRefuse.visibility = View.GONE

            when (val m = mode) {
                is Mode.Plain -> Unit
                is Mode.Admin -> {
                    if (member.id != SessionManager.getUserId()) {
                        btnMenu.visibility = View.VISIBLE
                        btnMenu.setOnClickListener { showAdminMenu(it, member, m) }
                    }
                }
                is Mode.Banned -> {
                    btnDeban.visibility = View.VISIBLE
                    btnDeban.setOnClickListener { m.onDeban(member) }
                }
                is Mode.Requests -> {
                    btnAccept.visibility = View.VISIBLE
                    btnRefuse.visibility = View.VISIBLE
                    btnAccept.setOnClickListener { m.onAccept(member) }
                    btnRefuse.setOnClickListener { m.onRefuse(member) }
                }
            }
        }

        private fun showAdminMenu(anchor: View, member: GroupMemberDto, mode: Mode.Admin) {
            val popup = PopupMenu(anchor.context, anchor)
            popup.menu.add(0, 1, 0, R.string.groups_admin_transfer)
            popup.menu.add(0, 2, 1, R.string.groups_admin_ban)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> { mode.onTransfer(member); true }
                    2 -> { mode.onBan(member); true }
                    else -> false
                }
            }
            popup.show()
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<GroupMemberDto>() {
            override fun areItemsTheSame(oldItem: GroupMemberDto, newItem: GroupMemberDto) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: GroupMemberDto, newItem: GroupMemberDto) = oldItem == newItem
        }
    }
}
