package com.example.myapplication.fragment.group

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.adapter.GroupSearchAdapter
import com.example.myapplication.adapter.GroupsAdapter
import com.example.myapplication.dto.group.GroupInfosDto
import com.example.myapplication.dto.group.GroupSearchDto
import com.example.myapplication.utils.AdminGroupsCache
import com.example.myapplication.utils.ApiClient
import com.example.myapplication.utils.AuthViewModel
import com.example.myapplication.utils.requestToJoinGroup
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.net.URLEncoder

class GroupsFragment : Fragment() {

    private val authViewModel: AuthViewModel by activityViewModels()

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var etSearch: TextInputEditText
    private lateinit var tilSearch: TextInputLayout

    private val groupsAdapter: GroupsAdapter by lazy {
        GroupsAdapter { group ->
            val bundle = Bundle().apply {
                putInt(GroupDetailFragment.ARG_GROUP_ID, group.id)
            }
            findNavController().navigate(R.id.groupDetailFragment, bundle)
        }
    }

    private val searchAdapter: GroupSearchAdapter by lazy {
        GroupSearchAdapter(
            onJoin = { group -> requestToJoinGroup(group.id) },
            onGroupClick = { group ->
                val bundle = Bundle().apply { putInt("groupId", group.id) }
                findNavController().navigate(R.id.groupDetailFragment, bundle)
            }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (!authViewModel.isAuthenticated()) {
            val guestView = inflater.inflate(R.layout.fragment_guest_prompt, container, false)
            guestView.findViewById<MaterialButton>(R.id.btnGuestLogin).setOnClickListener {
                findNavController().navigate(R.id.loginFragment)
            }
            return guestView
        }

        val view = inflater.inflate(R.layout.fragment_groups, container, false)

        recyclerView = view.findViewById(R.id.rvGroups)
        tvEmpty = view.findViewById(R.id.tvGroupsEmpty)
        etSearch = view.findViewById(R.id.etGroupsSearch)
        tilSearch = view.findViewById(R.id.tilGroupsSearch)

        view.findViewById<View>(R.id.fabNewGroup).setOnClickListener {
            findNavController().navigate(R.id.createGroupFragment)
        }

        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = etSearch.text.toString().trim()
                if (query.isNotEmpty()) performSearch(query) else clearSearchResults()
                true
            } else false
        }

        val toggle = view.findViewById<MaterialButtonToggleGroup>(R.id.tgGroupsTabs)
        toggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            when (checkedId) {
                R.id.btnTabMyGroups -> showMyGroups()
                R.id.btnTabSearchGroups -> showSearchTab()
                R.id.btnTabPopularGroups -> showPopularGroups()
            }
        }
        toggle.check(R.id.btnTabMyGroups)

        return view
    }

    private fun showMyGroups() {
        tilSearch.visibility = View.GONE
        recyclerView.adapter = groupsAdapter
        tvEmpty.setText(R.string.groups_empty)
        fetchMyGroups()
    }

    private fun showSearchTab() {
        tilSearch.visibility = View.VISIBLE
        recyclerView.adapter = searchAdapter
        searchAdapter.submitList(emptyList())
        tvEmpty.setText(R.string.groups_search_empty)
        renderState(true)
        val q = etSearch.text.toString().trim()
        if (q.isNotEmpty()) performSearch(q)
    }

    private fun showPopularGroups() {
        tilSearch.visibility = View.GONE
        recyclerView.adapter = searchAdapter
        tvEmpty.setText(R.string.groups_search_empty)
        fetchPopularGroups()
    }

    private fun fetchMyGroups() {
        ApiClient.get("group/my-groups") { body, _, error ->
            activity?.runOnUiThread {
                if (error == null && body != null) {
                    try {
                        val type = object : TypeToken<List<GroupInfosDto>>() {}.type
                        val groups: List<GroupInfosDto> = Gson().fromJson(body, type)
                        AdminGroupsCache.set(groups.filter { it.isAdmin }.map { it.id })
                        renderState(groups.isEmpty())
                        groupsAdapter.submitList(groups)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    Toast.makeText(context, R.string.error_load_groups, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun fetchPopularGroups() {
        ApiClient.get("group/popular") { body, _, error ->
            activity?.runOnUiThread {
                if (error == null && body != null) {
                    try {
                        val type = object : TypeToken<List<GroupSearchDto>>() {}.type
                        val groups: List<GroupSearchDto> = Gson().fromJson(body, type)
                        renderState(groups.isEmpty())
                        searchAdapter.submitList(groups)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    Toast.makeText(context, R.string.error_load_groups, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun performSearch(query: String) {
        recyclerView.adapter = searchAdapter
        tvEmpty.setText(R.string.groups_search_empty)

        val encoded = URLEncoder.encode(query, Charsets.UTF_8.name())
        ApiClient.get("search/groups?name=$encoded") { body, _, error ->
            activity?.runOnUiThread {
                if (error == null && body != null) {
                    try {
                        val type = object : TypeToken<List<GroupSearchDto>>() {}.type
                        val results: List<GroupSearchDto> = Gson().fromJson(body, type)
                        renderState(results.isEmpty())
                        searchAdapter.submitList(results)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun clearSearchResults() {
        searchAdapter.submitList(emptyList())
        renderState(true)
    }

    private fun renderState(isEmpty: Boolean) {
        tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
}
