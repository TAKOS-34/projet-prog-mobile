package com.example.myapplication.fragment.post

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.myapplication.R
import com.example.myapplication.dto.post.PostDto
import com.example.myapplication.dto.post.UpdatePostRequestDto
import com.example.myapplication.utils.ApiClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson

class EditPostFragment : Fragment() {

    private lateinit var post: PostDto

    private lateinit var etTitle: TextInputEditText
    private lateinit var etLocation: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var btnUpdate: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_edit_post, container, false)

        val json = arguments?.getString(ARG_POST) ?: return view
        post = Gson().fromJson(json, PostDto::class.java)

        etTitle = view.findViewById(R.id.etTitle)
        etLocation = view.findViewById(R.id.etLocation)
        etDescription = view.findViewById(R.id.etDescription)
        btnUpdate = view.findViewById(R.id.btnUpdatePost)

        etTitle.hint = post.title
        etLocation.hint = post.localisation
        etDescription.hint = post.description.orEmpty()

        view.findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            findNavController().navigateUp()
        }

        btnUpdate.setOnClickListener { performUpdate() }

        return view
    }

    private fun performUpdate() {
        val newTitle = etTitle.text.toString().trim().takeIf { it.isNotEmpty() && it != post.title }
        val newLocation = etLocation.text.toString().trim()
            .takeIf { it.isNotEmpty() && it != post.localisation }
        val newDescription = etDescription.text.toString().trim()
            .takeIf { it.isNotEmpty() && it != post.description.orEmpty() }

        val dto = UpdatePostRequestDto(
            title = newTitle,
            localisation = newLocation,
            description = newDescription
        )

        btnUpdate.isEnabled = false
        ApiClient.patch("post/${post.id}", dto) { _, _, error ->
            activity?.runOnUiThread {
                btnUpdate.isEnabled = true
                if (error == null) {
                    Toast.makeText(context, R.string.success_post_updated, Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                } else {
                    Toast.makeText(context, R.string.error_post_update, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    companion object {
        const val ARG_POST = "post"
    }
}
