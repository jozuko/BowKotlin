package com.studio.jozu.bow.presentation.main.settings.dog

import android.graphics.PointF
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.RecyclerView
import com.studio.jozu.bow.R
import com.studio.jozu.bow.domain.Dog

class DogListAdapter : RecyclerView.Adapter<DogListAdapter.DogViewHolder>() {
    class DogViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val viewName: TextView = view.findViewById(R.id.dog_list_row_name)
        val viewColor: ImageView = view.findViewById(R.id.dog_list_row_color)
        val viewIcon: ImageView = view.findViewById(R.id.dog_list_row_image)
        val viewEdit: ImageView = view.findViewById(R.id.dog_list_row_edit)
        val viewVisibilityOff: ImageView = view.findViewById(R.id.dog_list_row_visibility_off)
    }

    private var dogList = listOf<Dog>()
    var onEditing: ((dog: Dog, targetPosition: PointF) -> Unit)? = null

    fun saveDog(dog: Dog) {
        var changePosition = 0
        val newDogList = mutableListOf<Dog>()

        dogList.forEachIndexed { index, listDog ->
            if (listDog.dogId == dog.dogId) {
                newDogList.add(dog)
                changePosition = index
            } else {
                newDogList.add(listDog)
            }
        }

        if (dogList.find { it.dogId == dog.dogId } == null) {
            newDogList.add(dog)
            changePosition = newDogList.count() - 1
        }

        this.dogList = newDogList
        notifyItemChanged(changePosition)
    }

    fun replaceList(newDogList: List<Dog>) {
        this.dogList = newDogList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DogViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.setting_dog_edit_list_row, parent, false)
        return DogViewHolder(view)
    }

    override fun onBindViewHolder(holder: DogViewHolder, position: Int) {
        val item = dogList[position]
        holder.viewName.text = item.name
        holder.viewColor.setImageResource(item.color.drawableRes)

        holder.viewIcon.setImageDrawable(null)
        item.photo(holder.viewIcon.context)
            ?.let { photo ->
                holder.viewIcon.setImageURI(Uri.fromFile(photo))
            }
            ?: let {
                holder.viewIcon.setImageResource(R.mipmap.ic_dog)
            }
        holder.viewIcon.invalidate()

        holder.viewEdit.setOnClickListener { view ->
            val posArray = IntArray(2)
            view.getLocationOnScreen(posArray)

            val centerX = posArray[0] + (view.width.toFloat()) / 2
            val centerY = posArray[1] + (view.height.toFloat()) / 2
            onEditing?.invoke(item, PointF(centerX, centerY))
        }
        holder.viewVisibilityOff.isInvisible = item.enabled
    }

    override fun getItemCount(): Int {
        return dogList.count()
    }
}