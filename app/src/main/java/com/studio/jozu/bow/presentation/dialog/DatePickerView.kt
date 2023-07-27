package com.studio.jozu.bow.presentation.dialog

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.appcompat.widget.LinearLayoutCompat
import com.studio.jozu.bow.databinding.DatePickerViewBinding
import com.studio.jozu.bow.domain.extension.CalendarEx
import java.util.*

class DatePickerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : LinearLayoutCompat(context, attrs, defStyleAttr) {
    companion object {
        private val YEAR_MAX = Calendar.getInstance()[Calendar.YEAR]
        private const val YEAR_MIN = 1910
    }

    private val binding = DatePickerViewBinding.inflate(LayoutInflater.from(context), this)

    var date: Calendar = CalendarEx.today
        private set
    private var currentYear: Int = 2000
    private var currentMonth: Int = 1
    private var currentDay: Int = 1


    init {
        this.orientation = HORIZONTAL
        setUpPicker()
    }

    fun setCurrentDate(date: Calendar) {
        this.date = date
        setUpPicker()
    }

    /**
     * 年月日のドラムを設定
     */
    private fun setUpPicker() {
        currentYear = date[Calendar.YEAR]
        currentMonth = date[Calendar.MONTH] + 1
        currentDay = date[Calendar.DAY_OF_MONTH]

        updatePickerYear()
        updatePickerMonth()
        updatePickerDay()

        binding.pickerYear.setOnValueChangedListener { _, _, newVal ->
            currentYear = newVal
            updatePickerDay()
        }
        binding.pickerMonth.setOnValueChangedListener { _, _, newVal ->
            currentMonth = newVal
            updatePickerDay()
        }
        binding.pickerDay.setOnValueChangedListener { _, _, newVal ->
            currentDay = newVal
            updatePickerDay()
        }
    }

    /**
     * 年のドラムを設定
     */
    private fun updatePickerYear() {
        binding.pickerYear.maxValue = YEAR_MAX
        binding.pickerYear.minValue = YEAR_MIN
        binding.pickerYear.value = currentYear
    }

    /**
     * 月のドラムを設定
     */
    private fun updatePickerMonth() {
        binding.pickerMonth.maxValue = 12
        binding.pickerMonth.minValue = 1
        binding.pickerMonth.value = currentMonth
    }

    /**
     * 日のドラムを設定
     */
    private fun updatePickerDay() {
        date = CalendarEx.setDay(currentYear, currentMonth - 1, currentDay)

        binding.pickerDay.maxValue = date.getActualMaximum(Calendar.DATE)
        binding.pickerDay.minValue = 1
        binding.pickerDay.value = if (currentDay > binding.pickerDay.maxValue) binding.pickerDay.maxValue else currentDay
    }
}