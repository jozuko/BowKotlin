package com.studio.jozu.bow.presentation.main.calendar

import android.util.Size
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.studio.jozu.bow.databinding.CalendarPageBinding
import com.studio.jozu.bow.domain.CalendarMonth
import com.studio.jozu.bow.domain.Dog
import com.studio.jozu.bow.domain.extension.CalendarEx.format
import com.studio.jozu.bow.usecase.DogListCase
import com.studio.jozu.bow.usecase.EventCase
import com.studio.jozu.bow.usecase.TaskListCase
import timber.log.Timber

class CalendarPagerAdapter(
    private val monthList: List<CalendarMonth>,
    private val dogList: List<Dog>,
    private val eventCase: EventCase,
    private val dogListCase: DogListCase,
    private val taskListCase: TaskListCase,
    private val parentView: ViewGroup,
) : RecyclerView.Adapter<CalendarPagerViewHolder>() {
    var onPrev: (() -> Unit)? = null
    var onNext: (() -> Unit)? = null

    private val viewSize: Size by lazy {
        Size(parentView.width, parentView.height)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarPagerViewHolder {
        val binding = CalendarPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CalendarPagerViewHolder(binding, eventCase, dogListCase, taskListCase, viewSize)
    }

    override fun onBindViewHolder(holder: CalendarPagerViewHolder, position: Int) {
        val month = monthList[position]
        Timber.d("CalendarPagerAdapter#onBindViewHolder: month:${month.month.format("yyyy/M")}")

        val canGoPrev = position > 0
        val canGoNext = position < itemCount - 1
        holder.setMonth(month, canGoPrev, canGoNext)
        holder.setPrevListener(onPrev)
        holder.setNextListener(onNext)
        holder.bindDayCell()
    }

    override fun getItemCount(): Int {
        return monthList.count()
    }
}