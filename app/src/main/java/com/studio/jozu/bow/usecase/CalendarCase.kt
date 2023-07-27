package com.studio.jozu.bow.usecase

import com.studio.jozu.bow.domain.CalendarMonth
import com.studio.jozu.bow.domain.extension.CalendarEx
import com.studio.jozu.bow.domain.extension.CalendarEx.addMonths
import com.studio.jozu.bow.domain.extension.CalendarEx.isSameMonth
import com.studio.jozu.bow.domain.extension.CalendarEx.startMonth
import java.util.*

class CalendarCase {
    companion object {
        private val START_DAY = Calendar.getInstance().apply {
            this[Calendar.YEAR] = 2020
            this[Calendar.MONTH] = Calendar.JANUARY
            this[Calendar.DAY_OF_MONTH] = 1
            this[Calendar.HOUR_OF_DAY] = 0
            this[Calendar.MINUTE] = 0
            this[Calendar.SECOND] = 0
            this[Calendar.MILLISECOND] = 0
        }

        private const val MONTH_RANGE = 20/*year*/ * 12/*month*/
    }

    val allMonthList = createAllMonthList()

    private fun createAllMonthList(): List<CalendarMonth> {
        val startMonth = START_DAY.clone() as Calendar
        val currentMonth = CalendarEx.today.startMonth
        val monthList = mutableListOf<CalendarMonth>()
        (0 until MONTH_RANGE).forEach {
            val month = startMonth.addMonths(it)
            if (month <= currentMonth) {
                val calendarMonth = CalendarMonth.Builder().build(month)
                monthList.add(calendarMonth)
            }
        }
        return monthList
    }

    val todayMonthIndex: Int
        get() {
            val today = CalendarEx.today
            return allMonthList.indexOfFirst { it.month.isSameMonth(today) }
        }
}