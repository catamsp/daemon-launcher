package com.catamsp.Daemon.ui.settings

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.core.graphics.alpha
import com.catamsp.Daemon.R
import com.catamsp.Daemon.preferences.LauncherPreferences
import com.catamsp.Daemon.preferences.theme.Font
import com.catamsp.Daemon.ui.UIObject
import com.catamsp.Daemon.ui.views.ColorWheelView
import com.catamsp.Daemon.ui.views.PremiumRibbonSlider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ColorPickerBottomSheet(
    private val initialColor: Int,
    private val onColorSelected: (Int) -> Unit
) : BottomSheetDialogFragment(), UIObject {

    private var currentHue = 0f
    private var currentSaturation = 1f
    private var currentValue = 1f
    private var currentAlpha = 255

    private lateinit var previewBox: View
    private lateinit var colorWheel: ColorWheelView
    private lateinit var brightnessSlider: PremiumRibbonSlider
    private lateinit var alphaSlider: PremiumRibbonSlider

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_color_wheel, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        previewBox = view.findViewById(R.id.color_preview_box)
        colorWheel = view.findViewById(R.id.color_wheel)
        brightnessSlider = view.findViewById(R.id.brightness_slider)
        alphaSlider = view.findViewById(R.id.alpha_slider)
        val title = view.findViewById<TextView>(R.id.color_picker_title)
        val btnApply = view.findViewById<TextView>(R.id.btn_apply_color)

        // Apply Premium Font
        val currentFont = LauncherPreferences.theme().font()
        val typeface = Font.getTypeface(requireContext(), currentFont)
        title.typeface = typeface
        btnApply.typeface = typeface

        // Setup Initial State
        val hsv = FloatArray(3)
        Color.colorToHSV(initialColor, hsv)
        currentHue = hsv[0]
        currentSaturation = hsv[1]
        currentValue = hsv[2]
        currentAlpha = initialColor.alpha

        colorWheel.setColor(initialColor)
        brightnessSlider.setValues(0, 100, (currentValue * 100).toInt())
        alphaSlider.setValues(0, 255, currentAlpha)
        updatePreview()

        // Listeners
        colorWheel.onColorChangeListener = { hue, saturation ->
            currentHue = hue
            currentSaturation = saturation
            updatePreview()
        }

        brightnessSlider.onValueChangeListener = { value ->
            currentValue = value / 100f
            colorWheel.setValue(currentValue)
            updatePreview()
        }

        alphaSlider.onValueChangeListener = { alpha ->
            currentAlpha = alpha
            updatePreview()
        }

        btnApply.setOnClickListener {
            val finalColor = Color.HSVToColor(currentAlpha, floatArrayOf(currentHue, currentSaturation, currentValue))
            onColorSelected(finalColor)
            dismiss()
        }

        applyFont(view)
    }

    private fun updatePreview() {
        val rgb = Color.HSVToColor(floatArrayOf(currentHue, currentSaturation, currentValue))
        val finalColor = Color.argb(currentAlpha, Color.red(rgb), Color.green(rgb), Color.blue(rgb))
        
        val drawable = GradientDrawable().apply {
            cornerRadius = 16f
            setColor(finalColor)
            setStroke(2, Color.WHITE) 
        }
        previewBox.background = drawable
    }

    override fun onStart() {
        super<BottomSheetDialogFragment>.onStart()
        super<UIObject>.onStart()
        dialog?.window?.let { window ->
            window.setDimAmount(0f)
            window.setBackgroundDrawableResource(android.R.color.transparent)
            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        }
    }

    override fun getTheme(): Int = R.style.CustomBottomSheetDialog
}
