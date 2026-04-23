package com.example.myapplication.fragment.group

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.adapter.GroupsAdapter
import com.example.myapplication.dto.group.GroupCardInfosDto
import com.example.myapplication.utils.ApiClient
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class GroupsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: GroupsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_groups, container, false)

        recyclerView = view.findViewById(R.id.rvGroups)
        tvEmpty = view.findViewById(R.id.tvGroupsEmpty)

        adapter = GroupsAdapter { group ->
            val bundle = Bundle().apply {
                putString(GroupDetailFragment.ARG_GROUP, Gson().toJson(group))
            }
            findNavController().navigate(R.id.groupDetailFragment, bundle)
        }
        recyclerView.adapter = adapter

        fetchGroups()

        return view
    }

    private fun fetchGroups() {
        ApiClient.get("group/my-groups") { body, _, error ->
            activity?.runOnUiThread {
                if (error == null && body != null) {
                    try {
                        val type = object : TypeToken<List<GroupCardInfosDto>>() {}.type
                        val groups: List<GroupCardInfosDto> = Gson().fromJson(body, type)
                        tvEmpty.visibility = if (groups.isEmpty()) View.VISIBLE else View.GONE
                        recyclerView.visibility = if (groups.isEmpty()) View.GONE else View.VISIBLE
                        adapter.submitList(groups)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    Toast.makeText(context, R.string.error_load_groups, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
