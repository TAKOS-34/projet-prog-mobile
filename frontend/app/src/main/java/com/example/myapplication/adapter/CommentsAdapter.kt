package com.example.myapplication.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.myapplication.R
import com.example.myapplication.dto.comment.CommentDto
import com.example.myapplication.utils.DateUtils
import com.example.myapplication.utils.SessionManager
import com.example.myapplication.utils.resolveBackendUrl
import com.google.android.material.imageview.ShapeableImageView

class CommentsAdapter(
    private val onShowReplies: (CommentDto) -> Unit,
    private val onLike: (CommentDto, Boolean) -> Unit,
    private val onEditSave: (CommentDto, String) -> Unit,
    private val onDelete: (CommentDto) -> Unit,
    private val onReply: (CommentDto) -> Unit
) : ListAdapter<CommentDto, CommentsAdapter.CommentViewHolder>(DIFF) {

    private var expandedIds = setOf<Int>()

    fun setExpandedIds(ids: Set<Int>) {
        expandedIds = ids
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view, onShowReplies, onLike, onEditSave, onDelete, onReply)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(getItem(position), expandedIds.contains(getItem(position).id))
    }

    class CommentViewHolder(
        itemView: View,
        private val onShowReplies: (CommentDto) -> Unit,
        private val onLike: (CommentDto, Boolean) -> Unit,
        private val onEditSave: (CommentDto, String) -> Unit,
        private val onDelete: (CommentDto) -> Unit,
        private val onReply: (CommentDto) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val clRoot: ConstraintLayout = itemView.findViewById(R.id.clCommentRoot)
        private val ivAvatar: ShapeableImageView = itemView.findViewById(R.id.ivCommentAvatar)
        private val tvUsername: TextView = itemView.findViewById(R.id.tvCommentUsername)
        private val tvDate: TextView = itemView.findViewById(R.id.tvCommentDate)
        private val tvEdited: TextView = itemView.findViewById(R.id.tvEditedLabel)
        private val tvContent: TextView = itemView.findViewById(R.id.tvCommentContent)
        private val tvShowReplies: TextView = itemView.findViewById(R.id.tvShowReplies)
        private val btnReply: TextView = itemView.findViewById(R.id.btnReplyComment)
        private val btnLike: ImageView = itemView.findViewById(R.id.btnLikeComment)
        private val tvLikeCount: TextView = itemView.findViewById(R.id.tvCommentLikeCount)
        private val btnEdit: ImageView = itemView.findViewById(R.id.btnEditComment)
        private val btnDelete: ImageView = itemView.findViewById(R.id.btnDeleteComment)
        private val llEditContainer: LinearLayout = itemView.findViewById(R.id.llEditContainer)
        private val etEdit: EditText = itemView.findViewById(R.id.etEditComment)
        private val btnSave: TextView = itemView.findViewById(R.id.btnSaveEdit)
        private val btnCancelEdit: TextView = itemView.findViewById(R.id.btnCancelEdit)

        fun bind(comment: CommentDto, isExpanded: Boolean) {
            val context = itemView.context
            val dp = context.resources.displayMetrics.density

            val paddingStart = if (comment.parentId != null) (60 * dp).toInt() else (12 * dp).toInt()
            clRoot.setPadding(paddingStart, clRoot.paddingTop, clRoot.paddingEnd, clRoot.paddingBottom)

            ivAvatar.load(comment.avatar.resolveBackendUrl()) {
                crossfade(true)
                transformations(CircleCropTransformation())
            }
            tvUsername.text = comment.username
            tvDate.text = DateUtils.formatRelativeDate(context, comment.creationDate)
            
            if (comment.isEdited) {
                tvEdited.visibility = View.VISIBLE
                tvEdited.text = context.getString(R.string.comment_modified_at)
            } else {
                tvEdited.visibility = View.GONE
            }

            tvContent.text = comment.content
            tvContent.visibility = View.VISIBLE
            llEditContainer.visibility = View.GONE

            var isLikedCurrent = comment.isLiked
            var likeCount = comment.nbLikes

            fun updateLikeUi() {
                btnLike.setImageResource(if (isLikedCurrent) R.drawable.ic_like_filled else R.drawable.ic_like)
                btnLike.setColorFilter(
                    androidx.core.content.ContextCompat.getColor(context, if (isLikedCurrent) R.color.primary else R.color.text_secondary)
                )
                tvLikeCount.text = likeCount.toString()
            }

            updateLikeUi()

            btnLike.setOnClickListener {
                isLikedCurrent = !isLikedCurrent
                likeCount += if (isLikedCurrent) 1 else -1
                updateLikeUi()
                onLike(comment, isLikedCurrent)
            }

            btnReply.setOnClickListener { onReply(comment) }

            if (comment.userId == SessionManager.getUserId()) {
                btnEdit.visibility = View.VISIBLE
                btnDelete.visibility = View.VISIBLE
                btnEdit.setOnClickListener {
                    tvContent.visibility = View.GONE
                    btnReply.visibility = View.GONE
                    tvShowReplies.visibility = View.GONE
                    llEditContainer.visibility = View.VISIBLE
                    etEdit.setText(comment.content)
                }
                btnDelete.setOnClickListener { onDelete(comment) }

                fun closeEdit() {
                    tvContent.visibility = View.VISIBLE
                    llEditContainer.visibility = View.GONE
                    btnReply.visibility = View.VISIBLE
                    tvShowReplies.visibility = if (comment.nbReplies > 0) View.VISIBLE else View.GONE
                }

                btnSave.setOnClickListener {
                    val newContent = etEdit.text.toString().trim()
                    if (newContent.isNotEmpty() && newContent != comment.content) {
                        onEditSave(comment, newContent)
                    } else {
                        closeEdit()
                    }
                }
                btnCancelEdit.setOnClickListener { closeEdit() }
            } else {
                btnEdit.visibility = View.GONE
                btnDelete.visibility = View.GONE
            }

            if (comment.nbReplies > 0) {
                tvShowReplies.visibility = View.VISIBLE
                tvShowReplies.text = if (isExpanded) {
                    context.getString(R.string.comment_hide_responses)
                } else {
                    context.getString(R.string.comment_print_responses, comment.nbReplies)
                }
                tvShowReplies.setOnClickListener { onShowReplies(comment) }
            } else {
                tvShowReplies.visibility = View.GONE
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<CommentDto>() {
            override fun areItemsTheSame(oldItem: CommentDto, newItem: CommentDto) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: CommentDto, newItem: CommentDto) = oldItem == newItem
        }
    }
}
