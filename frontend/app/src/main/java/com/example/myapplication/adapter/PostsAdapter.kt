package com.example.myapplication.adapter

import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.myapplication.R
import com.example.myapplication.dto.post.PostDto
import com.example.myapplication.utils.AdminGroupsCache
import com.example.myapplication.utils.DateUtils
import com.example.myapplication.utils.SessionManager
import com.example.myapplication.utils.resolveBackendUrl
import com.google.android.material.chip.Chip
import androidx.core.graphics.toColorInt

class PostsAdapter(
    private val onLike: (PostDto, Boolean) -> Unit,
    private val onComment: (PostDto) -> Unit,
    private val onReport: (PostDto) -> Unit,
    private val onLocation: (PostDto) -> Unit,
    private val onEdit: (PostDto) -> Unit = {},
    private val onDelete: (PostDto) -> Unit = {},
    private val onUserClick: (PostDto) -> Unit = {},
    private val onImageClick: (PostDto) -> Unit = {}
) : ListAdapter<PostDto, PostsAdapter.PostViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view, onLike, onComment, onReport, onLocation, onEdit, onDelete, onUserClick, onImageClick)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: PostViewHolder) {
        super.onViewRecycled(holder)
        holder.releasePlayer()
    }

    class PostViewHolder(
        itemView: View,
        private val onLike: (PostDto, Boolean) -> Unit,
        private val onComment: (PostDto) -> Unit,
        private val onReport: (PostDto) -> Unit,
        private val onLocation: (PostDto) -> Unit,
        private val onEdit: (PostDto) -> Unit,
        private val onDelete: (PostDto) -> Unit,
        private val onUserClick: (PostDto) -> Unit,
        private val onImageClick: (PostDto) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val ivImage: ImageView = itemView.findViewById(R.id.ivPostImage)
        private val btnEdit: ImageView = itemView.findViewById(R.id.btnEdit)
        private val btnDelete: ImageView = itemView.findViewById(R.id.btnDelete)
        private val ivAvatar: ImageView = itemView.findViewById(R.id.ivAvatar)
        private val llGroupBadge: LinearLayout = itemView.findViewById(R.id.llGroupBadge)
        private val ivGroupBadgeAvatar: ImageView = itemView.findViewById(R.id.ivGroupBadgeAvatar)
        private val tvGroupBadgeName: TextView = itemView.findViewById(R.id.tvGroupBadgeName)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvPostTitle)
        private val tvAuthor: TextView = itemView.findViewById(R.id.tvPostAuthor)
        private val tvLocation: TextView = itemView.findViewById(R.id.tvPostLocation)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private val llAudio: LinearLayout = itemView.findViewById(R.id.llAudio)
        private val btnPlayAudio: ImageView = itemView.findViewById(R.id.btnPlayAudio)
        private val tvAudioTimer: TextView = itemView.findViewById(R.id.tvAudioTimer)
        private val llTags: LinearLayout = itemView.findViewById(R.id.llTags)
        private val btnLike: ImageView = itemView.findViewById(R.id.btnLike)
        private val tvLikeCount: TextView = itemView.findViewById(R.id.tvLikeCount)
        private val btnComment: ImageView = itemView.findViewById(R.id.btnComment)
        private val tvCommentCount: TextView = itemView.findViewById(R.id.tvCommentCount)
        private val btnLocation: ImageView = itemView.findViewById(R.id.btnLocation)
        private val btnReport: ImageView = itemView.findViewById(R.id.btnReport)

        private var isLikedCurrent = false
        private var likeCountCurrent = 0
        private var mediaPlayer: MediaPlayer? = null
        private var isPlaying = false
        private var audioDurationMs = 0
        private val handler = Handler(Looper.getMainLooper())
        private val progressRunnable = object : Runnable {
            override fun run() {
                val mp = mediaPlayer ?: return
                val current = mp.currentPosition
                val duration = if (audioDurationMs > 0) audioDurationMs else mp.duration.coerceAtLeast(0)
                tvAudioTimer.text = "${formatTime(current)} / ${formatTime(duration)}"
                handler.postDelayed(this, 200)
            }
        }

        fun bind(post: PostDto) {
            val context = itemView.context
            val dp = context.resources.displayMetrics.density

            releasePlayer()

            ivImage.load(post.image.resolveBackendUrl()) { crossfade(true) }
            ivAvatar.load(post.avatar.resolveBackendUrl()) {
                crossfade(true)
                transformations(CircleCropTransformation())
            }

            tvTitle.text = post.title
            tvAuthor.text = "${context.getString(R.string.by)} ${post.username} · ${DateUtils.formatRelativeDate(context, post.creationDate)}"
            tvLocation.text = post.localisation
            tvCommentCount.text = post.nbComments.toString()

            if (post.groupId != null && !post.groupName.isNullOrBlank()) {
                llGroupBadge.visibility = View.VISIBLE
                tvGroupBadgeName.text = post.groupName
                post.groupAvatar?.takeIf { it.isNotBlank() }?.let { url ->
                    ivGroupBadgeAvatar.load(url.resolveBackendUrl()) {
                        crossfade(true)
                        transformations(CircleCropTransformation())
                    }
                } ?: ivGroupBadgeAvatar.setImageDrawable(null)
            } else {
                llGroupBadge.visibility = View.GONE
            }

            if (!post.description.isNullOrBlank()) {
                tvDescription.text = post.description
                tvDescription.visibility = View.VISIBLE
            } else {
                tvDescription.visibility = View.GONE
            }

            if (!post.audio.isNullOrBlank() && post.audioDuration is Int) {
                llAudio.visibility = View.VISIBLE
                audioDurationMs = post.audioDuration
                updateAudioButton()
                tvAudioTimer.text = "0:00 / ${formatTime(audioDurationMs)}"
                btnPlayAudio.setOnClickListener { toggleAudio(post.audio.resolveBackendUrl()) }
            } else {
                llAudio.visibility = View.GONE
            }

            isLikedCurrent = post.isLiked
            likeCountCurrent = post.nbLikes
            applyLikeState(context)

            btnLike.setOnClickListener {
                isLikedCurrent = !isLikedCurrent
                likeCountCurrent += if (isLikedCurrent) 1 else -1
                applyLikeState(context)
                onLike(post, isLikedCurrent)
            }

            btnComment.setOnClickListener { onComment(post) }
            btnReport.setOnClickListener { onReport(post) }
            btnLocation.setOnClickListener { onLocation(post) }

            ivAvatar.setOnClickListener { onUserClick(post) }
            tvAuthor.setOnClickListener { onUserClick(post) }
            ivImage.setOnClickListener { onImageClick(post) }

            val isOwner = post.userId == SessionManager.getUserId()
            val canModerate = isOwner || AdminGroupsCache.isAdminOf(post.groupId)

            if (isOwner) {
                btnEdit.visibility = View.VISIBLE
                btnEdit.setOnClickListener { onEdit(post) }
            } else {
                btnEdit.visibility = View.GONE
            }

            if (canModerate) {
                btnDelete.visibility = View.VISIBLE
                btnDelete.setOnClickListener { onDelete(post) }
            } else {
                btnDelete.visibility = View.GONE
            }

            llTags.removeAllViews()
            post.tags.forEach { tag ->
                val chip = Chip(context).apply {
                    text = "#$tag"
                    chipBackgroundColor = android.content.res.ColorStateList.valueOf("#D1FAE5".toColorInt())
                    setTextColor("#065F46".toColorInt())
                    isClickable = false
                    isCheckable = false
                    chipStrokeWidth = 0f
                    chipMinHeight = 0f
                    textSize = 11f
                    chipStartPadding = 10 * dp
                    chipEndPadding = 10 * dp
                    setPadding(0, (6 * dp).toInt(), 0, (6 * dp).toInt())
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { marginEnd = (8 * dp).toInt() }
                }
                llTags.addView(chip)
            }
        }

        private fun toggleAudio(url: String) {
            if (isPlaying) {
                mediaPlayer?.pause()
                isPlaying = false
                handler.removeCallbacks(progressRunnable)
            } else {
                if (mediaPlayer == null) {
                    mediaPlayer = MediaPlayer().apply {
                        setOnPreparedListener {
                            start()
                            handler.post(progressRunnable)
                        }
                        setOnCompletionListener {
                            this@PostViewHolder.isPlaying = false
                            handler.removeCallbacks(progressRunnable)
                            updateAudioButton()
                        }
                        setDataSource(url)
                        prepareAsync()
                    }
                } else {
                    mediaPlayer?.start()
                    handler.post(progressRunnable)
                }
                isPlaying = true
            }
            updateAudioButton()
        }

        private fun formatTime(ms: Int): String {
            val s = ms / 1000
            return "%d:%02d".format(s / 60, s % 60)
        }

        private fun updateAudioButton() {
            btnPlayAudio.setImageResource(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            )
        }

        fun releasePlayer() {
            handler.removeCallbacks(progressRunnable)
            mediaPlayer?.release()
            mediaPlayer = null
            isPlaying = false
            audioDurationMs = 0
        }

        private fun applyLikeState(context: android.content.Context) {
            btnLike.setImageResource(if (isLikedCurrent) R.drawable.ic_like_filled else R.drawable.ic_like)
            btnLike.setColorFilter(
                ContextCompat.getColor(context, if (isLikedCurrent) R.color.primary else R.color.text_secondary)
            )
            tvLikeCount.text = likeCountCurrent.toString()
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<PostDto>() {
            override fun areItemsTheSame(oldItem: PostDto, newItem: PostDto) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: PostDto, newItem: PostDto) = oldItem == newItem
        }
    }
}
