package com.example.myapplication.fragment.trip

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.myapplication.R
import com.example.myapplication.dto.post.PostType
import com.example.myapplication.utils.LocalisationSuggester
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText

class CreateTripFragment : Fragment() {

    private lateinit var etCity: TextInputEditText
    private lateinit var cgCitySuggestions: ChipGroup
    private lateinit var tgMode: MaterialButtonToggleGroup
    private lateinit var cgPostTypes: ChipGroup
    private lateinit var sliderBudget: Slider
    private lateinit var sliderTime: Slider
    private lateinit var tvBudgetValue: TextView
    private lateinit var tvTimeValue: TextView
    private lateinit var tvBudgetLabel: TextView
    private lateinit var tvTimeLabel: TextView

    private val selectedTypes = mutableSetOf<PostType>()
    private var ignoreNextCityChange = false
    private val handler = Handler(Looper.getMainLooper())
    private var locRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_create_trip, container, false)

        view.findViewById<ImageView>(R.id.btnBack)
            .setOnClickListener { findNavController().navigateUp() }

        etCity = view.findViewById(R.id.etTripCity)
        cgCitySuggestions = view.findViewById(R.id.cgCitySuggestions)
        cgPostTypes = view.findViewById(R.id.cgPostTypes)
        sliderBudget = view.findViewById(R.id.sliderBudget)
        sliderTime = view.findViewById(R.id.sliderTime)
        tvBudgetValue = view.findViewById(R.id.tvBudgetValue)
        tvTimeValue = view.findViewById(R.id.tvTimeValue)
        tvBudgetLabel = view.findViewById(R.id.tvBudgetLabel)
        tvTimeLabel = view.findViewById(R.id.tvTimeLabel)

        tvBudgetLabel.text = getString(R.string.trip_budget_label)
        tvTimeLabel.text = getString(R.string.trip_time_label)

        setupCitySuggestions()
        setupPostTypeChips()
        setupSliders()

        view.findViewById<MaterialButton>(R.id.btnSubmitTrip).setOnClickListener {
            if (validateFields()) {
                onSubmit()
            }
        }

        return view
    }

    private fun setupCitySuggestions() {
        etCity.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                locRunnable?.let { handler.removeCallbacks(it) }
                if (ignoreNextCityChange) { ignoreNextCityChange = false; return }
                val q = s?.toString()?.trim().orEmpty()
                if (q.length < 2) { cgCitySuggestions.removeAllViews(); return }
                locRunnable = Runnable {
                    LocalisationSuggester.suggest(q) { results ->
                        activity?.runOnUiThread {
                            cgCitySuggestions.removeAllViews()
                            results.forEach { suggestion ->
                                val chip = Chip(context).apply {
                                    text = suggestion.label
                                    isClickable = true
                                    setOnClickListener {
                                        ignoreNextCityChange = true
                                        etCity.setText(suggestion.label)
                                        etCity.setSelection(etCity.text?.length ?: 0)
                                        cgCitySuggestions.removeAllViews()
                                    }
                                }
                                cgCitySuggestions.addView(chip)
                            }
                        }
                    }
                }
                handler.postDelayed(locRunnable!!, 350)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupPostTypeChips() {
        PostType.entries.forEach { type ->
            val chip = Chip(context).apply {
                text = getString(type.labelRes)
                isClickable = true
                isCheckable = true
                isChecked = false
                setOnCheckedChangeListener { _, checked ->
                    if (checked) selectedTypes.add(type) else selectedTypes.remove(type)
                }
            }
            cgPostTypes.addView(chip)
        }
    }

    private fun setupSliders() {
        tvBudgetValue.text = getString(R.string.trip_budget_format, sliderBudget.value.toInt())
        tvTimeValue.text = getString(R.string.trip_time_format, sliderTime.value.toInt())

        sliderBudget.addOnChangeListener { _, value, _ ->
            tvBudgetValue.text = getString(R.string.trip_budget_format, value.toInt())
        }
        sliderTime.addOnChangeListener { _, value, _ ->
            tvTimeValue.text = getString(R.string.trip_time_format, value.toInt())
        }
    }

    private fun validateFields(): Boolean {
        var isValid = true

        if (etCity.text.toString().trim().isEmpty()) {
            etCity.error = getString(R.string.error_city_required)
            isValid = false
        }

        return isValid
    }

    private fun onSubmit() {
        val city = etCity.text.toString().trim()
        val budget = sliderBudget.value.toInt()
        val hours = sliderTime.value.toInt() * 60
        val types = selectedTypes.map { it.name }

        Toast.makeText(context, "TravelPath : $city, budget=${budget}€, ${hours}h, types=$types", Toast.LENGTH_LONG).show()
    }
}