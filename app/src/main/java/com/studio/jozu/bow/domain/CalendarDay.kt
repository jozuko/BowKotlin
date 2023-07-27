package com.studio.jozu.bow.domain

import java.util.*

data class CalendarDay(
    val day: Calendar,
    val isCurrentMonthDay: Boolean,
    val dogRecordList: MutableList<Event>,
)