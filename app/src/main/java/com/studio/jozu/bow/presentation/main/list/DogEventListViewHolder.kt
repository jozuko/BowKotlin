package com.studio.jozu.bow.presentation.main.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.studio.jozu.bow.R
import com.studio.jozu.bow.databinding.DogEventListRowEventBinding
import com.studio.jozu.bow.databinding.DogEventListRowMonthBinding
import com.studio.jozu.bow.databinding.DogEventListRowReadMoreBinding
import com.studio.jozu.bow.domain.extension.CalendarEx
import com.studio.jozu.bow.domain.extension.CalendarEx.diffDays
import com.studio.jozu.bow.domain.extension.CalendarEx.format

class DogEventListViewHolder(private val binding: ViewBinding) : RecyclerView.ViewHolder(binding.root) {
    companion object {
        fun newInstance(parent: ViewGroup, viewType: DogEventListViewType): DogEventListViewHolder {
            val binding = when (viewType) {
                DogEventListViewType.EVENT -> DogEventListRowEventBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                DogEventListViewType.MONTH -> DogEventListRowMonthBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                DogEventListViewType.READ_MORE -> DogEventListRowReadMoreBinding.inflate(LayoutInflater.from(parent.context), parent, false)

            }
            return DogEventListViewHolder(binding)
        }
    }

    fun bindData(
        eventModel: DogEventListModel,
        onSelectedEvent: (eventModel: DogEventListModelEvent) -> Unit,
        onReadMore: () -> Unit,
    ) {
        when (eventModel.viewType) {
            DogEventListViewType.EVENT -> bindEventData(eventModel as DogEventListModelEvent, onSelectedEvent)
            DogEventListViewType.MONTH -> bindMonthData(eventModel as DogEventListModelMonth)
            DogEventListViewType.READ_MORE -> bindReadMore(onReadMore)
        }
    }

    private fun bindEventData(eventModel: DogEventListModelEvent, onSelectedEvent: (eventModel: DogEventListModelEvent) -> Unit) {
        val binding = this.binding as? DogEventListRowEventBinding ?: return
        val context = binding.root.context

        binding.dogEventRowEventDay.text = eventModel.event.timestamp.format("M/d")
        binding.dogEventRowEventTime.text = eventModel.event.timestamp.format("H:mm")
        binding.dogEventRowEventPast.text = context.getString(R.string.diff_days, CalendarEx.today.diffDays(eventModel.event.timestamp))
        binding.dogEventRowEventName.text = eventModel.dog.name
        binding.dogEventRowDogColor.setBackgroundColor(ContextCompat.getColor(context, eventModel.dog.color.colorRes))
        binding.dogEventRowEventImage.setImageResource(eventModel.task.icon.iconRes)
        binding.dogEventRowEventTitle.text = eventModel.task.title
        binding.root.setOnClickListener {
            onSelectedEvent.invoke(eventModel)
        }
    }

    private fun bindMonthData(eventModel: DogEventListModelMonth) {
        val binding = this.binding as? DogEventListRowMonthBinding ?: return
        binding.root.text = eventModel.month.format("y/M")
    }

    private fun bindReadMore(onReadMore: () -> Unit) {
        val binding = this.binding as? DogEventListRowReadMoreBinding ?: return
        binding.root.setOnClickListener { onReadMore.invoke() }
    }
}