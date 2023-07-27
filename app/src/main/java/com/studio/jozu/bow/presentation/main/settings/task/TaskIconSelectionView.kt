package com.studio.jozu.bow.presentation.main.settings.task

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.studio.jozu.bow.databinding.SettingTaskIconSelectionViewBinding
import com.studio.jozu.bow.domain.TaskIcon

class TaskIconSelectionView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {
    private var binding = SettingTaskIconSelectionViewBinding.inflate(LayoutInflater.from(context), this, true)

    var onSelected: ((icon: TaskIcon) -> Unit)? = null

    var isIconSelected: Boolean
        get() {
            return binding.iconSelectionViewBase.isSelected
        }
        set(value) {
            binding.iconSelectionViewBase.isSelected = value
            if (useCheckFootIcon) {
                binding.iconSelectionSelected.isVisible = value
            }
        }

    var useCheckFootIcon: Boolean = true
        set(value) {
            field = value
            binding.iconSelectionSelected.isVisible = false
        }

    var icon: TaskIcon = TaskIcon.FOOD
        set(value) {
            field = value
            binding.iconSelectionIcon.setImageResource(value.iconRes)
        }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        binding.iconSelectionViewBase.setOnClickListener { onSelected?.invoke(icon) }
    }
}