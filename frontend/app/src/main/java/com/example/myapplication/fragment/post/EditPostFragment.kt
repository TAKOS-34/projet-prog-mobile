package com.example.myapplication.fragment.post

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.myapplication.R
import com.example.myapplication.dto.post.PostDto
import com.example.myapplication.dto.post.PostType
import com.example.myapplication.dto.post.UpdatePostRequestDto
import com.example.myapplication.utils.ApiClient
import com.example.myapplication.utils.PostMetrics
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson

class EditPostFragment : Fragment() {

    private lateinit var post: PostDto

    private lateinit var etTitle: TextInputEditText
    private lateinit var tilType: TextInputLayout
    private lateinit var atvType: AutoCompleteTextView
    private lateinit var etDescription: TextInputEditText
    private lateinit var btnUpdate: MaterialButton

    private lateinit var tvPriceLabel: TextView
    private lateinit var tilPrice: TextInputLayout
    private lateinit var etPrice: TextInputEditText
    private lateinit var divPrice: View
    private lateinit var tvDurationLabel: TextView
    private lateinit var tilDuration: TextInputLayout
    private lateinit var etDuration: TextInputEditText
    private lateinit var divDuration: View

    private var selectedPostType: PostType? = null
    private var selectedMinPrice: Int? = null
    private var selectedMaxPrice: Int? = null
    private var selectedMinDuration: Int? = null
    private var selectedMaxDuration: Int? = null

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
        tvPriceLabel = view.findViewById(R.id.tvPriceLabel)
        tilPrice = view.findViewById(R.id.tilPrice)
        etPrice = view.findViewById(R.id.etPrice)
        divPrice = view.findViewById(R.id.divPrice)
        tvDurationLabel = view.findViewById(R.id.tvDurationLabel)
        tilDuration = view.findViewById(R.id.tilDuration)
        etDuration = view.findViewById(R.id.etDuration)
        divDuration = view.findViewById(R.id.divDuration)

        etTitle.hint = post.title
        etDescription.hint = post.description.orEmpty()

        selectedMinPrice = post.minPrice
        selectedMaxPrice = post.maxPrice
        selectedMinDuration = post.minDuration
        selectedMaxDuration = post.maxDuration

        setupTypeDropdown()
        refreshMetricInputs()

        etPrice.setOnClickListener { showPriceDialog() }
        etDuration.setOnClickListener { showDurationDialog() }

        view.findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            findNavController().navigateUp()
        }

        btnUpdate.setOnClickListener { performUpdate() }

        return view
    }

    private fun setupTypeDropdown() {
        val types = PostType.entries
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, types.map { getString(it.labelRes) })
        atvType.setAdapter(adapter)

        val initialType = types.find { it.name == post.type }
        initialType?.let {
            atvType.setText(getString(it.labelRes), false)
            selectedPostType = it
        }

        atvType.setOnItemClickListener { _, _, position, _ ->
            selectedPostType = types[position]
            refreshMetricInputs()
        }
    }

    private fun refreshMetricInputs() {
        val type = selectedPostType
        val showPrice = PostMetrics.supportsPrice(type)
        val showDuration = PostMetrics.supportsDuration(type)
        val ctx = requireContext()

        tvPriceLabel.visibility = if (showPrice) View.VISIBLE else View.GONE
        tilPrice.visibility = if (showPrice) View.VISIBLE else View.GONE
        divPrice.visibility = if (showPrice) View.VISIBLE else View.GONE
        if (showPrice) {
            etPrice.setText(PostMetrics.formatPrice(ctx, selectedMinPrice, selectedMaxPrice).orEmpty())
        } else {
            selectedMinPrice = null
            selectedMaxPrice = null
            etPrice.setText("")
        }

        tvDurationLabel.visibility = if (showDuration) View.VISIBLE else View.GONE
        tilDuration.visibility = if (showDuration) View.VISIBLE else View.GONE
        divDuration.visibility = if (showDuration) View.VISIBLE else View.GONE
        if (showDuration) {
            etDuration.setText(PostMetrics.formatDuration(ctx, selectedMinDuration, selectedMaxDuration).orEmpty())
        } else {
            selectedMinDuration = null
            selectedMaxDuration = null
            etDuration.setText("")
        }
    }

    private fun showPriceDialog() {
        val ctx = requireContext()
        val labels = listOf(getString(R.string.picker_unspecified)) +
            PostMetrics.PRICE_RANGES.map { (min, max) -> PostMetrics.formatPrice(ctx, min, max).orEmpty() }
        MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.label_post_price)
            .setItems(labels.toTypedArray()) { _, position ->
                if (position == 0) {
                    selectedMinPrice = null
                    selectedMaxPrice = null
                    etPrice.setText("")
                } else {
                    val (min, max) = PostMetrics.PRICE_RANGES[position - 1]
                    selectedMinPrice = min
                    selectedMaxPrice = max
                    etPrice.setText(labels[position])
                }
            }
            .show()
    }

    private fun showDurationDialog() {
        val ctx = requireContext()
        val labels = listOf(getString(R.string.picker_unspecified)) +
            PostMetrics.DURATION_RANGES_MIN.map { (min, max) -> PostMetrics.formatDuration(ctx, min, max).orEmpty() }
        MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.label_post_duration)
            .setItems(labels.toTypedArray()) { _, position ->
                if (position == 0) {
                    selectedMinDuration = null
                    selectedMaxDuration = null
                    etDuration.setText("")
                } else {
                    val (min, max) = PostMetrics.DURATION_RANGES_MIN[position - 1]
                    selectedMinDuration = min
                    selectedMaxDuration = max
                    etDuration.setText(labels[position])
                }
            }
            .show()
    }

    private fun performUpdate() {
        val newTitle = etTitle.text.toString().trim().takeIf { it.isNotEmpty() && it != post.title }
        val newType = selectedPostType?.name?.takeIf { it != post.type }
        val newDescription = etDescription.text.toString().trim()
            .takeIf { it.isNotEmpty() && it != post.description.orEmpty() }

        val body = mutableMapOf<String, Any?>()
        if (newTitle != null) body["title"] = newTitle
        if (newType != null) body["type"] = newType
        if (newDescription != null) body["description"] = newDescription

        if (selectedMinPrice != post.minPrice) body["minPrice"] = selectedMinPrice
        if (selectedMaxPrice != post.maxPrice) body["maxPrice"] = selectedMaxPrice
        if (selectedMinDuration != post.minDuration) body["minDuration"] = selectedMinDuration
        if (selectedMaxDuration != post.maxDuration) body["maxDuration"] = selectedMaxDuration

        btnUpdate.isEnabled = false
        ApiClient.patchMap("post/${post.id}", body) { _, _, error ->
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
