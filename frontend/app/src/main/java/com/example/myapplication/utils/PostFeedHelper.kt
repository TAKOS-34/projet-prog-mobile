package com.example.myapplication.utils

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.VelocityTracker
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.myapplication.R
import com.example.myapplication.adapter.PostsAdapter
import com.example.myapplication.utils.LocalisationFormat
import com.example.myapplication.dto.post.PostDto
import com.example.myapplication.dto.trip.TripFeedItemDto
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
    onLocation = { post -> showGpsDialog(post.localisation, post.lat, post.long, post.creationDate) },
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
        if (findNavController().currentDestination?.id != R.id.postViewerFragment) {
            val bundle = Bundle().apply { putString(PostViewerFragment.ARG_POST_ID, post.id) }
            findNavController().navigate(R.id.postViewerFragment, bundle)
        } else {
            showFullscreenImage(post.image)
        }
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
    } else null,
    onBookmark = if (SessionManager.getUserId() != null) { post, isNowBookmarked ->
        togglePostBookmark(post, isNowBookmarked)
    } else null
)

private fun Fragment.showFullscreenImage(imageUrl: String) {
    val ctx = context ?: return
    val metrics = ctx.resources.displayMetrics
    val dp = metrics.density
    val dialog = Dialog(ctx, android.R.style.Theme_Black_NoTitleBar_Fullscreen)

    val root = FrameLayout(ctx).apply { setBackgroundColor(Color.BLACK) }

    val iv = ImageView(ctx).apply {
        layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        scaleType = ImageView.ScaleType.FIT_CENTER
    }
    iv.load(imageUrl.resolveBackendUrl()) { crossfade(true) }
    root.addView(iv)

    var currentScale = 1f
    var startRawY = 0f
    var isDragging = false
    val velocityTracker = VelocityTracker.obtain()

    val scaleDetector = ScaleGestureDetector(ctx, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            currentScale = (currentScale * detector.scaleFactor).coerceIn(0.5f, 5f)
            iv.scaleX = currentScale
            iv.scaleY = currentScale
            return true
        }
    })

    iv.setOnTouchListener { _, event ->
        scaleDetector.onTouchEvent(event)
        velocityTracker.addMovement(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startRawY = event.rawY
                isDragging = false
            }
            MotionEvent.ACTION_MOVE -> {
                if (!scaleDetector.isInProgress && currentScale <= 1.05f) {
                    val dy = event.rawY - startRawY
                    if (!isDragging && dy > 10 * dp) isDragging = true
                    if (isDragging && dy >= 0f) {
                        iv.translationY = dy
                        val alpha = (1f - (dy / (400 * dp)).coerceIn(0f, 1f))
                        root.setBackgroundColor(Color.argb((alpha * 255).toInt(), 0, 0, 0))
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                velocityTracker.computeCurrentVelocity(1000)
                val vy = velocityTracker.yVelocity
                val dy = iv.translationY
                velocityTracker.clear()
                when {
                    dy > 150 * dp || vy > 1500 -> {
                        iv.animate()
                            .translationY(metrics.heightPixels.toFloat())
                            .alpha(0f)
                            .setDuration(250)
                            .withEndAction { dialog.dismiss() }
                            .start()
                    }
                    !isDragging && !scaleDetector.isInProgress -> dialog.dismiss()
                    else -> {
                        iv.animate().translationY(0f).alpha(1f).setDuration(200).start()
                        root.setBackgroundColor(Color.BLACK)
                    }
                }
                isDragging = false
            }
        }
        true
    }

    dialog.setContentView(root)
    dialog.show()
}

internal fun Fragment.showGpsDialog(name: String, lat: Double, long: Double, date: String? = null) {
    val ctx = context ?: return
    val dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_gps_info, null)

    dialogView.findViewById<TextView>(R.id.tvLocationTitle).text = LocalisationFormat.display(name)

    val cvDateBlock = dialogView.findViewById<android.view.View>(R.id.cvDateBlock)
    if (date != null) {
        dialogView.findViewById<TextView>(R.id.tvDate).text = DateUtils.formatAbsoluteDate(date)
    } else {
        cvDateBlock.visibility = android.view.View.GONE
    }

    dialogView.findViewById<TextView>(R.id.tvCoordinates).text =
        "%.4f N\n%.4f E".format(lat, long)

    val dialog = Dialog(ctx, android.R.style.Theme_Translucent_NoTitleBar)
    dialog.setContentView(dialogView)
    dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

    dialogView.findViewById<android.widget.ImageButton>(R.id.btnClosePopup).setOnClickListener { dialog.dismiss() }
    dialogView.findViewById<MaterialButton>(R.id.btnOpenMaps).setOnClickListener {
        val uri = Uri.parse("geo:$lat,$long?q=$lat,$long($name)")
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
    dialogView.findViewById<MaterialButton>(R.id.btnTravelPathRoute).setOnClickListener {
        dialog.dismiss()
        val bundle = Bundle().apply { putString("prefillCity", name) }
        findNavController().navigate(R.id.createTripFragment, bundle)
    }

    dialog.show()
}

private fun Fragment.togglePostBookmark(post: PostDto, isNowBookmarked: Boolean) {
    val msg = if (isNowBookmarked) R.string.success_bookmarked else R.string.success_unbookmarked
    val call: (String?, Int, String?) -> Unit = { _, _, error ->
        if (error == null) {
            activity?.runOnUiThread {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }
    if (isNowBookmarked) {
        ApiClient.post("bookmark/post/${post.id}", emptyMap<String, String>(), call)
    } else {
        ApiClient.delete("bookmark/post/${post.id}", call)
    }
}

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

internal fun Fragment.confirmDeleteTrip(trip: TripFeedItemDto, onDeleted: () -> Unit) {
    val ctx = context ?: return
    MaterialAlertDialogBuilder(ctx)
        .setTitle(R.string.trip_delete_confirm_title)
        .setMessage(R.string.post_delete_confirm_message)
        .setNegativeButton(R.string.cancel, null)
        .setPositiveButton(R.string.btn_delete_post) { _, _ ->
            ApiClient.delete("trip/${trip.id}") { _, _, error ->
                activity?.runOnUiThread {
                    if (error == null) {
                        Toast.makeText(context, R.string.success_trip_deleted, Toast.LENGTH_SHORT).show()
                        onDeleted()
                    } else {
                        Toast.makeText(context, R.string.error_post_delete, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        .show()
}
