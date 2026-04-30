package com.example.myapplication.utils

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.myapplication.R
import com.example.myapplication.adapter.PostsAdapter
import com.example.myapplication.dto.post.PostDto
import com.example.myapplication.fragment.post.EditPostFragment
import com.example.myapplication.fragment.post.LocalisationViewerFragment
import com.example.myapplication.fragment.post.PostViewerFragment
import com.example.myapplication.fragment.post.TagViewerFragment
import com.example.myapplication.fragment.profile.ProfileViewerFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson

fun Fragment.buildPostsAdapter(onChanged: () -> Unit): PostsAdapter = PostsAdapter(
    onLike = { post, isNowLiked -> togglePostLike(post, isNowLiked) },
    onComment = { post ->
        findNavController().navigate(R.id.commentsFragment, Bundle().apply { putString("postId", post.id) })
    },
    onReport = { post ->
        findNavController().navigate(R.id.reportFragment, Bundle().apply { putString("postId", post.id) })
    },
    onLocation = { post -> showGpsDialog(post) },
    onEdit = { post ->
        val bundle = Bundle().apply { putString(EditPostFragment.ARG_POST, Gson().toJson(post)) }
        findNavController().navigate(R.id.editPostFragment, bundle)
    },
    onDelete = { post -> confirmDeletePost(post, onChanged) },
    onUserClick = { post ->
        val bundle = Bundle().apply { putInt(ProfileViewerFragment.ARG_USER_ID, post.userId) }
        findNavController().navigate(R.id.profileViewerFragment, bundle)
    },
    onImageClick = { post ->
        val bundle = Bundle().apply { putString(PostViewerFragment.ARG_POST_ID, post.id) }
        findNavController().navigate(R.id.postViewerFragment, bundle)
    },
    onGroupClick = { post ->
        post.groupId?.let { id ->
            val bundle = Bundle().apply { putInt("groupId", id) }
            findNavController().navigate(R.id.groupDetailFragment, bundle)
        }
    },
    onTagClick = if (SessionManager.getUserId() != null) { tag ->
        val bundle = Bundle().apply { putString(TagViewerFragment.ARG_TAG, tag) }
        findNavController().navigate(R.id.tagViewerFragment, bundle)
    } else null,
    onLocationNameClick = if (SessionManager.getUserId() != null) { name ->
        val bundle = Bundle().apply { putString(LocalisationViewerFragment.ARG_LOCALISATION, name) }
        findNavController().navigate(R.id.localisationViewerFragment, bundle)
    } else null
)

private fun togglePostLike(post: PostDto, isNowLiked: Boolean) {
    if (isNowLiked) {
        ApiClient.post("like/post/${post.id}", emptyMap<String, String>()) { _, _, _ -> }
    } else {
        ApiClient.delete("like/post/${post.id}") { _, _, _ -> }
    }
}

private fun Fragment.confirmDeletePost(post: PostDto, onDeleted: () -> Unit) {
    val ctx = context ?: return
    MaterialAlertDialogBuilder(ctx)
        .setTitle(R.string.post_delete_confirm_title)
        .setMessage(R.string.post_delete_confirm_message)
        .setNegativeButton(R.string.cancel, null)
        .setPositiveButton(R.string.btn_delete_post) { _, _ -> deletePost(post, onDeleted) }
        .show()
}

private fun Fragment.deletePost(post: PostDto, onDeleted: () -> Unit) {
    ApiClient.delete("post/${post.id}") { _, _, error ->
        activity?.runOnUiThread {
            if (error == null) {
                Toast.makeText(context, R.string.success_post_deleted, Toast.LENGTH_SHORT).show()
                onDeleted()
            } else {
                Toast.makeText(context, R.string.error_post_delete, Toast.LENGTH_LONG).show()
            }
        }
    }
}

private fun Fragment.showGpsDialog(post: PostDto) {
    val ctx = context ?: return
    val dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_gps_info, null)

    dialogView.findViewById<TextView>(R.id.tvLocationTitle).text = post.localisation
    dialogView.findViewById<TextView>(R.id.tvDate).text = DateUtils.formatAbsoluteDate(post.creationDate)
    dialogView.findViewById<TextView>(R.id.tvCoordinates).text = "%.4f N\n%.4f E".format(post.lat, post.long)

    val dialog = Dialog(ctx, android.R.style.Theme_Translucent_NoTitleBar)
    dialog.setContentView(dialogView)
    dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

    dialogView.findViewById<MaterialCardView>(R.id.btnClosePopup).setOnClickListener { dialog.dismiss() }
    dialogView.findViewById<MaterialButton>(R.id.btnOpenMaps).setOnClickListener {
        val uri = Uri.parse("geo:${post.lat},${post.long}?q=${post.lat},${post.long}(${post.localisation})")
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
    dialogView.findViewById<MaterialButton>(R.id.btnTravelPathRoute).setOnClickListener { dialog.dismiss() }

    dialog.show()
}
