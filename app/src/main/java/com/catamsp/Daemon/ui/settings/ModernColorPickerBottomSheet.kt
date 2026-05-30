package com.catamsp.Daemon.ui.settings

import android.app.Dialog
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.catamsp.Daemon.R
import com.catamsp.Daemon.preferences.LauncherPreferences
import com.catamsp.Daemon.preferences.theme.Font
import com.catamsp.Daemon.ui.views.PremiumRibbonSlider

class ModernColorPickerBottomSheet() : BottomSheetDialogFragment() {

    private var initialColor: Int = 0
    private var onColorSelected: ((Int) -> Unit)? = null

    constructor(initialColor: Int, onColorSelected: (Int) -> Unit) : this() {
        this.initialColor = initialColor
        this.onColorSelected = onColorSelected
        arguments = Bundle().apply { putInt(ARG_INITIAL_COLOR, initialColor) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialColor = savedInstanceState?.getInt(ARG_INITIAL_COLOR, initialColor)
            ?: arguments?.getInt(ARG_INITIAL_COLOR, initialColor)
            ?: initialColor
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(ARG_INITIAL_COLOR, initialColor)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.window?.setDimAmount(0f)

        dialog.setOnShowListener { dialogInterface ->
            val d = dialogInterface as BottomSheetDialog
            val bottomSheet = d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                sheet.background = null
                val behavior = BottomSheetBehavior.from(sheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }
        return dialog
    }

    private var currentColor = initialColor
    private lateinit var colorPreviewBox: View
    private lateinit var hexInput: EditText
    private lateinit var sliderHue: PremiumRibbonSlider
    private lateinit var sliderSaturation: PremiumRibbonSlider
    private lateinit var sliderBrightness: PremiumRibbonSlider
    private lateinit var sliderAlpha: PremiumRibbonSlider

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_modern_color_picker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentFont = LauncherPreferences.theme().font()
        val typeface = Font.getTypeface(requireContext(), currentFont)

        // Initialize views
        colorPreviewBox = view.findViewById(R.id.color_preview_box)
        hexInput = view.findViewById(R.id.hex_edit_text)
        sliderHue = view.findViewById(R.id.slider_hue)
        sliderSaturation = view.findViewById(R.id.slider_saturation)
        sliderBrightness = view.findViewById(R.id.slider_brightness)
        sliderAlpha = view.findViewById(R.id.slider_alpha)
        val btnCancel: TextView = view.findViewById(R.id.btn_cancel)
        val btnApply: TextView = view.findViewById(R.id.btn_apply)

        // Apply font
        listOf(
            view.findViewById<TextView>(R.id.btn_cancel),
            view.findViewById<TextView>(R.id.btn_apply)
        ).forEach { it.typeface = typeface }

        // Extract initial HSB values
        val hsv = FloatArray(3)
        Color.colorToHSV(initialColor, hsv)
        val initialHue = hsv[0].toInt()
        val initialSat = (hsv[1] * 100).toInt()
        val initialBright = (hsv[2] * 100).toInt()
        val initialAlpha = Color.alpha(initialColor)

        // Configure slider ranges
        sliderHue.setValues(0, 360, initialHue)
        sliderSaturation.setValues(0, 100, initialSat)
        sliderBrightness.setValues(0, 100, initialBright)
        sliderAlpha.setValues(0, 255, initialAlpha)

        // Update color from sliders
        val updateColor = { _: Int ->
            val h = sliderHue.progress.toFloat()
            val s = sliderSaturation.progress.toFloat() / 100f
            val v = sliderBrightness.progress.toFloat() / 100f
            val a = sliderAlpha.progress

            val hsvArray = floatArrayOf(h, s, v)
            currentColor = Color.HSVToColor(a, hsvArray)

            colorPreviewBox.setBackgroundColor(currentColor)
            hexInput.setText(String.format("#%08X", currentColor))
        }

        // Attach listeners
        sliderHue.onValueChangeListener = updateColor
        sliderSaturation.onValueChangeListener = updateColor
        sliderBrightness.onValueChangeListener = updateColor
        sliderAlpha.onValueChangeListener = updateColor

        // Initialize UI
        updateColor(0)

        // Hex input listener
        hexInput.setOnEditorActionListener { _, _, _ ->
            try {
                val newHex = hexInput.text.toString()
                val newAlpha = sliderAlpha.progress
                currentColor = Color.parseColor(newHex)
                currentColor = Color.argb(newAlpha, Color.red(currentColor), Color.green(currentColor), Color.blue(currentColor))
                // Update sliders to match
                val hsvNew = FloatArray(3)
                Color.colorToHSV(currentColor, hsvNew)
                sliderHue.progress = hsvNew[0].toInt()
                sliderSaturation.progress = (hsvNew[1] * 100).toInt()
                sliderBrightness.progress = (hsvNew[2] * 100).toInt()
                colorPreviewBox.setBackgroundColor(currentColor)
                true
            } catch (e: Exception) {
                Toast.makeText(context, "Invalid color format", Toast.LENGTH_SHORT).show()
                false
            }
        }

        btnCancel.setOnClickListener { dismiss() }
        btnApply.setOnClickListener {
            onColorSelected?.invoke(currentColor)
            dismiss()
        }
    }

    companion object {
        private const val ARG_INITIAL_COLOR = "initial_color"
    }
}