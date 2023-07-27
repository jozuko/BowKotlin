package com.studio.jozu.bow.presentation.main

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.studio.jozu.bow.R
import com.studio.jozu.bow.databinding.CalendarDogSelectionViewBinding
import com.studio.jozu.bow.domain.Dog
import com.studio.jozu.bow.domain.Task
import com.studio.jozu.bow.domain.extension.CalendarEx
import com.studio.jozu.bow.domain.extension.CalendarEx.format
import java.util.*

class DogSelectionView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {
    private val binding = CalendarDogSelectionViewBinding.inflate(LayoutInflater.from(context), this, true)

    private var selectedTask: Task? = null
    private var timestamp = CalendarEx.now
    private var tempTimestamp = CalendarEx.now
    private var dogs = listOf<Dog>()

    var onCancel: (() -> Unit)? = null
    var onSelectedDog: ((task: Task, timestamp: Calendar, dogs: List<Dog>) -> Unit)? = null
    var onDeleteEvent: (() -> Unit)? = null

    init {
        binding.dogSelectionClose.setOnClickListener {
            onCancel?.invoke()
        }

        binding.dogSelectionDelete.setOnClickListener {
            onDeleteEvent?.invoke()
        }

        binding.dogSelectionAllDog.setOnClickListener {
            val task = selectedTask ?: return@setOnClickListener
            onSelectedDog?.invoke(task, timestamp, dogs)
        }

        binding.dogSelectionList.setOnItemClickListener { _, _, position, _ ->
            val task = selectedTask ?: return@setOnItemClickListener
            (binding.dogSelectionList.adapter as? DogSelectionDogListAdapter)?.getItem(position)?.let { dog ->
                onSelectedDog?.invoke(task, timestamp, listOf(dog))
            }
        }

        binding.dogSelectionEventTimestamp.text = timestamp.format("yyyy/M/d H:mm")
        binding.dogSelectionEventTimestamp.setOnClickListener {
            showDateTimePicker()
        }
    }

    fun setTask(task: Task) {
        this.selectedTask = task

        binding.dogSelectionTask.text = task.title
        binding.dogSelectionTask.setLeftDrawable(task.icon.iconRes, R.dimen.dog_selection_task_icon_size)
    }

    fun setDogList(dogs: List<Dog>) {
        this.dogs = dogs
    }

    fun setDefaultDog(defaultDog: Dog?) {
        binding.dogSelectionList.adapter = DogSelectionDogListAdapter(context, dogs, defaultDog)
    }

    fun setTimestamp(timestamp: Calendar) {
        this.timestamp = timestamp.clone() as Calendar
        binding.dogSelectionEventTimestamp.text = timestamp.format("yyyy/M/d H:mm")
    }

    var canDelete: Boolean = false
        set(value) {
            field = value
            binding.dogSelectionDelete.isVisible = value
        }

    private fun TextView.setLeftDrawable(@DrawableRes id: Int = 0, @DimenRes sizeRes: Int) {
        val drawable = ContextCompat.getDrawable(context, id)
        val size = resources.getDimensionPixelSize(sizeRes)
        drawable?.setBounds(0, 0, size, size)
        this.setCompoundDrawables(drawable, null, null, null)
    }

    private fun showDateTimePicker() {
        val timePickerListener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
            tempTimestamp[Calendar.HOUR_OF_DAY] = hourOfDay
            tempTimestamp[Calendar.MINUTE] = minute
            setTimestamp(tempTimestamp)
        }

        val datePickerListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            tempTimestamp[Calendar.YEAR] = year
            tempTimestamp[Calendar.MONTH] = month
            tempTimestamp[Calendar.DAY_OF_MONTH] = dayOfMonth

            TimePickerDialog(
                context,
                timePickerListener,
                tempTimestamp[Calendar.HOUR_OF_DAY],
                tempTimestamp[Calendar.MINUTE],
                true
            ).show()
        }

        tempTimestamp = timestamp.clone() as Calendar
        DatePickerDialog(
            context,
            datePickerListener,
            timestamp[Calendar.YEAR],
            timestamp[Calendar.MONTH],
            timestamp[Calendar.DAY_OF_MONTH],
        ).show()
    }

}