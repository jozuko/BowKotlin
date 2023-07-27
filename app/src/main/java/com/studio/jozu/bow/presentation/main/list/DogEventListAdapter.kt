package com.studio.jozu.bow.presentation.main.list

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class DogEventListAdapter(
    private val onSelectedEvent: (eventModel: DogEventListModelEvent) -> Unit,
    private val onReadMore: () -> Unit,
) : RecyclerView.Adapter<DogEventListViewHolder>() {
    private val eventList: MutableList<DogEventListModel> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DogEventListViewHolder {
        val type = DogEventListViewType.values()[viewType]
        return DogEventListViewHolder.newInstance(parent, type)
    }

    override fun onBindViewHolder(holder: DogEventListViewHolder, position: Int) {
        holder.bindData(eventList[position], onSelectedEvent, onReadMore)
    }

    override fun getItemCount(): Int {
        return eventList.count()
    }

    override fun getItemViewType(position: Int): Int {
        return eventList[position].viewType.ordinal
    }

    fun replaceEventList(eventList: List<DogEventListModel>) {
        this.eventList.clear()
        this.eventList.addAll(eventList)
        notifyDataSetChanged()
    }
}