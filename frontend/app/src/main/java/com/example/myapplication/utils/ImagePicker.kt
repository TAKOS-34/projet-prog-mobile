package com.example.myapplication.utils

import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

class ImagePicker(
    private val fragment: Fragment,
    private val onPicked: (Uri) -> Unit
) {
    private var pendingCameraUri: Uri? = null

    private val galleryLauncher = fragment.registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) onPicked(uri) }

    private val cameraLauncher = fragment.registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success -> if (success) pendingCameraUri?.let(onPicked) }

    fun pick() {
        val ctx = fragment.requireContext()
        val items = arrayOf(
            ctx.getString(R.string.picker_source_gallery),
            ctx.getString(R.string.picker_source_camera)
        )
        MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.picker_source_title)
            .setItems(items) { _, which ->
                if (which == 0) galleryLauncher.launch("image/*") else launchCamera()
            }
            .show()
    }

    private fun launchCamera() {
        val ctx = fragment.requireContext()
        val file = File(ctx.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        pendingCameraUri = uri
        cameraLauncher.launch(uri)
    }
}
