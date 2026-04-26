package com.example.myapplication.fragment.group

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import coil.load
import coil.transform.CircleCropTransformation
import com.example.myapplication.R
import com.example.myapplication.utils.ApiClient
import com.example.myapplication.utils.ImagePicker
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class CreateGroupFragment : Fragment() {

    private lateinit var ivAvatar: ShapeableImageView
    private lateinit var tilName: TextInputLayout
    private lateinit var etName: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var switchPrivate: SwitchMaterial
    private lateinit var btnPublish: MaterialButton

    private var pickedAvatarUri: Uri? = null

    private val imagePicker = ImagePicker(this) { uri ->
        pickedAvatarUri = uri
        ivAvatar.load(uri) {
            crossfade(true)
            transformations(CircleCropTransformation())
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_create_group, container, false)

        ivAvatar = view.findViewById(R.id.ivGroupAvatar)
        tilName = view.findViewById(R.id.tilGroupName)
        etName = view.findViewById(R.id.etGroupName)
        etDescription = view.findViewById(R.id.etGroupDescription)
        switchPrivate = view.findViewById(R.id.switchGroupPrivate)
        btnPublish = view.findViewById(R.id.btnPublishGroup)

        view.findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            findNavController().navigateUp()
        }
        view.findViewById<MaterialButton>(R.id.btnSelectGroupAvatar).setOnClickListener {
            imagePicker.pick()
        }
        btnPublish.setOnClickListener { if (validate()) publish() }

        return view
    }

    private fun validate(): Boolean {
        val name = etName.text.toString().trim()
        return if (name.isEmpty()) {
            tilName.error = getString(R.string.error_group_name_required)
            false
        } else {
            tilName.error = null
            true
        }
    }

    private fun publish() {
        val ctx = context ?: return
        val name = etName.text.toString().trim()
        val description = etDescription.text.toString().trim().takeIf { it.isNotEmpty() }
        val isPrivate = switchPrivate.isChecked

        val parts = mutableMapOf<String, Any?>()
        parts["name"] = name
        parts["isGroupPrivate"] = isPrivate.toString()
        parts["description"] = description

        val uri = pickedAvatarUri
        btnPublish.isEnabled = false

        if (uri == null) {
            ApiClient.postMultipart(
                path = "group",
                parts = parts,
                imageBytes = null,
                imageName = null,
                imageMediaType = null
            ) { _, code, error -> handleResponse(code, error) }
        } else {
            val bytes = ctx.contentResolver.openInputStream(uri)?.readBytes()
            if (bytes == null) {
                btnPublish.isEnabled = true
                Toast.makeText(ctx, R.string.error_group_avatar, Toast.LENGTH_LONG).show()
                return
            }
            val mime = ctx.contentResolver.getType(uri) ?: "image/jpeg"
            val ext = mime.split("/").last()
            ApiClient.postMultipart(
                path = "group",
                parts = parts,
                imageBytes = bytes,
                imageName = "group_avatar.$ext",
                imageMediaType = mime,
                imageFieldKey = "avatar"
            ) { _, code, error -> handleResponse(code, error) }
        }
    }

    private fun handleResponse(code: Int, error: String?) {
        activity?.runOnUiThread {
            btnPublish.isEnabled = true
            if (error == null) {
                Toast.makeText(context, R.string.success_group_created, Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            } else if (code == 400) {
                Toast.makeText(context, R.string.error_group_name_already_set, Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, R.string.error_group_create, Toast.LENGTH_LONG).show()
            }
        }
    }
}
