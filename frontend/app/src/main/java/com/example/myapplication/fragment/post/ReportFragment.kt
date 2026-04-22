package com.example.myapplication.fragment.post

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.myapplication.R
import com.example.myapplication.dto.post.ReportReason
import com.example.myapplication.dto.post.ReportRequestDto
import com.example.myapplication.utils.ApiClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class ReportFragment : Fragment() {

    private var postId: String? = null
    private lateinit var rgReasons: RadioGroup
    private lateinit var etDetails: TextInputEditText
    private lateinit var btnSubmit: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        postId = arguments?.getString("postId")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_report, container, false)

        rgReasons = view.findViewById(R.id.rgReportReasons)
        etDetails = view.findViewById(R.id.etDetails)
        btnSubmit = view.findViewById(R.id.btnSubmitReport)

        view.findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            findNavController().navigateUp()
        }

        btnSubmit.setOnClickListener {
            submitReport()
        }

        return view
    }

    private fun submitReport() {
        val id = postId ?: run {
            findNavController().navigateUp()
            return
        }

        val checkedId = rgReasons.checkedRadioButtonId
        if (checkedId == -1) {
            Toast.makeText(context, R.string.error_report_reason_required, Toast.LENGTH_SHORT).show()
            return
        }

        val reason = when (checkedId) {
            R.id.rbInappropriate -> ReportReason.INNAPROPRIATE_CONTENT
            R.id.rbWrongLoc -> ReportReason.WRONG_LOCALISATION
            R.id.rbSpam -> ReportReason.SPAM
            R.id.rbPrivacy -> ReportReason.PRIVATE_LIFE_VIOLATION
            R.id.rbDangerous -> ReportReason.DANGEROUS_INFORMATIONS
            else -> ReportReason.OTHER
        }

        val details = etDetails.text.toString().trim().takeIf { it.isNotEmpty() }
        val dto = ReportRequestDto(reason, details)

        btnSubmit.isEnabled = false
        ApiClient.post("post/report/$id", dto) { _, code, error ->
            activity?.runOnUiThread {
                btnSubmit.isEnabled = true
                if (error == null) {
                    Toast.makeText(context, R.string.success_report_sent, Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                } else {
                    Toast.makeText(context, error ?: getString(R.string.error_report_failed), Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}