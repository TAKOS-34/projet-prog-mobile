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
import com.example.myapplication.dto.group.GroupInfosDto
import com.example.myapplication.dto.post.PostType
import com.example.myapplication.utils.ApiClient
import com.example.myapplication.utils.LocalisationSuggester
import com.example.myapplication.utils.resolveBackendUrl
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.view.inputmethod.EditorInfo
import android.widget.AutoCompleteTextView

class CreatePostFragment : Fragment() {

    private lateinit var ivPreview: ImageView
    private lateinit var cvPreview: CardView
    private lateinit var btnSelectImage: MaterialButton
    private lateinit var tilTitle: TextInputLayout
    private lateinit var etTitle: TextInputEditText
    private lateinit var tilType: TextInputLayout
    private lateinit var atvType: AutoCompleteTextView
    private lateinit var tilLocation: TextInputLayout
    private lateinit var etLocation: TextInputEditText
    private lateinit var tilDescription: TextInputLayout
    private lateinit var etDescription: TextInputEditText
    private lateinit var tilTags: TextInputLayout
    private lateinit var etTags: TextInputEditText
    private lateinit var cgSuggestedTags: ChipGroup
    private lateinit var cgLocationSuggestions: ChipGroup
    private lateinit var btnIaTags: MaterialButton
    private lateinit var pbIaLoading: ProgressBar
    private var popularTags: List<String> = emptyList()
    private lateinit var btnSelectAudio: MaterialButton
    private lateinit var tvAudioStatus: TextView
    private lateinit var btnPublish: MaterialButton
    private lateinit var tilGroup: TextInputLayout
    private lateinit var etGroup: TextInputEditText

    private var selectedImageUri: Uri? = null
    private var selectedAudioUri: Uri? = null
    private var selectedGroupId: Int? = null
    private var selectedPostType: PostType? = null
    private var myGroups: List<GroupInfosDto> = emptyList()

    private val imagePicker = com.example.myapplication.utils.ImagePicker(this) { uri ->
        selectedImageUri = uri
        ivPreview.setImageURI(uri)
        cvPreview.visibility = View.VISIBLE
        btnIaTags.visibility = View.VISIBLE
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
        setupTypeDropdown()

        view.findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            findNavController().navigateUp()
        }

        btnSelectImage.setOnClickListener { imagePicker.pick() }
        btnSelectAudio.setOnClickListener { audioPickerLauncher.launch("audio/*") }
        btnIaTags.setOnClickListener { fetchIaSuggestions() }

        btnPublish.setOnClickListener {
            if (validateFields()) {
                performPublish()
            }
        }

        setupTagSuggestions()
        fetchPopularTags()
        setupLocationSuggestions()
        fetchMyGroups()

        return view
    }

    private fun fetchMyGroups() {
        ApiClient.get("group/my-groups") { body, _, error ->
            activity?.runOnUiThread {
                if (error == null && body != null) {
                    try {
                        val type = object : TypeToken<List<GroupInfosDto>>() {}.type
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
        val entries: List<GroupInfosDto?> = listOf(null) + myGroups

        val adapter = object : ArrayAdapter<GroupInfosDto?>(ctx, 0, entries) {
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

        etTags.filters = arrayOf(android.text.InputFilter { source, start, end, _, _, _ ->
            val filtered = StringBuilder()
            var changed = false
            for (i in start until end) {
                val c = source[i]
                if (c.isLetterOrDigit() || c == '-' || c == '_' || c == ',') filtered.append(c)
                else changed = true
            }
            if (changed) filtered.toString() else null
        })

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
            override fun afterTextChanged(s: Editable?) {
                val currentText = s?.toString() ?: return
                val committedTags = if (currentText.endsWith(","))
                    currentText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                else
                    currentText.split(",").map { it.trim() }.dropLast(1).filter { it.isNotEmpty() }
                for (i in 0 until cgSuggestedTags.childCount) {
                    val chip = cgSuggestedTags.getChildAt(i) as? Chip ?: continue
                    chip.isChecked = committedTags.contains(chip.text.toString())
                }
            }
        })

        etTags.imeOptions = EditorInfo.IME_ACTION_DONE
        etTags.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val current = etTags.text.toString()
                if (current.isNotEmpty() && !current.endsWith(",")) etTags.append(",")
                true
            } else false
        }
    }

    private fun setupLocationSuggestions() {
        val handler = Handler(Looper.getMainLooper())
        var pending: Runnable? = null
        var ignoreNext = false

        etLocation.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                pending?.let { handler.removeCallbacks(it) }
                if (ignoreNext) { ignoreNext = false; return }
                val q = s?.toString()?.trim().orEmpty()
                if (q.length < 2) {
                    cgLocationSuggestions.removeAllViews()
                    return
                }
                pending = Runnable {
                    LocalisationSuggester.suggest(q) { results ->
                        activity?.runOnUiThread { renderLocationChips(results) { picked ->
                            ignoreNext = true
                            etLocation.setText(picked)
                            etLocation.setSelection(etLocation.text?.length ?: 0)
                            cgLocationSuggestions.removeAllViews()
                        } }
                    }
                }
                handler.postDelayed(pending!!, 350)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun renderLocationChips(
        results: List<com.example.myapplication.utils.LocalisationSuggestion>,
        onPick: (String) -> Unit
    ) {
        cgLocationSuggestions.removeAllViews()
        results.forEach { suggestion ->
            val chip = Chip(context).apply {
                text = suggestion.label
                isClickable = true
                setOnClickListener { onPick(suggestion.name) }
            }
            cgLocationSuggestions.addView(chip)
        }
    }

    private fun fetchPopularTags() {
        ApiClient.get("tag/popular") { body, _, _ ->
            body?.let { json ->
                try { popularTags = Gson().fromJson(json, Array<String>::class.java).toList() } catch (e: Exception) {}
                updateTagChips(json)
            }
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
                val suggestions = Gson().fromJson(json, Array<String>::class.java).toMutableList()
                popularTags.forEach { if (!suggestions.contains(it)) suggestions.add(it) }
                val currentText = etTags.text.toString()
                val committedTags = if (currentText.endsWith(","))
                    currentText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                else
                    currentText.split(",").map { it.trim() }.dropLast(1).filter { it.isNotEmpty() }
                committedTags.forEach { if (!suggestions.contains(it)) suggestions.add(it) }
                val partial = if (!currentText.endsWith(",")) currentText.split(",").lastOrNull()?.trim() ?: "" else ""
                if (partial.isNotEmpty() && !suggestions.contains(partial)) suggestions.add(0, partial)
                cgSuggestedTags.removeAllViews()
                suggestions.forEach { tag ->
                    val chip = Chip(context).apply {
                        text = tag
                        isClickable = true
                        isCheckable = true
                        isChecked = committedTags.contains(tag)
                        setOnClickListener { addTagToInput(tag) }
                    }
                    cgSuggestedTags.addView(chip)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun addTagToInput(tag: String) {
        val currentText = etTags.text.toString()
        val committed = if (currentText.endsWith(","))
            currentText.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
        else
            currentText.split(",").map { it.trim() }.dropLast(1).filter { it.isNotEmpty() }.toMutableList()
        if (committed.contains(tag)) committed.remove(tag) else committed.add(tag)
        etTags.setText(if (committed.isEmpty()) "" else committed.joinToString(",") + ",")
        etTags.setSelection(etTags.text?.length ?: 0)
        for (i in 0 until cgSuggestedTags.childCount) {
            val chip = cgSuggestedTags.getChildAt(i) as? Chip ?: continue
            chip.isChecked = committed.contains(chip.text.toString())
        }
    }

    private fun initViews(view: View) {
        ivPreview = view.findViewById(R.id.ivPreview)
        cvPreview = view.findViewById(R.id.cvPreview)
        btnIaTags = view.findViewById(R.id.btnIaTags)
        pbIaLoading = view.findViewById(R.id.pbIaLoading)
        btnSelectImage = view.findViewById(R.id.btnSelectImage)
        tilTitle = view.findViewById(R.id.tilTitle)
        etTitle = view.findViewById(R.id.etTitle)
        tilType = view.findViewById(R.id.tilType)
        atvType = view.findViewById(R.id.atvType)
        tilLocation = view.findViewById(R.id.tilLocation)
        etLocation = view.findViewById(R.id.etLocation)
        tilDescription = view.findViewById(R.id.tilDescription)
        etDescription = view.findViewById(R.id.etDescription)
        tilTags = view.findViewById(R.id.tilTags)
        etTags = view.findViewById(R.id.etTags)
        cgSuggestedTags = view.findViewById(R.id.cgSuggestedTags)
        cgLocationSuggestions = view.findViewById(R.id.cgLocationSuggestions)
        btnSelectAudio = view.findViewById(R.id.btnSelectAudio)
        tvAudioStatus = view.findViewById(R.id.tvAudioStatus)
        btnPublish = view.findViewById(R.id.btnPublish)
        tilGroup = view.findViewById(R.id.tilGroup)
        etGroup = view.findViewById(R.id.etGroup)
    }

    private fun setupTypeDropdown() {
        val types = PostType.values()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, types.map { getString(it.labelRes) })
        atvType.setAdapter(adapter)
        atvType.setOnItemClickListener { _, _, position, _ ->
            selectedPostType = types[position]
            tilType.error = null
        }
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

        if (selectedPostType == null) {
            tilType.error = getString(R.string.error_field_required)
            isValid = false
        } else {
            tilType.error = null
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
        val type = selectedPostType?.name ?: return
        val location = etLocation.text.toString().trim()
        val description = etDescription.text.toString().trim().takeIf { it.isNotEmpty() }
        val tagsString = etTags.text.toString().trim()
        val tags = if (tagsString.isNotEmpty()) tagsString.split(",").map { it.trim() } else null

        val parts = mutableMapOf<String, Any?>()
        parts["title"] = title
        parts["type"] = type
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