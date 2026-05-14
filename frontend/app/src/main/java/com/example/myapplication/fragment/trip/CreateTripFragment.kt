package com.example.myapplication.fragment.trip

import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.myapplication.R
import com.example.myapplication.dto.post.PostType
import com.example.myapplication.dto.trip.StartingTime
import com.example.myapplication.dto.trip.SuggestTripRequestDto
import com.example.myapplication.dto.trip.TransportMode
import com.example.myapplication.utils.ApiClient
import com.example.myapplication.utils.LocalisationSuggester
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText

class CreateTripFragment : Fragment() {

    private lateinit var etCity: TextInputEditText
    private lateinit var cgCitySuggestions: ChipGroup
    private lateinit var cgPostTypes: ChipGroup
    private lateinit var sliderBudget: Slider
    private lateinit var sliderTime: Slider
    private lateinit var tvBudgetValue: TextView
    private lateinit var tvTimeValue: TextView
    private lateinit var tvBudgetLabel: TextView
    private lateinit var tvTimeLabel: TextView
    private lateinit var llStartingTime: LinearLayout
    private lateinit var llTransportMode: LinearLayout

    private val selectedTypes = mutableSetOf<PostType>()
    private var selectedStartingTime: StartingTime? = null
    private var selectedTransportMode: TransportMode? = null
    private var ignoreNextCityChange = false
    private var selectedCityLat = 0.0
    private var selectedCityLong = 0.0
    private val handler = Handler(Looper.getMainLooper())
    private var locRunnable: Runnable? = null

    private val startingTimeCards = mutableMapOf<StartingTime, MaterialCardView>()
    private val transportModeCards = mutableMapOf<TransportMode, MaterialCardView>()

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
        llStartingTime = view.findViewById(R.id.llStartingTime)
        llTransportMode = view.findViewById(R.id.llTransportMode)

        tvBudgetLabel.text = getString(R.string.trip_budget_label)
        tvTimeLabel.text = getString(R.string.trip_time_label)

        arguments?.getString("prefillCity")?.let { city ->
            ignoreNextCityChange = true
            etCity.setText(city)
        }

        setupCitySuggestions()
        setupPostTypeChips()
        setupSliders()
        setupStartingTimeCards()
        setupTransportModeCards()

        view.findViewById<MaterialButton>(R.id.btnSubmitTrip).setOnClickListener {
            if (validateFields()) onSubmit()
        }

        return view
    }

    private fun setupCitySuggestions() {
        etCity.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                locRunnable?.let { handler.removeCallbacks(it) }
                if (ignoreNextCityChange) { ignoreNextCityChange = false; return }
                selectedCityLat = 0.0
                selectedCityLong = 0.0
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
                                        selectedCityLat = suggestion.lat
                                        selectedCityLong = suggestion.long
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

    private fun setupStartingTimeCards() {
        StartingTime.entries.forEach { time ->
            val card = buildOptionCard(getString(time.labelRes), time.iconRes) {
                selectedStartingTime = time
                startingTimeCards.forEach { (t, c) -> applyCardState(c, t == time) }
            }
            startingTimeCards[time] = card
            llStartingTime.addView(card)
        }
    }

    private fun setupTransportModeCards() {
        TransportMode.entries.forEach { mode ->
            val card = buildOptionCard(getString(mode.labelRes), mode.iconRes) {
                selectedTransportMode = mode
                transportModeCards.forEach { (m, c) -> applyCardState(c, m == mode) }
            }
            transportModeCards[mode] = card
            llTransportMode.addView(card)
        }
    }

    private fun buildOptionCard(label: String, iconRes: Int, onClick: () -> Unit): MaterialCardView {
        val ctx = requireContext()
        val dp8 = (8 * resources.displayMetrics.density).toInt()
        val dp12 = (12 * resources.displayMetrics.density).toInt()
        val dp32 = (32 * resources.displayMetrics.density).toInt()
        val dp80 = (80 * resources.displayMetrics.density).toInt()

        val card = MaterialCardView(ctx).apply {
            radius = dp12.toFloat()
            cardElevation = 0f
            strokeWidth = (1 * resources.displayMetrics.density).toInt()
            setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.surface))
            strokeColor = ContextCompat.getColor(ctx, R.color.divider)
            isClickable = true
            isFocusable = true
            val lp = LinearLayout.LayoutParams(dp80, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.setMargins(0, 0, dp8, 0)
            layoutParams = lp
        }

        val inner = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(dp8, dp12, dp8, dp12)
        }

        val icon = ImageView(ctx).apply {
            setImageResource(iconRes)
            imageTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.text_secondary))
            val iconLp = LinearLayout.LayoutParams(dp32, dp32)
            layoutParams = iconLp
        }

        val text = TextView(ctx).apply {
            text = label
            setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary))
            textSize = 11f
            gravity = android.view.Gravity.CENTER
            val textLp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            textLp.topMargin = (4 * resources.displayMetrics.density).toInt()
            layoutParams = textLp
        }

        inner.addView(icon)
        inner.addView(text)
        card.addView(inner)

        card.setOnClickListener { onClick() }
        return card
    }

    private fun applyCardState(card: MaterialCardView, selected: Boolean) {
        val ctx = requireContext()
        if (selected) {
            card.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.primary_light))
            card.strokeColor = ContextCompat.getColor(ctx, R.color.primary)
            card.strokeWidth = (2 * resources.displayMetrics.density).toInt()
            val inner = card.getChildAt(0) as? LinearLayout ?: return
            (inner.getChildAt(0) as? ImageView)?.imageTintList =
                ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.primary))
            (inner.getChildAt(1) as? TextView)?.setTextColor(
                ContextCompat.getColor(ctx, R.color.primary)
            )
        } else {
            card.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.surface))
            card.strokeColor = ContextCompat.getColor(ctx, R.color.divider)
            card.strokeWidth = (1 * resources.displayMetrics.density).toInt()
            val inner = card.getChildAt(0) as? LinearLayout ?: return
            (inner.getChildAt(0) as? ImageView)?.imageTintList =
                ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.text_secondary))
            (inner.getChildAt(1) as? TextView)?.setTextColor(
                ContextCompat.getColor(ctx, R.color.text_secondary)
            )
        }
    }

    private fun validateFields(): Boolean {
        var isValid = true

        if (etCity.text.toString().trim().isEmpty()) {
            etCity.error = getString(R.string.error_city_required)
            isValid = false
        }
        if (selectedStartingTime == null) {
            Toast.makeText(context, R.string.error_starting_time_required, Toast.LENGTH_SHORT).show()
            isValid = false
        }
        if (selectedTransportMode == null) {
            Toast.makeText(context, R.string.error_transport_mode_required, Toast.LENGTH_SHORT).show()
            isValid = false
        }

        return isValid
    }

    private fun onSubmit() {
        val localisation = etCity.text.toString().trim()
        val dto = SuggestTripRequestDto(
            localisation = localisation,
            maxBudget = sliderBudget.value.toInt(),
            maxDuration = sliderTime.value.toInt() * 60,
            startingTime = selectedStartingTime!!.name,
            transportMode = selectedTransportMode!!.name,
            preferredTypes = selectedTypes.map { it.name }
        )

        ApiClient.post("trip/suggest", dto) { body, code, error ->
            activity?.runOnUiThread {
                if (error != null || body == null) {
                    Toast.makeText(context, error ?: getString(R.string.error_network), Toast.LENGTH_LONG).show()
                } else {
                    val bundle = Bundle().apply {
                        putString("tripsJson", body)
                        putString("localisation", localisation)
                        putString("requestJson", com.google.gson.Gson().toJson(dto))
                        putDouble("startLat", selectedCityLat)
                        putDouble("startLong", selectedCityLong)
                    }
                    findNavController().navigate(R.id.action_createTrip_to_tripResult, bundle)
                }
            }
        }
    }
}
