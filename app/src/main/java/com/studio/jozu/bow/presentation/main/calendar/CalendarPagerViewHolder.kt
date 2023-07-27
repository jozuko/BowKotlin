package com.studio.jozu.bow.presentation.main.calendar

import android.content.Context
import android.content.res.ColorStateList
import android.util.Size
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.doOnDetach
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.gridlayout.widget.GridLayout
import androidx.recyclerview.widget.RecyclerView
import com.studio.jozu.bow.R
import com.studio.jozu.bow.databinding.CalendarGuideBinding
import com.studio.jozu.bow.databinding.CalendarPageBinding
import com.studio.jozu.bow.domain.*
import com.studio.jozu.bow.domain.extension.CalendarEx.format
import com.studio.jozu.bow.domain.extension.CalendarEx.isSameDay
import com.studio.jozu.bow.domain.extension.ContextEx.density
import com.studio.jozu.bow.domain.extension.DisposableListEx.dispose
import com.studio.jozu.bow.usecase.DogListCase
import com.studio.jozu.bow.usecase.EventCase
import com.studio.jozu.bow.usecase.TaskListCase
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import timber.log.Timber
import java.util.*
import kotlin.math.roundToInt

class CalendarPagerViewHolder(
    private val binding: CalendarPageBinding,
    private val eventCase: EventCase,
    private val dogListCase: DogListCase,
    private val taskListCase: TaskListCase,
    private val viewSize: Size,
) : RecyclerView.ViewHolder(binding.root) {
    companion object {
        private const val MAX_ICON_COUNT = 3
    }

    private val context: Context = binding.root.context
    private val layoutInflater = LayoutInflater.from(context)
    private val disposableList = mutableListOf<Disposable>()

    private var eventList = listOf<Event>()
    private var dogList = listOf<Dog>()
    private var taskList = listOf<Task>()
    private var month: CalendarMonth? = null

    private val margin1 = 1 * context.density
    private val cellWidth = (viewSize.width.toFloat() - /* cell-margin */ (margin1 * 8)) / 7
    private val iconSize: Int by lazy {
        val baseCellWidth = cellWidth - (margin1 * 2) - (margin1 * (MAX_ICON_COUNT + 1))
        (baseCellWidth / MAX_ICON_COUNT).roundToInt()
    }
    private val iconTintList: ColorStateList by lazy {
        ColorStateList(
            arrayOf(intArrayOf(android.R.attr.enabled), intArrayOf(-android.R.attr.enabled)),
            intArrayOf(ContextCompat.getColor(context, R.color.white), ContextCompat.getColor(context, R.color.white))
        )
    }

    private val viewDayCellList = mutableListOf<ViewGroup>()

    init {
        binding.root.doOnDetach {
            disposableList.dispose()
        }
    }

    fun setMonth(month: CalendarMonth, canGoPrev: Boolean, canGoNext: Boolean) {
        this.month = month
        binding.prevButton.isInvisible = !canGoPrev
        binding.nextButton.isInvisible = !canGoNext
        binding.calendarYear.text = month.month.format("yyyy")
        binding.calendarMonth.text = context.getString(R.string.month_label, month.month.format("M"))
        binding.eventListBase.isVisible = false
    }

    fun setPrevListener(onClick: (() -> Unit)?) {
        binding.prevButton.setOnClickListener { onClick?.invoke() }
    }

    fun setNextListener(onClick: (() -> Unit)?) {
        binding.nextButton.setOnClickListener { onClick?.invoke() }
    }

    /**
     * 犬凡例
     */
    private fun bindGuide() {
        if (binding.dogListBase.childCount > 1) {
            return
        }

        dogList.forEachIndexed { index, dog ->
            val guideBinding = CalendarGuideBinding.inflate(layoutInflater, binding.dogListBase, false)
            val viewGuide = guideBinding.root
            viewGuide.id = ViewCompat.generateViewId()
            binding.dogListBase.addView(viewGuide, index)
            binding.dogList.addView(viewGuide)

            val viewDog: TextView = viewGuide.findViewById(R.id.viewDog)
            viewDog.text = dog.name

            val viewColor: ImageView = viewGuide.findViewById(R.id.dog_list_row_color)
            viewColor.setImageResource(dog.color.drawableRes)
        }
    }

    /**
     * 日セル
     */
    fun bindDayCell() {
        val month = this.month ?: return

        binding.dayGrid.removeAllViews()
        viewDayCellList.clear()
        month.dayList.forEach { calendarDay ->
            val gridLayoutParams = GridLayout.LayoutParams()
            gridLayoutParams.width = 0
            gridLayoutParams.height = 0
            gridLayoutParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL, 1F)
            gridLayoutParams.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL, 1F)

            // 日セルをGridに追加
            val viewCell = layoutInflater.inflate(R.layout.calendar_cell, binding.dayGrid, false) as ViewGroup
            viewCell.layoutParams = gridLayoutParams
            binding.dayGrid.addView(viewCell)
            viewDayCellList.add(viewCell)

            // 日セルの背景色
            bindDayCellBackground(viewCell, calendarDay)

            // 日付
            bindDayCellDay(viewCell, calendarDay)
        }

        refreshEvent()
    }

    private fun refreshEvent() {
        val month = this.month ?: return

        val dayFrom = month.dayList.first().day
        val dayTo = month.dayList.last().day

        val disposable = eventCase.find(dayFrom, dayTo)
            .flatMap { eventList ->
                this.eventList = eventList
                eventCase.refreshDogIfNeeded(eventList)
            }
            .flatMap {
                dogListCase.getDogListFromLocal()
            }
            .flatMap {
                this.dogList = it.filter { dog -> dog.enabled }
                eventCase.refreshTaskIfNeeded(eventList)
            }
            .flatMap {
                taskListCase.getTaskListFromLocal()
            }
            .map {
                this.taskList = it.filter { task -> task.enabled }
                true
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    // 犬凡例
                    bindGuide()

                    // イベント表示
                    month.dayList.forEachIndexed { index, calendarDay ->
                        viewDayCellList.getOrNull(index)?.let { viewCell ->
                            bindDayCellRecord(viewCell, calendarDay)
                        }
                    }
                },
                {
                    Timber.e("CalendarPagerViewHolder#bindDayCell: ${it.localizedMessage}")
                }
            )
        disposableList.add(disposable)
    }

    /**
     * 日セルの背景色
     */
    private fun bindDayCellBackground(viewCell: ViewGroup, calendarDay: CalendarDay) {
        val viewBackground: ViewGroup = viewCell.findViewById(R.id.viewBackground)
        val backgroundColor = when (calendarDay.day[Calendar.DAY_OF_WEEK]) {
            Calendar.SUNDAY -> R.color.calendar_cell_sunday
            Calendar.SATURDAY -> R.color.calendar_cell_saturday
            else -> R.color.calendar_cell_weekday
        }
        viewBackground.setBackgroundColor(ContextCompat.getColor(viewBackground.context, backgroundColor))
    }

    /**
     * 日セルの日付
     */
    private fun bindDayCellDay(viewCell: ViewGroup, calendarDay: CalendarDay) {
        val viewDay: TextView = viewCell.findViewById(R.id.viewDay)
        viewDay.text = calendarDay.day.format("d")
        if (!calendarDay.isCurrentMonthDay) {
            viewDay.alpha = 0.5f
        }
    }

    /**
     * 日セルの記録
     */
    private fun bindDayCellRecord(viewCell: ViewGroup, calendarDay: CalendarDay) {
        val eventMap = mutableMapOf<String, MutableList<Event>>()

        eventList
            .filter { event -> event.timestamp.isSameDay(calendarDay.day) }
            .filter { event -> event.getDog(dogList) != null }
            .filter { event -> event.getTask(taskList) != null }
            .forEach { event ->
                val mapKey = event.dogId
                eventMap[mapKey]
                    ?.add(event)
                    ?: eventMap.put(mapKey, mutableListOf(event))
            }

        val viewRecordList: LinearLayout = viewCell.findViewById(R.id.viewRecordList)
        viewRecordList.removeAllViews()

        dogList.forEach { dog ->
            val targetDogEventList = eventMap[dog.dogId]?.toList() ?: listOf()
            val displayIconCount = if (targetDogEventList.count() > MAX_ICON_COUNT) MAX_ICON_COUNT else targetDogEventList.count()

            if (displayIconCount > 0) {
                val dogIconBase = CalendarEventBackground(viewCell.context)
                dogIconBase.layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = margin1.roundToInt()
                    leftMargin = margin1.roundToInt()
                    rightMargin = margin1.roundToInt()
                }
                dogIconBase.setPadding(margin1.roundToInt(), 0, 0, 0)
                dogIconBase.orientation = LinearLayout.HORIZONTAL
                dogIconBase.fillColor = dog.color.colorRes
                viewRecordList.addView(dogIconBase, dogIconBase.layoutParams)

                for (i in 0 until displayIconCount) {
                    val iconView = ImageView(viewCell.context)
                    iconView.layoutParams = ViewGroup.MarginLayoutParams(iconSize, iconSize).apply {
                        topMargin = margin1.roundToInt()
                        bottomMargin = margin1.roundToInt()
                        rightMargin = margin1.roundToInt()
                    }
                    iconView.setImageResource(targetDogEventList[i].getTask(taskList)!!.icon.iconRes)
                    iconView.scaleType = ImageView.ScaleType.FIT_CENTER
                    iconView.imageTintList = iconTintList
                    dogIconBase.addView(iconView, iconView.layoutParams)
                }
            }
        }

        if (viewRecordList.childCount > 0) {
            eventMap.forEach { (_, eventList) ->
                eventList.sortBy { it.timestamp }
            }
            viewCell.setOnClickListener {
                onClickCell(viewCell, calendarDay, eventMap, dogList, taskList)
            }
        }
    }

    private fun onClickCell(viewCell: ViewGroup, calendarDay: CalendarDay, dogRecordMap: Map<String, MutableList<Event>>, dogList: List<Dog>, taskList: List<Task>) {
        openRecordView()
        binding.eventCloseButton.setOnClickListener { closeRecordView() }
        binding.eventDay.text = calendarDay.day.format("M/d")
        val adapter = CalendarEventListAdapter(viewCell.context, dogList, taskList, dogRecordMap)
        binding.eventList.adapter = adapter
        binding.eventList.setOnItemClickListener { _, _, position, _ ->
            val event = adapter.getItem(position).event ?: return@setOnItemClickListener
            editEvent(event)
        }
    }

    private fun editEvent(event: Event) {
        val task = event.getTask(taskList) ?: return
        val dog = event.getDog(dogList)

        binding.eventDogSelectionView.setDogList(dogList)
        binding.eventDogSelectionView.setDefaultDog(dog)
        binding.eventDogSelectionView.setTask(task)
        binding.eventDogSelectionView.setTimestamp(event.timestamp)
        binding.eventDogSelectionView.canDelete = true
        binding.eventDogSelectionView.isVisible = true

        binding.eventDogSelectionView.onCancel = {
            binding.eventDogSelectionView.isVisible = false
        }

        binding.eventDogSelectionView.onSelectedDog = { _, timestamp, dogs ->
            val disposable = eventCase.edit(event, task, dogs, timestamp)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        binding.eventDogSelectionView.isVisible = false
                        closeRecordView()
                        refreshEvent()
                    },
                    {
                        Timber.e("CalendarPagerViewHolder#editEvent: ${it.localizedMessage}")
                    }
                )
            disposableList.add(disposable)
        }

        binding.eventDogSelectionView.onDeleteEvent = {
            val disposable = eventCase.delete(event)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        binding.eventDogSelectionView.isVisible = false
                        closeRecordView()
                        refreshEvent()
                    },
                    {
                        Timber.e("CalendarPagerViewHolder#editEvent: ${it.localizedMessage}")
                    }
                )
            disposableList.add(disposable)
        }
    }

    private fun openRecordView() {
        binding.eventListBase.isVisible = true
    }

    private fun closeRecordView() {
        binding.eventListBase.isVisible = false
    }
}