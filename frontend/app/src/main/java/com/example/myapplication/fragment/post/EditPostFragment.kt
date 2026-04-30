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

import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import com.example.myapplication.dto.post.PostType
import com.google.android.material.textfield.TextInputLayout

class EditPostFragment : Fragment() {

    private lateinit var post: PostDto

    private lateinit var etTitle: TextInputEditText
    private lateinit var tilType: TextInputLayout
    private lateinit var atvType: AutoCompleteTextView
    private lateinit var etDescription: TextInputEditText
    private lateinit var btnUpdate: MaterialButton

    private var selectedPostType: PostType? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_edit_post, container, false)

        val json = arguments?.getString(ARG_POST) ?: return view
        post = Gson().fromJson(json, PostDto::class.java)

        etTitle = view.findViewById(R.id.etTitle)
        tilType = view.findViewById(R.id.tilType)
        atvType = view.findViewById(R.id.atvType)
        etDescription = view.findViewById(R.id.etDescription)
        btnUpdate = view.findViewById(R.id.btnUpdatePost)

        etTitle.hint = post.title
        etDescription.hint = post.description.orEmpty()

        setupTypeDropdown()

        view.findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            findNavController().navigateUp()
        }

        btnUpdate.setOnClickListener { performUpdate() }

        return view
    }

    private fun setupTypeDropdown() {
        val types = PostType.values()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, types.map { getString(it.labelRes) })
        atvType.setAdapter(adapter)

        val initialType = types.find { it.name == post.type }
        initialType?.let {
            atvType.setText(getString(it.labelRes), false)
            selectedPostType = it
        }

        atvType.setOnItemClickListener { _, _, position, _ ->
            selectedPostType = types[position]
        }
    }

    private fun performUpdate() {
        val newTitle = etTitle.text.toString().trim().takeIf { it.isNotEmpty() && it != post.title }
        val newType = selectedPostType?.name?.takeIf { it != post.type }
        val newDescription = etDescription.text.toString().trim()
            .takeIf { it.isNotEmpty() && it != post.description.orEmpty() }

        val dto = UpdatePostRequestDto(
            title = newTitle,
            type = newType,
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
