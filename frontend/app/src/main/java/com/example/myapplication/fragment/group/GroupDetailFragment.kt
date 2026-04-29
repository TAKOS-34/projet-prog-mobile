package com.example.myapplication.fragment.group

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.myapplication.R
import com.example.myapplication.adapter.PostsAdapter
import com.example.myapplication.dto.group.GroupInfosDto
import com.example.myapplication.dto.post.PostDto
import com.example.myapplication.utils.ApiClient
import com.example.myapplication.utils.buildPostsAdapter
import com.example.myapplication.utils.resolveBackendUrl
import com.example.myapplication.utils.toShortDate
import com.example.myapplication.utils.requestToJoinGroup
import com.example.myapplication.utils.resolveBackendUrl
import com.example.myapplication.utils.toShortDate
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class GroupDetailFragment : Fragment() {

    private lateinit var group: GroupInfosDto
    private lateinit var postsAdapter: PostsAdapter
    private lateinit var rvPosts: RecyclerView
    private lateinit var scrollInfo: NestedScrollView
    private var postsLoaded = false

    private var ivAvatar: ShapeableImageView? = null
    private var btnSaveAvatar: MaterialButton? = null
    private var pickedAvatarUri: Uri? = null
    private var btnFollowGroup: ImageView? = null
    private var isFollowingGroup: Boolean = false

    private val imagePicker = com.example.myapplication.utils.ImagePicker(this) { uri ->
        pickedAvatarUri = uri
        ivAvatar?.load(uri) {
            crossfade(true)
            transformations(CircleCropTransformation())
        }
        btnSaveAvatar?.visibility = View.VISIBLE
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_group_detail, container, false)
        val groupId = arguments?.getInt("groupId", -1) ?: -1

        view.findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            findNavController().navigateUp()
        }

        scrollInfo = view.findViewById(R.id.scrollInfo)
        rvPosts = view.findViewById(R.id.rvGroupPosts)

        if (groupId != -1) {
            fetchGroupDetail(groupId, view)
        } else {
            findNavController().navigateUp()
        }

        return view
    }

    private fun fetchGroupDetail(id: Int, view: View) {
        ApiClient.get("group/$id") { body, _, error ->
            activity?.runOnUiThread {
                if (error == null && body != null) {
                    try {
                        group = Gson().fromJson(body, GroupInfosDto::class.java)
                        initUi(view)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        findNavController().navigateUp()
                    }
                } else {
                    Toast.makeText(context, R.string.error_load_groups, Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
            }
        }
    }

    private fun initUi(view: View) {
        setupInfoTab(view)
        setupPostsTab()
        setupTabs(view)
    }

    private fun setupInfoTab(view: View) {
        val ctx = requireContext()
        ivAvatar = view.findViewById(R.id.ivDetailAvatar)
        btnSaveAvatar = view.findViewById(R.id.btnSaveGroupAvatar)
        ivAvatar?.load(group.avatar.resolveBackendUrl()) {
            crossfade(true)
            placeholder(R.drawable.ic_launcher_background)
            transformations(CircleCropTransformation())
        }
        view.findViewById<TextView>(R.id.tvDetailName).text = group.name
        view.findViewById<ImageView>(R.id.ivDetailLock).visibility =
            if (group.isGroupPrivate) View.VISIBLE else View.GONE

        btnFollowGroup = view.findViewById(R.id.btnFollowGroup)
        group.isFollowing?.let { initial ->
            btnFollowGroup?.visibility = View.VISIBLE
            isFollowingGroup = initial
            applyFollowState()
            btnFollowGroup?.setOnClickListener { toggleFollowGroup() }
        }

        val tvDescription = view.findViewById<TextView>(R.id.tvDetailDescription)
        if (!group.description.isNullOrBlank()) {
            tvDescription.visibility = View.VISIBLE
            tvDescription.text = group.description
        } else {
            tvDescription.visibility = View.GONE
        }

        view.findViewById<TextView>(R.id.tvDetailMembers).text =
            ctx.getString(R.string.groups_members_count, group.nbMembers)
        view.findViewById<TextView>(R.id.tvDetailPosts).text =
            ctx.getString(R.string.groups_posts_count, group.nbPosts)
        view.findViewById<TextView>(R.id.tvDetailCreated).text =
            ctx.getString(R.string.groups_created_on, group.creationDate.toShortDate())

        view.findViewById<MaterialButton>(R.id.btnViewMembers).setOnClickListener {
            val bundle = Bundle().apply {
                putInt(GroupMembersFragment.ARG_GROUP_ID, group.id)
                putBoolean(GroupMembersFragment.ARG_IS_ADMIN, group.isAdmin)
            }
            findNavController().navigate(R.id.groupMembersFragment, bundle)
        }

        val btnBanned = view.findViewById<MaterialButton>(R.id.btnViewBanned)
        val btnRequests = view.findViewById<MaterialButton>(R.id.btnViewRequests)
        val btnEdit = view.findViewById<MaterialButton>(R.id.btnEditGroup)
        val btnDelete = view.findViewById<MaterialButton>(R.id.btnDeleteGroup)
        val btnQuit = view.findViewById<MaterialButton>(R.id.btnQuitGroup)
        val fabEditAvatar = view.findViewById<FloatingActionButton>(R.id.fabEditGroupAvatar)

        if (group.isMember && !group.isAdmin) {
            btnQuit.visibility = View.VISIBLE
            btnQuit.setOnClickListener { confirmQuitGroup() }
        } else {
            btnQuit.visibility = View.GONE
        }

        if (group.isAdmin) {
            btnBanned.visibility = View.VISIBLE
            btnBanned.setOnClickListener {
                val bundle = Bundle().apply { putInt(GroupBannedFragment.ARG_GROUP_ID, group.id) }
                findNavController().navigate(R.id.groupBannedFragment, bundle)
            }
            if (group.isGroupPrivate) {
                btnRequests.visibility = View.VISIBLE
                btnRequests.setOnClickListener {
                    val bundle = Bundle().apply { putInt(GroupRequestsFragment.ARG_GROUP_ID, group.id) }
                    findNavController().navigate(R.id.groupRequestsFragment, bundle)
                }
            }

            fabEditAvatar.visibility = View.VISIBLE
            fabEditAvatar.setOnClickListener { imagePicker.pick() }
            btnSaveAvatar?.setOnClickListener {
                pickedAvatarUri?.let { uploadAvatar(it) }
            }

            btnEdit.visibility = View.VISIBLE
            btnEdit.setOnClickListener {
                val bundle = Bundle().apply {
                    putString(EditGroupFragment.ARG_GROUP, Gson().toJson(group))
                }
                findNavController().navigate(R.id.editGroupFragment, bundle)
            }

            btnDelete.visibility = View.VISIBLE
            btnDelete.setOnClickListener { confirmDeleteGroup() }
        }

        val btnJoin = view.findViewById<MaterialButton>(R.id.btnJoinGroupDetail)
        val btnSeeMember = view.findViewById<MaterialButton>(R.id.btnViewMembers)
        val btnSeePostsAction = view.findViewById<MaterialButton>(R.id.btnTabPosts)
        val toggle = view.findViewById<MaterialButtonToggleGroup>(R.id.tgGroupTabs)

        if (!group.isMember) {
            btnFollowGroup?.visibility = View.GONE
            btnJoin.visibility = View.VISIBLE
            btnJoin.text = ctx.getString(if (group.isGroupPrivate) R.string.btn_request_join_group else R.string.btn_join_group)
            btnJoin.setOnClickListener {
                requestToJoinGroup(group.id) {
                    if (!group.isGroupPrivate) {
                        fetchGroupDetail(group.id, requireView())
                    }
                }
            }

            if (!group.isGroupPrivate) {
                btnSeePostsAction.visibility = View.VISIBLE
                btnSeePostsAction.setOnClickListener {
                    toggle.check(R.id.btnTabPosts)
                }
            } else {
                btnSeeMember.visibility = View.GONE
            }
        }
    }

    private fun uploadAvatar(uri: Uri) {
        val ctx = context ?: return
        val bytes = runCatching {
            ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull() ?: run {
            Toast.makeText(ctx, R.string.error_group_avatar, Toast.LENGTH_LONG).show()
            return
        }

        val mimeType = ctx.contentResolver.getType(uri) ?: "image/jpeg"
        val fileName = "group_avatar_${System.currentTimeMillis()}.jpg"

        btnSaveAvatar?.isEnabled = false
        ApiClient.patchMultipart("group/admin/avatar/${group.id}", "avatar", fileName, bytes, mimeType) { _, _, error ->
            activity?.runOnUiThread {
                btnSaveAvatar?.isEnabled = true
                if (error == null) {
                    Toast.makeText(ctx, R.string.success_group_avatar, Toast.LENGTH_SHORT).show()
                    btnSaveAvatar?.visibility = View.GONE
                    pickedAvatarUri = null
                } else {
                    Toast.makeText(ctx, R.string.error_group_avatar, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun applyFollowState() {
        val btn = btnFollowGroup ?: return
        btn.setImageResource(if (isFollowingGroup) R.drawable.ic_bell_filled else R.drawable.ic_bell)
        btn.setColorFilter(
            androidx.core.content.ContextCompat.getColor(
                requireContext(),
                if (isFollowingGroup) R.color.primary else R.color.text_secondary
            )
        )
    }

    private fun toggleFollowGroup() {
        val target = !isFollowingGroup
        btnFollowGroup?.isEnabled = false

        val callback: (String?, Int, String?) -> Unit = { _, _, error ->
            activity?.runOnUiThread {
                btnFollowGroup?.isEnabled = true
                if (error == null) {
                    isFollowingGroup = target
                    applyFollowState()
                    val msg = if (target) R.string.success_followed else R.string.success_unfollowed
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, R.string.error_follow_action, Toast.LENGTH_SHORT).show()
                }
            }
        }

        if (target) {
            ApiClient.post("notification/group/${group.id}", emptyMap<String, String>(), callback)
        } else {
            ApiClient.delete("notification/group/${group.id}", callback)
        }
    }

    private fun confirmQuitGroup() {
        val ctx = context ?: return
        MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.groups_quit_confirm_title)
            .setMessage(R.string.groups_quit_confirm_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.groups_quit_btn) { _, _ -> performQuitGroup() }
            .show()
    }

    private fun performQuitGroup() {
        ApiClient.delete("group/quit/${group.id}") { _, _, error ->
            activity?.runOnUiThread {
                if (error == null) {
                    Toast.makeText(context, R.string.success_group_quit, Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                } else {
                    Toast.makeText(context, R.string.error_generic_action, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun confirmDeleteGroup() {
        val ctx = context ?: return
        MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.groups_delete_confirm_title)
            .setMessage(R.string.groups_delete_confirm_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.groups_admin_delete_btn) { _, _ -> performDeleteGroup() }
            .show()
    }

    private fun performDeleteGroup() {
        ApiClient.delete("group/admin/${group.id}") { _, code, error ->
            activity?.runOnUiThread {
                if (error == null) {
                    Toast.makeText(context, R.string.success_group_deleted, Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                } else if (code == 409){
                    Toast.makeText(context, R.string.groups_admin_fail_delete_group, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, R.string.error_generic_action, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupPostsTab() {
        postsAdapter = buildPostsAdapter(onChanged = {
            postsLoaded = false
            fetchPosts()
        })
        rvPosts.adapter = postsAdapter
    }

    private fun setupTabs(view: View) {
        val toggle = view.findViewById<MaterialButtonToggleGroup>(R.id.tgGroupTabs)

        if (!group.isMember && group.isGroupPrivate) {
            toggle.visibility = View.GONE
        }

        toggle.check(R.id.btnTabInfo)
        toggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            when (checkedId) {
                R.id.btnTabInfo -> {
                    scrollInfo.visibility = View.VISIBLE
                    rvPosts.visibility = View.GONE
                }
                R.id.btnTabPosts -> {
                    scrollInfo.visibility = View.GONE
                    rvPosts.visibility = View.VISIBLE
                    if (!postsLoaded) fetchPosts()
                }
            }
        }
    }

    private fun fetchPosts() {
        ApiClient.get("group/${group.id}/posts") { body, _, error ->
            activity?.runOnUiThread {
                if (error == null && body != null) {
                    try {
                        val type = object : TypeToken<List<PostDto>>() {}.type
                        val posts: List<PostDto> = Gson().fromJson(body, type)
                        postsAdapter.submitList(posts)
                        postsLoaded = true
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    companion object {
        const val ARG_GROUP_ID = "groupId"
    }
}
