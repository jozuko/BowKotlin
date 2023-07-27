package com.studio.jozu.bow.presentation.main.calendar

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.squareup.picasso.Picasso
import com.studio.jozu.bow.R
import com.studio.jozu.bow.domain.Dog
import com.studio.jozu.bow.domain.Event
import com.studio.jozu.bow.domain.Task
import com.studio.jozu.bow.domain.extension.CalendarEx.format
import com.studio.jozu.bow.domain.extension.ContextEx.density
import com.studio.jozu.bow.presentation.picasso.PicassoRoundedSquareTransform
import kotlin.math.roundToInt

class CalendarEventListAdapter(
    private val context: Context,
    private val dogList: List<Dog>,
    private val taskList: List<Task>,
    private val dogEventMap: Map<String, MutableList<Event>>,
) : BaseAdapter() {
    data class DogEvent(val dog: Dog, val event: Event?)

    private val layoutInflater = LayoutInflater.from(context)
    private val dogEventList: List<DogEvent>
    private val iconRadius: Float = 8 * context.density
    private val rowInterval: Int = (16 * context.density).roundToInt()

    init {
        val list = mutableListOf<DogEvent>()
        dogList.forEach { dog ->
            dogEventMap[dog.dogId]?.forEachIndexed { index, dogRecord ->
                if (index == 0) {
                    list.add(DogEvent(dog, null))
                }
                list.add(DogEvent(dog, dogRecord))
            }
        }
        dogEventList = list
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val dogRecord = dogEventList[position]
        return dogRecord.event?.let { record -> createRecordView(record) } ?: createDogView(dogRecord.dog, position == 0)
    }

    override fun getCount(): Int {
        return dogEventList.count()
    }

    override fun getItem(position: Int): DogEvent {
        return dogEventList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    @SuppressLint("InflateParams")
    private fun createDogView(dog: Dog, isFirst: Boolean): View {
        val viewBase = layoutInflater.inflate(R.layout.calendar_event_dog_row, null, false)
        if (!isFirst) {
            viewBase.setPadding(0, rowInterval, 0, 0)
        }

        // 写真
        val viewPhoto: ImageView = viewBase.findViewById(R.id.viewPhoto)
        dog.photo(context)?.let { photo ->
            Picasso.get()
                .load(photo)
                .transform(PicassoRoundedSquareTransform(viewPhoto.layoutParams.width, iconRadius))
                .into(viewPhoto)
        }

        // 名前
        val viewName: TextView = viewBase.findViewById(R.id.dog_list_row_name)
        viewName.text = dog.name

        // 色
        val dogBaseColor = ContextCompat.getColor(context, dog.color.colorRes)
        val viewColor: View = viewBase.findViewById(R.id.dog_list_row_color)
        viewColor.setBackgroundColor(dogBaseColor)

        return viewBase
    }

    @SuppressLint("InflateParams")
    private fun createRecordView(event: Event): View {
        val viewBase = layoutInflater.inflate(R.layout.calendar_event_event_row, null, false)
        val task = event.getTask(taskList)!!

        // アイコン
        val viewIcon: ImageView = viewBase.findViewById(R.id.dog_list_row_image)
        viewIcon.setImageResource(task.icon.iconRes)

        // タイトル
        val viewTitle: TextView = viewBase.findViewById(R.id.viewTitle)
        viewTitle.text = task.title

        // 時間
        val viewTime: TextView = viewBase.findViewById(R.id.viewTime)
        viewTime.text = event.timestamp.format("H:mm")

        return viewBase
    }

}