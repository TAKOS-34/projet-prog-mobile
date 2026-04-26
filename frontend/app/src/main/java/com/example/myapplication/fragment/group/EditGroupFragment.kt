package com.example.myapplication.fragment.group

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.myapplication.R
import com.example.myapplication.dto.group.GroupCardInfosDto
import com.example.myapplication.dto.group.UpdateGroupRequestDto
import com.example.myapplication.utils.ApiClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson

class EditGroupFragment : Fragment() {

    private lateinit var group: GroupCardInfosDto

    private lateinit var tilName: TextInputLayout
    private lateinit var etName: TextInputEditText
    private lateinit var tilDescription: TextInputLayout
    private lateinit var etDescription: TextInputEditText
    private lateinit var switchPrivate: SwitchMaterial
    private lateinit var btnUpdate: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_edit_group, container, false)

        val json = arguments?.getString(ARG_GROUP) ?: return view
        group = Gson().fromJson(json, GroupCardInfosDto::class.java)

        tilName = view.findViewById(R.id.tilGroupName)
        etName = view.findViewById(R.id.etGroupName)
        tilDescription = view.findViewById(R.id.tilGroupDescription)
        etDescription = view.findViewById(R.id.etGroupDescription)
        switchPrivate = view.findViewById(R.id.switchGroupPrivate)
        btnUpdate = view.findViewById(R.id.btnUpdateGroup)

        etName.hint = group.name
        etDescription.hint = group.description.orEmpty()
        switchPrivate.isChecked = group.isGroupPrivate

        view.findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            findNavController().navigateUp()
        }

        btnUpdate.setOnClickListener { performUpdate() }

        return view
    }

    private fun performUpdate() {
        val newName = etName.text.toString().trim().takeIf { it.isNotEmpty() && it != group.name }
        val newDesc = etDescription.text.toString().trim()
            .takeIf { it.isNotEmpty() && it != group.description.orEmpty() }
        val newPrivate = switchPrivate.isChecked.takeIf { it != group.isGroupPrivate }

        val dto = UpdateGroupRequestDto(
            name = newName,
            description = newDesc,
            isGroupPrivate = newPrivate
        )

        btnUpdate.isEnabled = false
        ApiClient.patch("group/admin/${group.id}", dto) { _, code, error ->
            activity?.runOnUiThread {
                btnUpdate.isEnabled = true
                if (error == null) {
                    Toast.makeText(context, R.string.success_group_updated, Toast.LENGTH_SHORT)
                        .show()
                    findNavController().navigateUp()
                } else if (code == 200) {
                    Toast.makeText(context, R.string.groups_admin_fail_update_name, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, R.string.error_group_update, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    companion object {
        const val ARG_GROUP = "group"
    }
}
