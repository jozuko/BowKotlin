package com.studio.jozu.bow.presentation.main.settings.dog

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.core.view.isVisible
import com.studio.jozu.bow.R
import com.studio.jozu.bow.databinding.SettingDogColorSelectionViewBinding
import com.studio.jozu.bow.domain.DogColor

/**
 * Created by r.mori on 2020/10/19.
 * Copyright (c) 2020 rei-frontier. All rights reserved.
 */
class DogColorSelectionView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {
    private val binding = SettingDogColorSelectionViewBinding.inflate(LayoutInflater.from(context), this, true)

    private var color: DogColor = DogColor.RED

    var onSelected: ((color: DogColor) -> Unit)? = null

    var isColorSelected: Boolean
        get() {
            return binding.colorSelectionViewBase.isSelected
        }
        set(value) {
            binding.colorSelectionViewBase.isSelected = value
            if (useCheckFootIcon) {
                binding.colorSelectionSelectedIcon.isVisible = value
            }
        }

    var useCheckFootIcon: Boolean = true
        set(value) {
            field = value
            binding.colorSelectionSelectedIcon.isVisible = false
        }

    init {
        attrs?.let { setColor(attrs) }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        binding.colorSelectionViewBase.setOnClickListener { onSelected?.invoke(color) }
    }

    private fun setColor(attrs: AttributeSet) {
        color = DogColor.RED
        context.obtainStyledAttributes(attrs, R.styleable.DogColorSelectionView).use {
            val colorType = it.getInt(R.styleable.DogColorSelectionView_color_type, DogColor.RED.ordinal)
            color = DogColor.getType(colorType)
        }

        setColor(color)
    }

    private fun setColor(color: DogColor) {
        binding.colorSelectionColor.setBackgroundColor(ContextCompat.getColor(context, color.colorRes))
    }
}