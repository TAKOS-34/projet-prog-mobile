package com.example.myapplication.fragment.group

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.adapter.GroupMembersAdapter
import com.example.myapplication.dto.group.GroupMemberDto
import com.example.myapplication.utils.ApiClient
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class GroupMembersFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: GroupMembersAdapter
    private var groupId: Int = 0
    private var isAdmin: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_group_members, container, false)

        recyclerView = view.findViewById(R.id.rvMembers)
        tvEmpty = view.findViewById(R.id.tvMembersEmpty)

        groupId = arguments?.getInt(ARG_GROUP_ID) ?: return view
        isAdmin = arguments?.getBoolean(ARG_IS_ADMIN) ?: false

        val mode = if (isAdmin) {
            GroupMembersAdapter.Mode.Admin(
                onBan = { member -> banMember(member.id) },
                onTransfer = { member -> transferAdmin(member.id) }
            )
        } else {
            GroupMembersAdapter.Mode.Plain
        }
        adapter = GroupMembersAdapter(mode)
        recyclerView.adapter = adapter

        view.findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            findNavController().navigateUp()
        }

        fetchMembers()

        return view
    }

    private fun fetchMembers() {
        ApiClient.get("group/$groupId/members") { body, _, error ->
            activity?.runOnUiThread {
                if (error == null && body != null) {
                    try {
                        val type = object : TypeToken<List<GroupMemberDto>>() {}.type
                        val members: List<GroupMemberDto> = Gson().fromJson(body, type)
                        tvEmpty.visibility = if (members.isEmpty()) View.VISIBLE else View.GONE
                        recyclerView.visibility = if (members.isEmpty()) View.GONE else View.VISIBLE
                        adapter.submitList(members)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun banMember(userId: Int) {
        ApiClient.post("group/admin/ban/$groupId/$userId", emptyMap<String, String>()) { _, _, error ->
            activity?.runOnUiThread {
                if (error == null) {
                    Toast.makeText(context, R.string.success_member_banned, Toast.LENGTH_SHORT).show()
                    fetchMembers()
                } else {
                    Toast.makeText(context, R.string.error_generic_action, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun transferAdmin(userId: Int) {
        ApiClient.patch("group/admin/transfer-admin-role/$groupId/$userId", emptyMap<String, String>()) { _, _, error ->
            activity?.runOnUiThread {
                if (error == null) {
                    Toast.makeText(context, R.string.success_admin_transferred, Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                } else {
                    Toast.makeText(context, R.string.error_generic_action, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        const val ARG_GROUP_ID = "groupId"
        const val ARG_IS_ADMIN = "isAdmin"
    }
}
