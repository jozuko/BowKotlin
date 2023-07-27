package com.studio.jozu.bow.presentation.main.settings.task

import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.PointF
import android.util.AttributeSet
import android.util.SizeF
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.isInvisible
import com.studio.jozu.bow.databinding.SettingTaskEditViewBinding
import com.studio.jozu.bow.domain.Task
import com.studio.jozu.bow.domain.TaskIcon

class TaskEditView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {
    private var binding = SettingTaskEditViewBinding.inflate(LayoutInflater.from(context), this, true)

    private val baseViewPoint: PointF by lazy {
        PointF(binding.taskEditBase.x, binding.taskEditBase.y)
    }
    private val thisViewSize: SizeF by lazy {
        SizeF(width.toFloat(), height.toFloat())
    }

    private val taskIconViewList = mutableListOf<TaskIconSelectionView>()

    private var completion: ((canceled: Boolean) -> Unit)? = null
    private var pivotPointF: PointF? = null
    var task: Task = Task.emptyTask
        set(value) {
            field = value
            refreshData()
        }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        setUpTaskIconList()
        binding.taskEditOkButton.setOnClickListener { onClickOk() }
        binding.taskEditCancelButton.setOnClickListener { onClickCancel() }
    }

    private fun setUpTaskIconList() {
        binding.taskEditIconList.removeAllViews()
        taskIconViewList.clear()

        TaskIcon.values().forEach { taskIcon ->
            val view = TaskIconSelectionView(context).apply {
                layoutParams = ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
                icon = taskIcon
                onSelected = ::onSelectedIcon
            }
            binding.taskEditIconList.addView(view, view.layoutParams)
            taskIconViewList.add(view)
        }
    }

    private fun onSelectedIcon(selectedIcon: TaskIcon) {
        taskIconViewList.forEach { view ->
            view.isIconSelected = (view.icon.iconNo == selectedIcon.iconNo)
        }
    }

    fun show(task: Task?, pivotPointF: PointF, completion: (canceled: Boolean) -> Unit) {
        this.task = task ?: Task.emptyTask
        this.completion = completion
        this.pivotPointF = pivotPointF
        showWithAnimation()
    }

    private fun showWithAnimation() {
        val pivotPointF = this.pivotPointF ?: PointF(thisViewSize.width / 2, thisViewSize.height / 2)

        val scaleProperties = PropertyValuesHolder.ofFloat("scale", 0f, 1f)
        val translateXProperties = PropertyValuesHolder.ofFloat("x", pivotPointF.x, baseViewPoint.x)
        val translateYProperties = PropertyValuesHolder.ofFloat("y", pivotPointF.y, baseViewPoint.y)

        val animator = ValueAnimator.ofPropertyValuesHolder(scaleProperties, translateXProperties, translateYProperties)
        animator.duration = 300L
        animator.addUpdateListener { animation ->
            val scale = animation.getAnimatedValue("scale") as Float
            binding.taskEditBase.scaleX = scale
            binding.taskEditBase.scaleY = scale
            binding.taskEditBase.pivotX = 0f
            binding.taskEditBase.pivotY = 0f
            binding.taskEditBase.x = animation.getAnimatedValue("x") as Float
            binding.taskEditBase.y = animation.getAnimatedValue("y") as Float
        }
        animator.doOnStart {
            this.isInvisible = false
        }
        animator.start()
    }

    private fun hideWithAnimation() {
        val pivotPointF = this.pivotPointF ?: PointF(thisViewSize.width / 2, thisViewSize.height / 2)

        val scaleProperties = PropertyValuesHolder.ofFloat("scale", 1f, 0f)
        val translateXProperties = PropertyValuesHolder.ofFloat("x", baseViewPoint.x, pivotPointF.x)
        val translateYProperties = PropertyValuesHolder.ofFloat("y", baseViewPoint.y, pivotPointF.y)

        val animator = ValueAnimator.ofPropertyValuesHolder(scaleProperties, translateXProperties, translateYProperties)
        animator.duration = 300L
        animator.addUpdateListener { animation ->
            val scale = animation.getAnimatedValue("scale") as Float
            binding.taskEditBase.scaleX = scale
            binding.taskEditBase.scaleY = scale
            binding.taskEditBase.pivotX = 0f
            binding.taskEditBase.pivotY = 0f
            binding.taskEditBase.x = animation.getAnimatedValue("x") as Float
            binding.taskEditBase.y = animation.getAnimatedValue("y") as Float
        }
        animator.doOnEnd {
            this@TaskEditView.isInvisible = true
        }

        animator.start()
    }

    private fun refreshData() {
        binding.taskEditTitle.setText(this.task.title)
        onSelectedIcon(this.task.icon)
    }

    private fun onClickOk() {
        saveCurrentInput()
        hideWithAnimation()
        completion?.invoke(false)
    }

    private fun onClickCancel() {
        hideWithAnimation()
        completion?.invoke(true)
    }

    private fun saveCurrentInput() {
        this.task = this.task.copy(
            title = binding.taskEditTitle.text.toString(),
            icon = taskIconViewList.find { it.isIconSelected }?.icon ?: TaskIcon.FOOD,
        )
    }
}