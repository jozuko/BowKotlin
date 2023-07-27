package com.studio.jozu.bow.domain

import com.studio.jozu.bow.domain.extension.CalendarEx.addDays
import com.studio.jozu.bow.domain.extension.CalendarEx.isSameMonth
import com.studio.jozu.bow.domain.extension.CalendarEx.startWeek
import java.util.*

data class CalendarMonth(
    val month: Calendar,
    val dayList: List<CalendarDay>
) {
    class Builder {
        fun build(month: Calendar): CalendarMonth {
            val targetMonth = month.clone() as Calendar
            val startDay = targetMonth.startWeek
            val dayList = mutableListOf<CalendarDay>()

            (0 until 6 * 7).forEach {
                val day = startDay.addDays(it)

                dayList.add(
                    CalendarDay(
                        day = day,
                        isCurrentMonthDay = day.isSameMonth(targetMonth),
                        dogRecordList = mutableListOf()
                    )
                )
            }

            return CalendarMonth(targetMonth, dayList)
        }
    }
}