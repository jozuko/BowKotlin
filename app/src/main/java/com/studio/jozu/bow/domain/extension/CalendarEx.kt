package com.studio.jozu.bow.domain.extension

import java.text.SimpleDateFormat
import java.util.*

object CalendarEx {
    val maxUnixTime: Int
        get() = Int.MAX_VALUE

    val now: Calendar
        get() = Calendar.getInstance()

    val today: Calendar
        get() = now.startDay

    fun fromUnixTime(seconds: Int?): Calendar {
        val unixTime = seconds ?: maxUnixTime

        return Calendar.getInstance().apply {
            timeInMillis = unixTime * 1000L
        }
    }

    fun setDay(year: Int, month: Int, day: Int): Calendar {
        return Calendar.getInstance().apply {
            this[Calendar.YEAR] = year
            this[Calendar.MONTH] = month
            this[Calendar.DAY_OF_MONTH] = day
            this[Calendar.HOUR_OF_DAY] = 0
            this[Calendar.MINUTE] = 0
            this[Calendar.SECOND] = 0
            this[Calendar.MILLISECOND] = 0
        }
    }

    val Calendar.unixTime: Int
        get() = (this.timeInMillis / 1000L).toInt()

    fun Calendar.format(format: String): String {
        val dateFormat = SimpleDateFormat(format, Locale.US)
        return dateFormat.format(this.time)
    }

    fun Calendar.addYears(years: Int): Calendar {
        return (this.clone() as Calendar).apply {
            this.add(Calendar.YEAR, years)
        }
    }

    fun Calendar.addMonths(months: Int): Calendar {
        return (this.clone() as Calendar).apply {
            this.add(Calendar.MONTH, months)
        }
    }

    fun Calendar.addDays(days: Int): Calendar {
        return (this.clone() as Calendar).apply {
            this.add(Calendar.DAY_OF_MONTH, days)
        }
    }

    fun Calendar.isSameMonth(target: Calendar): Boolean {
        return this[Calendar.YEAR] == target[Calendar.YEAR] && this[Calendar.MONTH] == target[Calendar.MONTH]
    }

    fun Calendar.isSameDay(target: Calendar): Boolean {
        return this[Calendar.YEAR] == target[Calendar.YEAR] && this[Calendar.MONTH] == target[Calendar.MONTH] && this[Calendar.DAY_OF_MONTH] == target[Calendar.DAY_OF_MONTH]
    }

    val Calendar.startMonth: Calendar
        get() = (this.clone() as Calendar).apply {
            this[Calendar.DAY_OF_MONTH] = 1
            this[Calendar.HOUR_OF_DAY] = 0
            this[Calendar.MINUTE] = 0
            this[Calendar.SECOND] = 0
            this[Calendar.MILLISECOND] = 0
        }

    val Calendar.endMonth: Calendar
        get() {
            val startDayOfNextMonth = this.addMonths(1).startMonth
            return startDayOfNextMonth.addDays(-1).startDay
        }

    val Calendar.startDay: Calendar
        get() = (this.clone() as Calendar).apply {
            this[Calendar.HOUR_OF_DAY] = 0
            this[Calendar.MINUTE] = 0
            this[Calendar.SECOND] = 0
            this[Calendar.MILLISECOND] = 0
        }

    val Calendar.endDay: Calendar
        get() = (this.clone() as Calendar).apply {
            this[Calendar.HOUR_OF_DAY] = 23
            this[Calendar.MINUTE] = 59
            this[Calendar.SECOND] = 59
            this[Calendar.MILLISECOND] = 0
        }


    val Calendar.startWeek: Calendar
        get() {
            val day = this.clone() as Calendar
            while (true) {
                if (day[Calendar.DAY_OF_WEEK] == Calendar.SUNDAY) {
                    return day
                }
                day.add(Calendar.DAY_OF_MONTH, -1)
            }
        }

    val Calendar.endWeek: Calendar
        get() {
            val day = this.clone() as Calendar
            while (true) {
                if (day[Calendar.DAY_OF_WEEK] == Calendar.SATURDAY) {
                    return day
                }
                day.add(Calendar.DAY_OF_MONTH, 1)
            }
        }

    val Calendar.pastMinutes: Int
        get() {
            val nowMillis = now.timeInMillis
            val valueMillis = timeInMillis
            val diffMillis = nowMillis - valueMillis
            return (diffMillis / 1000L).toInt() / 60
        }

    fun Calendar.diffDays(targetDay: Calendar): Int {
        val diffSeconds = this.startDay.unixTime - targetDay.startDay.unixTime
        return diffSeconds / (24 * 60 * 60)
    }
}