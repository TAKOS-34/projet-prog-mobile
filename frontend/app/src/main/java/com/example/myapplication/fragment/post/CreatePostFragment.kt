package com.example.myapplication.fragment.post

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListPopupWindow
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import coil.load
import coil.transform.CircleCropTransformation
import com.example.myapplication.R
import com.example.myapplication.dto.group.GroupCardInfosDto
import com.example.myapplication.utils.ApiClient
import com.example.myapplication.utils.resolveBackendUrl
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class CreatePostFragment : Fragment() {

    private lateinit var ivPreview: ImageView
    private lateinit var cvPreview: CardView
    private lateinit var btnSelectImage: MaterialButton
    private lateinit var tilTitle: TextInputLayout
    private lateinit var etTitle: TextInputEditText
    private lateinit var tilLocation: TextInputLayout
    private lateinit var etLocation: TextInputEditText
    private lateinit var tilDescription: TextInputLayout
    private lateinit var etDescription: TextInputEditText
    private lateinit var tilTags: TextInputLayout
    private lateinit var etTags: TextInputEditText
    private lateinit var cgSuggestedTags: ChipGroup
    private lateinit var btnIaTags: MaterialButton
    private lateinit var pbIaLoading: ProgressBar
    private lateinit var btnSelectAudio: MaterialButton
    private lateinit var tvAudioStatus: TextView
    private lateinit var btnPublish: MaterialButton
    private lateinit var tilGroup: TextInputLayout
    private lateinit var etGroup: TextInputEditText

    private var selectedImageUri: Uri? = null
    private var selectedAudioUri: Uri? = null
    private var selectedGroupId: Int? = null
    private var myGroups: List<GroupCardInfosDto> = emptyList()

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            ivPreview.setImageURI(it)
            cvPreview.visibility = View.VISIBLE
            btnIaTags.visibility = View.VISIBLE
        }
    }

    private val audioPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedAudioUri = it
            tvAudioStatus.text = getString(R.string.audio_selection)
            tvAudioStatus.visibility = View.VISIBLE
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_create_post, container, false)
        initViews(view)

        view.findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            findNavController().navigateUp()
        }

        btnSelectImage.setOnClickListener { imagePickerLauncher.launch("image/*") }
        btnSelectAudio.setOnClickListener { audioPickerLauncher.launch("audio/*") }
        btnIaTags.setOnClickListener { fetchIaSuggestions() }

        btnPublish.setOnClickListener {
            if (validateFields()) {
                performPublish()
            }
        }

        setupTagSuggestions()
        fetchPopularTags()
        fetchMyGroups()

        return view
    }

    private fun fetchMyGroups() {
        ApiClient.get("group/my-groups") { body, _, error ->
            activity?.runOnUiThread {
                if (error == null && body != null) {
                    try {
                        val type = object : TypeToken<List<GroupCardInfosDto>>() {}.type
                        myGroups = Gson().fromJson(body, type)
                        bindGroupDropdown()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    Toast.makeText(context, R.string.error_load_groups, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun bindGroupDropdown() {
        val ctx = context ?: return
        val noneLabel = getString(R.string.group_selector_none)
        val entries: List<GroupCardInfosDto?> = listOf(null) + myGroups

        val adapter = object : ArrayAdapter<GroupCardInfosDto?>(ctx, 0, entries) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val row = convertView ?: LayoutInflater.from(ctx)
                    .inflate(R.layout.item_group_dropdown, parent, false)
                val group = getItem(position)
                val avatar = row.findViewById<ShapeableImageView>(R.id.ivGroupAvatar)
                val name = row.findViewById<TextView>(R.id.tvGroupName)
                if (group == null) {
                    name.text = noneLabel
                    avatar.setImageDrawable(null)
                    avatar.visibility = View.GONE
                } else {
                    name.text = group.name
                    avatar.visibility = View.VISIBLE
                    avatar.load(group.avatar.resolveBackendUrl()) {
                        crossfade(true)
                        placeholder(R.drawable.ic_launcher_background)
                        transformations(CircleCropTransformation())
                    }
                }
                return row
            }
        }

        val popup = ListPopupWindow(ctx).apply {
            setAdapter(adapter)
            anchorView = tilGroup
            isModal = true
        }

        etGroup.setText(noneLabel)
        selectedGroupId = null

        etGroup.setOnClickListener { popup.show() }

        popup.setOnItemClickListener { _, _, position, _ ->
            val group = entries[position]
            selectedGroupId = group?.id
            etGroup.setText(group?.name ?: noneLabel)
            popup.dismiss()
        }
    }

    private fun setupTagSuggestions() {
        var runnable: Runnable? = null
        val handler = Handler(Looper.getMainLooper())

        etTags.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                runnable?.let { handler.removeCallbacks(it) }
                val query = s?.toString()?.split(",")?.lastOrNull()?.trim() ?: ""
                if (query.length >= 2) {
                    runnable = Runnable { fetchSuggestions(query) }
                    handler.postDelayed(runnable!!, 300)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun fetchPopularTags() {
        ApiClient.get("tag/popular") { body, _, _ ->
            body?.let { updateTagChips(it) }
        }
    }

    private fun fetchSuggestions(query: String) {
        ApiClient.get("tag/suggest?tag=$query") { body, _, _ ->
            body?.let { updateTagChips(it) }
        }
    }

    private fun fetchIaSuggestions() {
        val context = context ?: return
        val uri = selectedImageUri ?: return
        val bytes = context.contentResolver.openInputStream(uri)?.readBytes() ?: return
        val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
        val extension = mime.split("/").lastOrNull() ?: "bin"

        btnIaTags.isEnabled = false
        pbIaLoading.visibility = View.VISIBLE
        ApiClient.getMultipart(
            path = "tag/ia-suggestions",
            fileKey = "post",
            fileName = "ia_check.$extension",
            fileBytes = bytes,
            fileMediaType = mime,
            onResult = { body, _, error ->
                activity?.runOnUiThread {
                    btnIaTags.isEnabled = true
                    pbIaLoading.visibility = View.GONE
                    if (error == null && body != null) {
                        updateTagChips(body)
                    }
                }
            }
        )
    }

    private fun updateTagChips(json: String) {
        activity?.runOnUiThread {
            try {
                val tags = Gson().fromJson(json, Array<String>::class.java)
                cgSuggestedTags.removeAllViews()
                tags.forEach { tag ->
                    val chip = Chip(context).apply {
                        text = tag
                        isClickable = true
                        setOnClickListener { addTagToInput(tag) }
                    }
                    cgSuggestedTags.addView(chip)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun addTagToInput(tag: String) {
        val currentText = etTags.text.toString()
        val tags = currentText.split(",").map { it.trim() }.toMutableList()
        if (tags.isNotEmpty() && tags.last().isNotEmpty()) {
            tags[tags.size - 1] = tag
        } else {
            tags.add(tag)
        }
        val newText = tags.filter { it.isNotEmpty() }.joinToString(", ") + ", "
        etTags.setText(newText)
        etTags.setSelection(etTags.text?.length ?: 0)
    }

    private fun initViews(view: View) {
        ivPreview = view.findViewById(R.id.ivPreview)
        cvPreview = view.findViewById(R.id.cvPreview)
        btnIaTags = view.findViewById(R.id.btnIaTags)
        pbIaLoading = view.findViewById(R.id.pbIaLoading)
        btnSelectImage = view.findViewById(R.id.btnSelectImage)
        tilTitle = view.findViewById(R.id.tilTitle)
        etTitle = view.findViewById(R.id.etTitle)
        tilLocation = view.findViewById(R.id.tilLocation)
        etLocation = view.findViewById(R.id.etLocation)
        tilDescription = view.findViewById(R.id.tilDescription)
        etDescription = view.findViewById(R.id.etDescription)
        tilTags = view.findViewById(R.id.tilTags)
        etTags = view.findViewById(R.id.etTags)
        cgSuggestedTags = view.findViewById(R.id.cgSuggestedTags)
        btnSelectAudio = view.findViewById(R.id.btnSelectAudio)
        tvAudioStatus = view.findViewById(R.id.tvAudioStatus)
        btnPublish = view.findViewById(R.id.btnPublish)
        tilGroup = view.findViewById(R.id.tilGroup)
        etGroup = view.findViewById(R.id.etGroup)
    }

    private fun validateFields(): Boolean {
        var isValid = true

        if (selectedImageUri == null) {
            Toast.makeText(context, R.string.error_image_required, Toast.LENGTH_SHORT).show()
            isValid = false
        }

        if (etTitle.text.isNullOrBlank()) {
            tilTitle.error = getString(R.string.error_title_required)
            isValid = false
        } else {
            tilTitle.error = null
        }

        if (etLocation.text.isNullOrBlank()) {
            tilLocation.error = getString(R.string.error_location_required)
            isValid = false
        } else {
            tilLocation.error = null
        }

        return isValid
    }

    private fun performPublish() {
        val context = context ?: return
        val imageUri = selectedImageUri ?: return

        val title = etTitle.text.toString().trim()
        val location = etLocation.text.toString().trim()
        val description = etDescription.text.toString().trim().takeIf { it.isNotEmpty() }
        val tagsString = etTags.text.toString().trim()
        val tags = if (tagsString.isNotEmpty()) tagsString.split(",").map { it.trim() } else null

        val parts = mutableMapOf<String, Any?>()
        parts["title"] = title
        parts["localisation"] = location
        parts["description"] = description
        parts["tags"] = tags
        parts["groupId"] = selectedGroupId

        val imageBytes = context.contentResolver.openInputStream(imageUri)?.readBytes() ?: return
        val imageMime = context.contentResolver.getType(imageUri) ?: "image/jpeg"

        var audioBytes: ByteArray? = null
        var audioMime: String? = null
        selectedAudioUri?.let {
            audioBytes = context.contentResolver.openInputStream(it)?.readBytes()
            audioMime = context.contentResolver.getType(it) ?: "audio/mpeg"
        }

        btnPublish.isEnabled = false

        ApiClient.postMultipart(
            path = "post",
            parts = parts,
            imageBytes = imageBytes,
            imageName = "post_image.${imageMime.split("/").last()}",
            imageMediaType = imageMime,
            audioBytes = audioBytes,
            audioName = if (audioBytes != null) "post_audio.${audioMime?.split("/")?.last()}" else null,
            audioMediaType = audioMime
        ) { _, code, error ->
            activity?.runOnUiThread {
                btnPublish.isEnabled = true
                if (error == null) {
                    Toast.makeText(context, R.string.success_post_created, Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                } else {
                    Toast.makeText(context, error ?: getString(R.string.error_post_failed), Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}