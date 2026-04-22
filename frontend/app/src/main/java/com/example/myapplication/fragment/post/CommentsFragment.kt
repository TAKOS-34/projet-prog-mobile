package com.example.myapplication.fragment.post

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.adapter.CommentsAdapter
import com.example.myapplication.dto.comment.CommentDto
import com.example.myapplication.dto.comment.CreateCommentDto
import com.example.myapplication.dto.comment.UpdateCommentDto
import com.example.myapplication.utils.ApiClient
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class CommentsFragment : Fragment() {

    private var postId: String? = null
    private lateinit var adapter: CommentsAdapter
    private lateinit var etComment: TextInputEditText
    private lateinit var btnSend: ImageView
    private lateinit var llReplyIndicator: LinearLayout
    private lateinit var tvReplyTo: TextView
    private lateinit var btnCancelReply: ImageView
    
    private var replyToCommentId: Int? = null
    private val currentComments = mutableListOf<CommentDto>()
    private val expandedCommentIds = mutableSetOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        postId = arguments?.getString("postId")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_comments, container, false)

        initViews(view)

        adapter = CommentsAdapter(
            onShowReplies = { comment -> toggleReplies(comment) },
            onLike = { comment, isNowLiked -> toggleCommentLike(comment, isNowLiked) },
            onEditSave = { comment, newContent -> updateComment(comment, newContent) },
            onDelete = { comment -> deleteComment(comment) },
            onReply = { comment -> setReplyTo(comment) }
        )
        view.findViewById<RecyclerView>(R.id.rvComments).adapter = adapter

        view.findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            findNavController().navigateUp()
        }

        btnSend.setOnClickListener { postComment() }
        btnCancelReply.setOnClickListener { cancelReply() }

        fetchComments()

        return view
    }

    private fun initViews(view: View) {
        etComment = view.findViewById(R.id.etComment)
        btnSend = view.findViewById(R.id.btnSendComment)
        llReplyIndicator = view.findViewById(R.id.llReplyIndicator)
        tvReplyTo = view.findViewById(R.id.tvReplyTo)
        btnCancelReply = view.findViewById(R.id.btnCancelReply)
    }

    private fun setReplyTo(comment: CommentDto) {
        replyToCommentId = comment.parentId ?: comment.id
        tvReplyTo.text = getString(R.string.comment_response, comment.username)
        llReplyIndicator.visibility = View.VISIBLE
        etComment.requestFocus()
        val imm = context?.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
        imm?.showSoftInput(etComment, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }

    private fun cancelReply() {
        replyToCommentId = null
        llReplyIndicator.visibility = View.GONE
    }

    private fun fetchComments() {
        val id = postId ?: return
        ApiClient.get("post/$id/comments") { body, _, error ->
            activity?.runOnUiThread {
                if (error == null && body != null) {
                    val type = object : TypeToken<List<CommentDto>>() {}.type
                    val comments = Gson().fromJson<List<CommentDto>>(body, type)
                    currentComments.clear()
                    currentComments.addAll(comments)
                    expandedCommentIds.clear()
                    updateAdapter()
                }
            }
        }
    }

    private fun toggleReplies(parent: CommentDto) {
        val pId = postId ?: return
        
        if (expandedCommentIds.contains(parent.id)) {
            expandedCommentIds.remove(parent.id)
            currentComments.removeAll { it.parentId == parent.id }
            updateAdapter()
        } else {
            ApiClient.get("post/$pId/${parent.id}/replies") { body, _, error ->
                activity?.runOnUiThread {
                    if (error == null && body != null) {
                        val type = object : TypeToken<List<CommentDto>>() {}.type
                        val replies = Gson().fromJson<List<CommentDto>>(body, type)
                        
                        val index = currentComments.indexOfFirst { it.id == parent.id }
                        if (index != -1) {
                            val repliesWithParent = replies.map { it.copy(parentId = parent.id) }
                            currentComments.addAll(index + 1, repliesWithParent)
                            expandedCommentIds.add(parent.id)
                            updateAdapter()
                        }
                    }
                }
            }
        }
    }

    private fun updateAdapter() {
        adapter.setExpandedIds(expandedCommentIds)
        adapter.submitList(currentComments.toList())
    }

    private fun toggleCommentLike(comment: CommentDto, isNowLiked: Boolean) {
        if (isNowLiked) {
            ApiClient.post("like/comment/${comment.id}", emptyMap<String, String>()) { _, _, _ -> }
        } else {
            ApiClient.delete("like/comment/${comment.id}") { _, _, _ -> }
        }
    }

    private fun deleteComment(comment: CommentDto) {
        val ctx = context ?: return
        com.google.android.material.dialog.MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.delete_comment_confirm_title)
            .setMessage(R.string.delete_comment_confirm_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete_comment_btn) { _, _ ->
                ApiClient.delete("comment/${comment.id}") { _, _, error ->
                    activity?.runOnUiThread {
                        if (error == null) fetchComments()
                        else Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    private fun updateComment(comment: CommentDto, newContent: String) {
        val dto = UpdateCommentDto(newContent)
        ApiClient.patch("comment/${comment.id}", dto) { _, _, error ->
            activity?.runOnUiThread {
                if (error == null) fetchComments()
                else Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun postComment() {
        val id = postId ?: return
        val content = etComment.text.toString().trim()
        if (content.isEmpty()) return

        btnSend.isEnabled = false
        val dto = CreateCommentDto(content, replyToCommentId)

        ApiClient.post("comment/$id", dto) { _, _, error ->
            activity?.runOnUiThread {
                btnSend.isEnabled = true
                if (error == null) {
                    etComment.setText("")
                    cancelReply()
                    fetchComments()
                } else {
                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
