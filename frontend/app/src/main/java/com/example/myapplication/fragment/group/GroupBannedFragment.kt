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

class GroupBannedFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: GroupMembersAdapter
    private var groupId: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_group_members, container, false)

        recyclerView = view.findViewById(R.id.rvMembers)
        tvEmpty = view.findViewById(R.id.tvMembersEmpty)

        view.findViewById<TextView>(R.id.tvMembersTitle).setText(R.string.groups_banned_title)
        tvEmpty.setText(R.string.groups_banned_empty)

        groupId = arguments?.getInt(ARG_GROUP_ID) ?: return view

        adapter = GroupMembersAdapter(
            GroupMembersAdapter.Mode.Banned { member -> debanMember(member.id) }
        )
        recyclerView.adapter = adapter

        view.findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            findNavController().navigateUp()
        }

        fetchBanned()

        return view
    }

    private fun fetchBanned() {
        ApiClient.get("group/admin/$groupId/list/ban") { body, _, error ->
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

    private fun debanMember(userId: Int) {
        ApiClient.delete("group/admin/deban/$groupId/$userId") { _, _, error ->
            activity?.runOnUiThread {
                if (error == null) {
                    Toast.makeText(context, R.string.success_member_debanned, Toast.LENGTH_SHORT).show()
                    fetchBanned()
                } else {
                    Toast.makeText(context, R.string.error_generic_action, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        const val ARG_GROUP_ID = "groupId"
    }
}
